package org.evrete.api;

public interface RuntimeFact extends FieldToValue {

    <T> T getDelegate();

    Object[] getValues();

    boolean[] getAlphaTests();

    @SuppressWarnings("unchecked")
    default <T> T getValue(int i) {
        return (T) getValues()[i];
    }
}
