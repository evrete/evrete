package org.evrete.dsl;

public enum DefaultSort {
    BY_NAME(1),
    BY_NAME_INVERSE(-1);

    public static final DefaultSort DEFAULT = DefaultSort.BY_NAME;
    final int modifier;

    DefaultSort(int modifier) {
        this.modifier = modifier;
    }

    public int getModifier() {
        return modifier;
    }
}
