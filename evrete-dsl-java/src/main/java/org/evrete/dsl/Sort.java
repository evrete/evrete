package org.evrete.dsl;

public enum Sort {
    BY_NAME(1),
    BY_NAME_INVERSE(-1);

    public static final Sort DEFAULT = Sort.BY_NAME;
    final int modifier;

    Sort(int modifier) {
        this.modifier = modifier;
    }

    public int getModifier() {
        return modifier;
    }
}
