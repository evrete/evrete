package org.evrete.runtime;

public interface RhsFactGroup extends ActivationSubject {
    boolean isAlpha();

    int getIndex();

    <T extends RuntimeFactType> T[] getTypes();
}
