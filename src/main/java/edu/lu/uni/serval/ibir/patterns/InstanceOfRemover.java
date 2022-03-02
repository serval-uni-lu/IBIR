package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixpatterns.ChangeCondition;
import edu.lu.uni.serval.tbar.utils.Checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lang 14
 * - if (var instanceof T)
 * <p>
 * use Checker.isInstanceofExpression(contextInfo) to know if the statement has Instanceof Expression
 * Checker.isCastExpression(contextInfo)
 * <p>
 * OperatorMutator(5)
 * <p>
 * easy way to mutate an instanceOf expression : to change it by true or false.
 * i.e
 * <p>
 * -  if (var instanceof T) {
 * +  if (true) {
 * <p>
 * or to remove it completely.
 *
 * @see {@link edu.lu.uni.serval.tbar.fixpatterns.ClassCastChecker}
 * @see {@link edu.lu.uni.serval.tbar.fixpatterns.StatementRemover}
 */
public class InstanceOfRemover extends ChangeCondition {

    public static final boolean IOR_REMOVER_ENABLED = Boolean.getBoolean("extraRemovingPatternsEnabled");

    @Override
    public void generatePatches() {
        if (!IOR_REMOVER_ENABLED) return;
        Map<ITree, Integer> allSuspPredicateExps = readAllSuspiciousPredicateExpressions(this.getSuspiciousCodeTree());
        // Conditional expression.

        for (Map.Entry<ITree, Integer> entry : allSuspPredicateExps.entrySet()) {
            ITree predicateExp = entry.getKey();
            if (Checker.isInstanceofExpression(predicateExp.getType())) {
                // replace buggyCodeStr with fixedCodeStr1;
                getPatches().add(PatchFactory.createReplacePatch(predicateExp,"true"));
            }
        }
    }

    @Override
    protected Map<ITree, Integer> readAllSuspiciousPredicateExpressions(ITree suspStmtAst) {
        Map<ITree, Integer> predicateExps = new HashMap<>();
        ITree suspExpTree;
        if (Checker.isDoStatement(suspStmtAst.getType())) {
            List<ITree> children = suspStmtAst.getChildren();
            suspExpTree = children.get(children.size() - 1);
        } else if (Checker.withBlockStatement(suspStmtAst.getType())) {
            suspExpTree = suspStmtAst.getChild(0);
        } else {
            suspExpTree = suspStmtAst;
        }
        int suspExpTreeType = suspExpTree.getType();

        if (!Checker.isInfixExpression(suspExpTreeType)) {
            if (Checker.isStatement(suspExpTreeType)) {
                predicateExps.putAll(readInstanceOfExpressions(suspExpTree));
            } else {
                predicateExps.put(suspExpTree, 0);
            }
            return predicateExps;
        }
        predicateExps.put(suspExpTree, 0);

        List<ITree> subExps = suspExpTree.getChildren();
        predicateExps.putAll(readSubPredicateExpressions(subExps));
        return predicateExps;

    }

    private Map<ITree, Integer> readInstanceOfExpressions(ITree suspExpTree) {
        Map<ITree, Integer> predicateExps = new HashMap<>();
        List<ITree> children = suspExpTree.getChildren();
        for (ITree child : children) {
            int childType = child.getType();
            if (Checker.isComplexExpression(childType)) {
                if (Checker.isInstanceofExpression(childType)) {
                    predicateExps.put(child, child.getPos());
                }
                predicateExps.putAll(readInstanceOfExpressions(child));
            } else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
                predicateExps.putAll(readInstanceOfExpressions(child));
            } else if (Checker.isStatement(childType)) break;
        }
        return predicateExps;
    }


    private Map<ITree, Integer> readSubPredicateExpressions(List<ITree> subExps) {
        Map<ITree, Integer> predicateExps = new HashMap<>();
        ITree operator = subExps.get(1);
        String op = operator.getLabel();
        if ("||".equals(op) || "&&".equals(op)) {
            ITree leftExp = subExps.get(0);
            ITree rightExp = subExps.get(2);
            predicateExps.put(leftExp, rightExp.getPos());
            if (Checker.isInfixExpression(leftExp.getType())) {
                predicateExps.putAll(readSubPredicateExpressions(leftExp.getChildren()));
            }
            predicateExps.put(rightExp, operator.getPos());
            if (Checker.isInfixExpression(rightExp.getType())) {
                predicateExps.putAll(readSubPredicateExpressions(rightExp.getChildren()));
            }
            for (int index = 3, size = subExps.size(); index < size; index ++) {
                ITree subExp = subExps.get(index);
                ITree prevExp = subExps.get(index - 1);
                int pos = prevExp.getPos() + prevExp.getLength();
                predicateExps.put(subExp, pos);
                if (Checker.isInfixExpression(subExp.getType())) {
                    predicateExps.putAll(readSubPredicateExpressions(subExp.getChildren()));
                }
            }
        }
        return predicateExps;
    }

}
