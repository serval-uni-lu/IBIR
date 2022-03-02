package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.info.Patch;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static edu.lu.uni.serval.ibir.patterns.InjectCASTIdivCastToDouble.InjectionPatterns.*;
import static edu.lu.uni.serval.ibir.patterns.StringAstParser.expressionToAst;
import static edu.lu.uni.serval.ibir.patterns.StringAstParser.statementsToAst;
import static org.junit.Assert.*;

public class InjectCASTIdivCastToDoubleTest {

    // i1
    private static final String CODE_SAMPLE_1 = "1 / 10d";
    private static final String CODE_SAMPLE_2 = "(5+var) / 10f";

    // i2
    private static final String CODE_SAMPLE_3 = "1.0 / var";
    private static final String CODE_SAMPLE_4 = "10.0 / 10f";

    // i3
    private static final String CODE_SAMPLE_5 = "1.0 / (float) var";
    private static final String CODE_SAMPLE_6 = "10.0 / (double) 10f";

    // i4
    private static final String CODE_SAMPLE_7 = "(float) 7.0 /  var";
    private static final String CODE_SAMPLE_8 = "(double) 80.0 /  80f";
    private static final String CODE_SAMPLE_13 = "(double) var1 /  var2";
    private static final String CODE_SAMPLE_14 = "(float) var1 / (float) var2";

    // i5
    private static final String CODE_SAMPLE_9 = "0.5 * var";
    private static final String CODE_SAMPLE_10 = "0.25 * 80f";
    private static final String CODE_SAMPLE_11 = "0.32 * 80f";
    private static final String CODE_SAMPLE_12 = "0.65 * 80f";

    // FP3o4
    // IP1
    // IP2
    // IP3
    // IP4
    // IP5

    private List<Pair<ITree, InjectCASTIdivCastToDouble.InjectionPatterns>> identifyBuggyExprRes(String code) throws IOException {
        InjectCASTIdivCastToDouble injector = new InjectCASTIdivCastToDouble();
        injector.setSuspiciousCodeStr(code);
        return injector.identifyBuggyExpressions(statementsToAst(code).getChild(0));
    }

    private void testIdentifyBuggyExpressions(String code, InjectCASTIdivCastToDouble.InjectionPatterns expectedPattern, String expectedNode) throws IOException {
        List<Pair<ITree, InjectCASTIdivCastToDouble.InjectionPatterns>> result = identifyBuggyExprRes(code);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(expectedPattern, result.get(0).secondElement);
        assertEquals(expectedNode, result.get(0).firstElement.getLabel());
    }

    @Test
    public void identifyBuggyExpressions_FP2() throws IOException {
        final String CODE_SAMPLE_FP2_0 = "int i = 1/var;";
        testIdentifyBuggyExpressions(CODE_SAMPLE_FP2_0, FP2, "1 / var");
    }

    @Test
    public void identifyBuggyExpressions_FP1() throws IOException {
        final String CODE_SAMPLE_FP1_0 = "int i = var/10;";
        testIdentifyBuggyExpressions(CODE_SAMPLE_FP1_0, FP1o5, "var / 10");
    }

    @Test
    public void identifyBuggyExpressions_FP5() throws IOException {
        final String CODE_SAMPLE_FP5_0 = "int i = var/2;";
        testIdentifyBuggyExpressions(CODE_SAMPLE_FP5_0, FP1o5, "var / 2");
    }

    @Test
    public void identifyBuggyExpressions_FP34() throws IOException {
        final String CODE_SAMPLE = "int i = var1 / var2;";
        testIdentifyBuggyExpressions(CODE_SAMPLE, FP3o4, "var1 / var2");
    }

    @Test
    public void identifyBuggyExpressions_IP1() throws IOException {
        final String CODE_SAMPLE = "int i = var1 / 10d;";
        final String CODE_SAMPLE0 = "int i = var1 / 10f;";
        final String CODE_SAMPLE1 = "int i = var1 / 10.546f;";
        final String CODE_SAMPLE2 = "int i = var1 / 10.546d;";

        testIdentifyBuggyExpressions(CODE_SAMPLE, IP1, "var1 / 10d");
        testIdentifyBuggyExpressions(CODE_SAMPLE0, IP1, "var1 / 10f");
        testIdentifyBuggyExpressions(CODE_SAMPLE1, IP1, "var1 / 10.546f");
        testIdentifyBuggyExpressions(CODE_SAMPLE2, IP1, "var1 / 10.546d");

        final String CODE_SAMPLE3 = "int i = 5 / 10.546d;";
        testIdentifyBuggyExpressions(CODE_SAMPLE3, FP2, "5 / 10.546d"); // todo may be enlarge this to handle another version of IP1 like switching the "d" to "f".

        final String CODE_SAMPLE4 = "int i = 5.1 / 10.546d;";
        assertTrue(identifyBuggyExprRes(CODE_SAMPLE4).isEmpty()); // todo may be enlarge this to handle another version of IP1...

        final String CODE_SAMPLE5 = "int i = 5.1d / 10.546d;"; // todo may be enlarge this to handle another version of IP1...
        assertTrue(identifyBuggyExprRes(CODE_SAMPLE5).isEmpty());
    }

    private String divExpressionToStatement(String expression) {
        return "int i = " + expression + ";";
    }

    @Test
    public void identifyBuggyExpressions_IP2() throws IOException {
        final String expectedNodeLabel = "1.0 / var";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel), IP2, expectedNodeLabel);

        final String expectedNodeLabel0 = "1.0 / 10f";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel0), IP2, expectedNodeLabel0);

        final String expectedNodeLabel1 = "50.5 / 10.546f";
        assertTrue(identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel1)).isEmpty());

        final String expectedNodeLabel2 = "50.5d / 10.546d";
        assertTrue(identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel2)).isEmpty());
    }

    @Test
    public void identifyBuggyExpressions_IP3() throws IOException {
        final String expectedNodeLabel = "1.0 / (double)var";
        List<Pair<ITree, InjectCASTIdivCastToDouble.InjectionPatterns>> res = identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel));
        assertEquals(2, res.size());
        assertTrue(IP2.equals(res.get(0).secondElement) && IP3.equals(res.get(1).secondElement) || IP2.equals(res.get(1).secondElement) && IP3.equals(res.get(0).secondElement));
        assertEquals(expectedNodeLabel, res.get(0).firstElement.getLabel());
        assertEquals(expectedNodeLabel, res.get(1).firstElement.getLabel());

        final String expectedNodeLabel0 = "(double)var1 / (double)var2";
        res = identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel0));
        assertEquals(2, res.size());
        assertTrue(IP4.equals(res.get(0).secondElement) && IP3.equals(res.get(1).secondElement) || IP4.equals(res.get(1).secondElement) && IP3.equals(res.get(0).secondElement));
        assertEquals(expectedNodeLabel0, res.get(0).firstElement.getLabel());
        assertEquals(expectedNodeLabel0, res.get(1).firstElement.getLabel());

        final String expectedNodeLabel1 = "50.5 / (float)var2";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel1), IP3, expectedNodeLabel1);

        final String expectedNodeLabel2 = "50.5d / (float)10.546d";
        assertTrue(identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel2)).isEmpty()); // todo may be enlarge this to handle another version of IP3...
        // testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel2), IP3, expectedNodeLabel2);
    }

    @Test
    public void identifyBuggyExpressions_IP4() throws IOException {
        final String expectedNodeLabel = "(double)1.0 / var";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel), IP4, expectedNodeLabel);

        final String expectedNodeLabel0 = "(double)var1 / (double)var2";
        List<Pair<ITree, InjectCASTIdivCastToDouble.InjectionPatterns>> res = identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel0));
        assertEquals(2, res.size());
        assertTrue(IP4.equals(res.get(0).secondElement) && IP3.equals(res.get(1).secondElement) || IP4.equals(res.get(1).secondElement) && IP3.equals(res.get(0).secondElement));
        assertEquals(expectedNodeLabel0, res.get(0).firstElement.getLabel());
        assertEquals(expectedNodeLabel0, res.get(1).firstElement.getLabel());

        final String expectedNodeLabel1 = "(float)var2 / 50.5";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel1), IP4, expectedNodeLabel1);

        final String expectedNodeLabel2 = "(float)50.5d / 10.546d";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel2), IP4, expectedNodeLabel2);
    }

    @Test
    public void identifyBuggyExpressions_IP5() throws IOException {
        final String expectedNodeLabel = "0.5 * var";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel), IP5, expectedNodeLabel);

        final String expectedNodeLabel0 = "0.23 * 100";
        testIdentifyBuggyExpressions(divExpressionToStatement(expectedNodeLabel0), IP5, expectedNodeLabel0);

        final String expectedNodeLabel1 = "0.23d * 100";
        assertTrue(identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel1)).isEmpty()); // todo may be enlarge this to handle another version of IP5...

        final String expectedNodeLabel2 = "0.23f * 100";
        assertTrue(identifyBuggyExprRes(divExpressionToStatement(expectedNodeLabel2)).isEmpty()); // todo may be enlarge this to handle another version of IP5...
    }

    @Test
    public void i5changeMultiplicationToDivision() throws IOException {
        InjectCASTIdivCastToDouble injector = new InjectCASTIdivCastToDouble();
        injector.setSuspiciousCodeStr(CODE_SAMPLE_9);
        injector.i5changeMultiplicationToDivision(expressionToAst(CODE_SAMPLE_9), "", "");
        injector.setSuspiciousCodeStr(CODE_SAMPLE_10);
        injector.i5changeMultiplicationToDivision(expressionToAst(CODE_SAMPLE_10), "", "");
        injector.setSuspiciousCodeStr(CODE_SAMPLE_11);
        injector.i5changeMultiplicationToDivision(expressionToAst(CODE_SAMPLE_11), "", "");
        injector.setSuspiciousCodeStr(CODE_SAMPLE_12);
        injector.i5changeMultiplicationToDivision(expressionToAst(CODE_SAMPLE_12), "", "");
        List<Patch> patches = injector.getPatches();
        HashSet<Object> fixedCodes = new HashSet<>();
        for (Patch patch : patches) {
            fixedCodes.add(patch.getFixedCodeStr1());
        }
        Assert.assertTrue(fixedCodes.contains("var/2"));
        Assert.assertTrue(fixedCodes.contains("80f/4"));
        Assert.assertTrue(fixedCodes.contains("80f/3"));
        Assert.assertTrue(fixedCodes.contains("80f/1"));
    }

    @Test
    public void i4removeDividendCast() throws IOException {
        InjectCASTIdivCastToDouble injector = new InjectCASTIdivCastToDouble();
        injector.setSuspiciousCodeStr(CODE_SAMPLE_7);
        injector.i4removeDividendCast(expressionToAst(CODE_SAMPLE_7), "", "", CODE_SAMPLE_7.length());
        injector.setSuspiciousCodeStr(CODE_SAMPLE_8);
        injector.i4removeDividendCast(expressionToAst(CODE_SAMPLE_8), "", "", CODE_SAMPLE_8.length());
        injector.setSuspiciousCodeStr(CODE_SAMPLE_13);
        injector.i4removeDividendCast(expressionToAst(CODE_SAMPLE_13), "", "", CODE_SAMPLE_13.length());
        injector.setSuspiciousCodeStr(CODE_SAMPLE_14);
        injector.i4removeDividendCast(expressionToAst(CODE_SAMPLE_14), "", "", CODE_SAMPLE_14.length());
        List<Patch> patches = injector.getPatches();
        HashSet<Object> fixedCodes = new HashSet<>();
        for (Patch patch : patches) {
            fixedCodes.add(patch.getFixedCodeStr1());
        }
        Assert.assertTrue(fixedCodes.contains("7.0 /  var"));
        Assert.assertTrue(fixedCodes.contains("80.0 /  80f"));
        Assert.assertTrue(fixedCodes.contains("var1 /  var2"));
        Assert.assertTrue(fixedCodes.contains("var1 / (float) var2"));
    }

    @Test
    public void i1removeDivisorDorF() throws IOException {
        InjectCASTIdivCastToDouble injector = new InjectCASTIdivCastToDouble();
        injector.setSuspiciousCodeStr(CODE_SAMPLE_1);
        injector.i1removeDivisorDorF(expressionToAst(CODE_SAMPLE_1), "", "", 0);
        injector.setSuspiciousCodeStr(CODE_SAMPLE_2);
        injector.i1removeDivisorDorF(expressionToAst(CODE_SAMPLE_2), "", "", 0);
        List<Patch> patches = injector.getPatches();
        HashSet<Object> fixedCodes = new HashSet<>();
        for (Patch patch : patches) {
            fixedCodes.add(patch.getFixedCodeStr1());
        }
        Assert.assertTrue(fixedCodes.contains("1 / 10"));
        Assert.assertTrue(fixedCodes.contains("(5+var) / 10"));
    }

    @Test
    public void i2removeComaZero() throws IOException {
        InjectCASTIdivCastToDouble injector = new InjectCASTIdivCastToDouble();
        injector.setSuspiciousCodeStr(CODE_SAMPLE_3);
        injector.i2removeComaZero(expressionToAst(CODE_SAMPLE_3), "", "", CODE_SAMPLE_3.length());
        injector.setSuspiciousCodeStr(CODE_SAMPLE_4);
        injector.i2removeComaZero(expressionToAst(CODE_SAMPLE_4), "", "", CODE_SAMPLE_4.length());
        List<Patch> patches = injector.getPatches();
        HashSet<Object> fixedCodes = new HashSet<>();
        for (Patch patch : patches) {
            fixedCodes.add(patch.getFixedCodeStr1());
        }
        Assert.assertTrue(fixedCodes.contains("1 / var"));
        Assert.assertTrue(fixedCodes.contains("10 / 10f"));
    }

    @Test
    public void i3removeDivisorCast() throws IOException {
        InjectCASTIdivCastToDouble injector = new InjectCASTIdivCastToDouble();
        injector.setSuspiciousCodeStr(CODE_SAMPLE_5);
        injector.i3removeDivisorCast(expressionToAst(CODE_SAMPLE_5), "", "", 0);
        injector.setSuspiciousCodeStr(CODE_SAMPLE_6);
        injector.i3removeDivisorCast(expressionToAst(CODE_SAMPLE_6), "", "", 0);
        List<Patch> patches = injector.getPatches();
        HashSet<Object> fixedCodes = new HashSet<>();
        for (Patch patch : patches) {
            fixedCodes.add(patch.getFixedCodeStr1());
        }
        Assert.assertTrue(fixedCodes.contains("1.0 /  var"));
        Assert.assertTrue(fixedCodes.contains("10.0 /  10f"));
    }
}