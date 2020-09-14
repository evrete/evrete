package org.evrete.runtime;

interface ActivationSubject {
    boolean isInActiveState();

    void resetState();

    default boolean readState(ActivationSubject[] others) {
        boolean b = false;
        for (ActivationSubject subject : others) {
            b |= subject.isInActiveState();
        }
        return b;
    }

    default void resetState(ActivationSubject[] others) {
        for (ActivationSubject subject : others) {
            subject.resetState();
        }
    }
}
