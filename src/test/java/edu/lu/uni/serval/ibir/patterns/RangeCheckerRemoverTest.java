package edu.lu.uni.serval.ibir.patterns;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static edu.lu.uni.serval.ibir.patterns.StringAstParser.expressionToAst;

public class RangeCheckerRemoverTest {

    @Test
    public void isRangeCheckerPredicate() throws IOException {
        Assert.assertTrue(RangeCheckerRemover.isRangeCheckerPredicate(expressionToAst("var4.length == 5")));
        Assert.assertTrue(RangeCheckerRemover.isRangeCheckerPredicate(expressionToAst("var2.size() != va3.size()")));
        Assert.assertTrue(RangeCheckerRemover.isRangeCheckerPredicate(expressionToAst("var4.length == 5")));
        Assert.assertTrue(RangeCheckerRemover.isRangeCheckerPredicate(expressionToAst("var.length() > x && var2.size() != va3.size() || var4.length == 5")));
    }

}