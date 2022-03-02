package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixpatterns.ChangeCondition;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * remove null pointer checker
 * if (exp != null) or if (exp == null)  → if (true) and if (false)
 *
 * @see {@link edu.lu.uni.serval.tbar.fixpatterns.NullPointerChecker}
 */
public class RangeCheckerRemover extends ChangeCondition  {

    public static final boolean RC_REMOVER_ENABLED = Boolean.getBoolean("extraRemovingPatternsEnabled");

    private static final Set<String> RANGE_CHECK_METHODS = new HashSet() {{
        add(".size()");
        add(".length");
        add(".length()");
        add(".indexOf(");
        add(".charAt(");
    }};

    private static final String ARRAY_ACCESS_REGEX = ".+\\[.+\\]";
    private static final String CHAR_ACCESS_REGEX = ".+\\.charAt\\(.+\\)";
    private static final String LIST_ITEM_ACCESS_REGEX = ".+\\.get\\(.+\\)";
    private static final String INDEX_OF_REGEX = ".+\\.indexOf\\(.+\\)";


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
        if (!RC_REMOVER_ENABLED) return;
        Map<ITree, Integer> allSuspPredicateExps = readAllSuspiciousPredicateExpressions(this.getSuspiciousCodeTree());

        // Conditional expression.

        for (Map.Entry<ITree, Integer> entry : allSuspPredicateExps.entrySet()) {
            ITree predicateExp = entry.getKey();

            if (isRangeCheckerPredicate(predicateExp)) {
                // replace buggyCodeStr with fixedCodeStr1;
                getPatches().add(PatchFactory.createReplacePatch(predicateExp, "true"));
                getPatches().add(PatchFactory.createReplacePatch(predicateExp, "false"));
            }
        }
    }

    public static boolean isRangeCheckerPredicate(ITree predicateExp) {
        if (predicateExp == null) return false;
        List<ITree> children = predicateExp.getChildren();
        if (children == null || children.size() < 3) return false;

        // null == expression...  or ...expression == null (or !=)
        for (String rangeCheckMethod : RANGE_CHECK_METHODS) {
            if (predicateExp.getLabel().contains(rangeCheckMethod)) return true;
        }
        return false;
    }


    /**
     * this method is just used for testing.
     * Please: leave it there for the moment. or refactor the code, to be able to test easier...
     */
    public Map<ITree, Integer> proxyProtectedMethodReadSuspCode(ITree suspStmtAst) {
        return readAllSuspiciousPredicateExpressions(suspStmtAst);
    }
}
