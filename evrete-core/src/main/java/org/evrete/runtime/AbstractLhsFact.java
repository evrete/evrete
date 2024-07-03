package org.evrete.runtime;

/**
 * Base class for every LHS (Left-Hand Side) fact declaration.
 */
public abstract class AbstractLhsFact {

    private final int inRuleIndex;
    private final String varName;

    /**
     * Base constructor.
     *
     * @param inRuleIndex the index (order) of this fact declaration inside the rule.
     * @param varName     the variable name of the fact declaration, for example, "$customer".
     */
    protected AbstractLhsFact(int inRuleIndex, String varName) {
        this.inRuleIndex = inRuleIndex;
        this.varName = varName;
    }

    /**
     * A copy-all constructor for subclasses
     * @param other parent instance
     */
    protected AbstractLhsFact(AbstractLhsFact other) {
        this.inRuleIndex = other.inRuleIndex;
        this.varName = other.varName;
    }



    /**
     * Returns the index of this fact declaration inside the rule.
     *
     * @return the index of this fact declaration inside the rule.
     */
    public int getInRuleIndex() {
        return inRuleIndex;
    }

    /**
     * Returns the variable name of this fact declaration.
     *
     * @return the variable name
     */
    public String getVarName() {
        return varName;
    }


}
