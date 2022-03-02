package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.ibir.info.IBIrPatch;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.info.Patch;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static edu.lu.uni.serval.ibir.patterns.DummyData.*;
import static edu.lu.uni.serval.ibir.patterns.StringAstParser.expressionToAst;
import static edu.lu.uni.serval.ibir.patterns.StringAstParser.statementsToAst;

/**
 * Lang 12 can be a great example to test this.
 */
public class NullPointerCheckerRemoverTest {


    private static final Set<String> CODE_SAMPLE_EXPECTED_SCN_1 = new HashSet<String>() {{
        add("var == null");
        add("var3 == null");
        add("var2 != null");
    }};
    private static final Set<String> CODE_SAMPLE_EXPECTED_SCN_2 = new HashSet<String>() {{
        add("var == null");
    }};
    private static final Set<String> CODE_SAMPLE_EXPECTED_SCN_3 = new HashSet<String>() {{
        add("null == var");
    }};


    @Test
    public void isNullCheckerPredicate() throws IOException {
        Assert.assertTrue(NullPointerCheckerRemover.isNullCheckerPredicate(expressionToAst("var==null")));
        Assert.assertTrue(NullPointerCheckerRemover.isNullCheckerPredicate(expressionToAst("null==var")));
        Assert.assertTrue(NullPointerCheckerRemover.isNullCheckerPredicate(expressionToAst("var!=null")));
        Assert.assertTrue(NullPointerCheckerRemover.isNullCheckerPredicate(expressionToAst("null!=var")));

        Assert.assertFalse(NullPointerCheckerRemover.isNullCheckerPredicate(expressionToAst("return null")));
        Assert.assertFalse(NullPointerCheckerRemover.isNullCheckerPredicate(expressionToAst("var=null")));
    }

    @Test
    public void generatePatches0() throws IOException {
        NullPointerCheckerRemover nullPointerCheckerRemover = new NullPointerCheckerRemover();
        nullPointerCheckerRemover.setSuspiciousCodeStr(CODE_SAMPLE_0);
        nullPointerCheckerRemover.generatePatches(statementsToAst(CODE_SAMPLE_0));
        Assert.assertFalse(nullPointerCheckerRemover.getPatches().isEmpty());
        for (Patch patch : nullPointerCheckerRemover.getPatches()) {
            Assert.assertTrue(patch instanceof IBIrPatch);
            Assert.assertFalse(((IBIrPatch) patch).getNeedBugyCode());
            Assert.assertTrue("true".equals(patch.getFixedCodeStr1()) || "false".equals(patch.getFixedCodeStr1()));
        }
    }

    @Test
    public void readAllSuspiciousPredicateExpressions0() throws IOException {
        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_0)).isEmpty());
        Set<ITree> allSuspCodeAst0 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_0)).keySet();
        Assert.assertFalse(allSuspCodeAst0.isEmpty());
        for (ITree iTree : allSuspCodeAst0) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_1.contains(iTree.getLabel()));
        }
    }

    @Test
    public void readAllSuspiciousPredicateExpressions1() throws IOException {
        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_1)).isEmpty());
        Set<ITree> allSuspCodeAst1 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_1)).keySet();
        Assert.assertFalse(allSuspCodeAst1.isEmpty());
        for (ITree iTree : allSuspCodeAst1) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_1.contains(iTree.getLabel()));
        }
    }

    @Test
    public void readAllSuspiciousPredicateExpressions2() throws IOException {
        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_2)).isEmpty());
        Set<ITree> allSuspCodeAst2 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_2)).keySet();
        Assert.assertFalse(allSuspCodeAst2.isEmpty());
        for (ITree iTree : allSuspCodeAst2) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_2.contains(iTree.getLabel()));
        }
    }


    @Test
    public void readAllSuspiciousPredicateExpressions() throws IOException {

        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_3)).isEmpty());
        Set<ITree> allSuspCodeAst3 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_3)).keySet();
        Assert.assertFalse(allSuspCodeAst3.isEmpty());
        for (ITree iTree : allSuspCodeAst3) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_3.contains(iTree.getLabel()));
        }

        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_4)).isEmpty());
        Set<ITree> allSuspCodeAst4 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_4)).keySet();
        Assert.assertFalse(allSuspCodeAst4.isEmpty());
        for (ITree iTree : allSuspCodeAst4) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_3.contains(iTree.getLabel()));
        }

        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_5)).isEmpty());
        Set<ITree> allSuspCodeAst5 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_5)).keySet();
        Assert.assertFalse(allSuspCodeAst5.isEmpty());
        for (ITree iTree : allSuspCodeAst5) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_3.contains(iTree.getLabel()));
        }

        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_7)).isEmpty());
        Set<ITree> allSuspCodeAst7 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_7)).keySet();
        Assert.assertFalse(allSuspCodeAst7.isEmpty());
        for (ITree iTree : allSuspCodeAst7) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_3.contains(iTree.getLabel()));
        }

        Assert.assertTrue(new TestConditionalExpressionMutator(0).proxyProtectedMethodReadSuspCode(statementsToAst(CODE_SAMPLE_8)).isEmpty());
        Set<ITree> allSuspCodeAst8 = new NullPointerCheckerRemover().readAllSuspiciousPredicateExpressions(statementsToAst(CODE_SAMPLE_8)).keySet();
        Assert.assertFalse(allSuspCodeAst8.isEmpty());
        for (ITree iTree : allSuspCodeAst8) {
            Assert.assertTrue(CODE_SAMPLE_EXPECTED_SCN_3.contains(iTree.getLabel()));
        }
    }

}