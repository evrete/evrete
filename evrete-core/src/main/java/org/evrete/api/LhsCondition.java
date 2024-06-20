package org.evrete.api;

/**
 * A generic class for representing LHS conditions.
 *
 * @param <C>     the condition itself, usually a {@link EvaluatorHandle}
 * @param <Fact>  the type of the referenced fact
 * @param <Field> the type of the referenced field
 */
public class LhsCondition<C, Fact, Field> {
    private C condition;
    private final LhsField.Array<Fact, Field> descriptor;

    public LhsCondition(C condition, LhsField.Array<Fact, Field> descriptor) {
        this.condition = condition;
        this.descriptor = descriptor;
    }

    public LhsCondition(LhsCondition<C, ?, ?> other, LhsField.Array<Fact, Field> descriptor) {
        this(other.condition, descriptor);
    }

    public LhsField.Array<Fact, Field> getDescriptor() {
        return descriptor;
    }


    public void setCondition(C condition) {
        this.condition = condition;
    }

    public C getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "{" +
                "condition=" + condition +
                ", descriptor=" + descriptor +
                '}';
    }
}
