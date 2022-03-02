package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.tbar.fixpatterns.ConditionalExpressionMutator;

public class IbirConditionalExpressionMutator extends ConditionalExpressionMutator implements NamedPatterns{

    private static final String PATTERN_NAME_PREFIX = "ConditionMut_";
    private int typeCopy = 0;
    public IbirConditionalExpressionMutator(int type) {
        super(type);
        this.typeCopy = type; // the type variable is private in inherited class.
    }

    @Override
    public String getName() {
        return PATTERN_NAME_PREFIX + (2 == typeCopy? "Adder" : "Remover");
    }
}
