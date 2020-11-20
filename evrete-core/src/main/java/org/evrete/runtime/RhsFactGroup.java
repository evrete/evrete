package org.evrete.runtime;

public interface RhsFactGroup {
    boolean isAlpha();

    int getIndex();

    <T extends RuntimeFactType> T[] getTypes();
}
