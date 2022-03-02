package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixpatterns.ChangeCondition;
import edu.lu.uni.serval.tbar.utils.Checker;

import java.util.*;

/**
 * remove null pointer checker
 * if (exp != null) or if (exp == null)  → if (true) and if (false)
 *
 * @see {@link edu.lu.uni.serval.tbar.fixpatterns.NullPointerChecker}
 */
public class NullPointerCheckerRemover extends ChangeCondition  {

    public static final boolean NPEC_REMOVER_ENABLED = Boolean.getBoolean("extraRemovingPatternsEnabled");



    private static final Set<String> NULL_CHECK_OPERATORS = new HashSet() {{
        add("==");
        add("!=");
    }};

    /*
     * Null Pointer Checker:
     *
     * NPEFix,
     * ELIXIR-T3_NullPointer,
     * FindBugs- NP_NULL_ON_SOME_PATH, NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE, NP_NULL_ON_SOME_PATH_EXCEPTION.
     * PAR	NullChecker
     * FixMiner IfNullChecker
     *
     * Fuzzy fix patterns:
     * SketchFix	If-ConditionTransform
     * SimFix	InsIfStmt
     * SOFix	IfChecker
     *
     */

    @Override
    public void generatePatches() {
        if (!NPEC_REMOVER_ENABLED) return;
        generatePatches(this.getSuspiciousCodeTree());
    }

    public void generatePatches(ITree suspiciousCodeTree) {
        Map<ITree, Integer> allSuspPredicateExps = readAllSuspiciousPredicateExpressions(suspiciousCodeTree);

        // Conditional expression.

        for (Map.Entry<ITree, Integer> entry : allSuspPredicateExps.entrySet()) {
            ITree predicateExp = entry.getKey();

            if (isNullCheckerPredicate(predicateExp)) {
                // replace buggyCodeStr with fixedCodeStr1;
                getPatches().add(PatchFactory.createReplacePatch(predicateExp, "true"));
                getPatches().add(PatchFactory.createReplacePatch(predicateExp, "false"));
            }
        }
    }

    public static boolean isNullCheckerPredicate(ITree predicateExp) {
        if (predicateExp == null) return false;
        List<ITree> children = predicateExp.getChildren();
        if (predicateExp.getChildren() == null || predicateExp.getChildren().size() < 3) return false;

        // null == expression...  or ...expression == null (or !=)
        return "null".equals(children.get(0).getLabel())
                && NULL_CHECK_OPERATORS.contains(children.get(1).getLabel())
                || "null".equals(children.get(children.size() - 1).getLabel())
                && NULL_CHECK_OPERATORS.contains(children.get(children.size() - 2).getLabel());
    }


    @Override
    public Map<ITree, Integer> readAllSuspiciousPredicateExpressions(ITree suspStmtAst) {
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

        if (isNullCheckerPredicate(suspExpTree)){
            predicateExps.put(suspExpTree, 0);
        } else {
            predicateExps.putAll(readNullCheckExpressions(suspExpTree));
        }
        return predicateExps;
    }

    private Map<ITree, Integer> readNullCheckExpressions(ITree suspExpTree) {
        Map<ITree, Integer> predicateExps = new HashMap<>();
        List<ITree> children = suspExpTree.getChildren();
        for (ITree child : children) {
            if (isNullCheckerPredicate(child)) {
                predicateExps.put(child, child.getPos());
            } else {
                predicateExps.putAll(readNullCheckExpressions(child));
            }
        }
        return predicateExps;
    }
}
