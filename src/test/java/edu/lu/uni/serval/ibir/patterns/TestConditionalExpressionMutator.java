package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixpatterns.ConditionalExpressionMutator;

import java.util.Map;

public class TestConditionalExpressionMutator extends ConditionalExpressionMutator {

    public TestConditionalExpressionMutator(int type) {
        super(type);
    }

    /**
     *     this method is just used for testing.
     *     Please: leave it there for the moment. or refactor the code, to be able to test easier...
     */
    public Map<ITree, Integer> proxyProtectedMethodReadSuspCode(ITree suspStmtAst){
        return readAllSuspiciousPredicateExpressions(suspStmtAst);
    }
}
