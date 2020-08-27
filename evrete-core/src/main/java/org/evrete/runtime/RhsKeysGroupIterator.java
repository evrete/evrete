package org.evrete.runtime;

import org.evrete.api.*;

import java.util.EnumMap;
import java.util.function.BooleanSupplier;

public class RhsKeysGroupIterator implements KeyIteratorsBundle<ValueRow[]> {
    private static final BooleanSupplier TRUE_PREDICATE = () -> true;
    private final RhsFactGroupIterator groupIterator;
    private final ValueRow[][] state;
    private final int keyGroupId;
    private BooleanSupplier statePredicate = TRUE_PREDICATE;
    private final KeyIteratorsBundle<ValueRow[]> endNode;

    public RhsKeysGroupIterator(int keyGroupId, KeyIteratorsBundle<ValueRow[]> endNode, RhsFactGroupIterator groupIterator, ValueRow[][] state) {
        this.groupIterator = groupIterator;
        this.keyGroupId = keyGroupId;
        this.state = state;
        this.endNode = endNode;
    }

/*
    static RhsKeysGroupIterator factory(int keyGroupId, RhsFactGroupDescriptor groupDescriptor, RhsFactGroupIterator groupIterator, ReIterator<ValueRow[]> mainIterator, ReIterator<ValueRow[]> deltaIterator, RuntimeFactTypeKeyed[] rtFactTypes, ValueRow[][] state) {

        return groupDescriptor.isAllUniqueKeysAndAlpha() ?
                new WithIterators(keyGroupId, groupIterator, mainIterator, deltaIterator, rtFactTypes, state)
                :
                new WithIterables(keyGroupId, groupIterator, mainIterator, deltaIterator, rtFactTypes, state);

    }
*/

    @Override
    public EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators() {
        return endNode.keyIterators();
    }

    void initFactIterators(ValueRow[] key) {
        groupIterator.setIterables(key);
    }

/*
    @Override
    public void run() {
        runForEach(runnable);
    }
*/

    public void addStateKeyPredicate(BooleanSupplier predicate) {
        if (this.statePredicate == TRUE_PREDICATE) {
            this.statePredicate = predicate;
        } else {
            BooleanSupplier old = this.statePredicate;
            this.statePredicate = () -> old.getAsBoolean() && predicate.getAsBoolean();
        }
    }

/*
    public ReIterator<ValueRow[]> getMainIterator() {
        return mainIterator;
    }

    public ReIterator<ValueRow[]> getDeltaIterator() {
        return deltaIterator;
    }
*/

/*
    @Override
    public void runForEach(Runnable r) {
        if (mainIterator.reset() == 0) return;
        ValueRow[] next;
        while (mainIterator.hasNext()) {
            next = (state[keyGroupId] = mainIterator.next());
            if (statePredicate.getAsBoolean()) {
                initFactIterators(next);
                runnable.run();
            }
        }
    }
*/

    void setFactIterables(ValueRow[] next) {
        groupIterator.setIterables(next);
    }

    void setFactIterators(ValueRow[] next) {
        groupIterator.setIterators(next);
    }

/*
    abstract void initFactIterators(ValueRow[] next);
*/

/*
    private static class WithIterators extends RhsKeysGroupIterator {
        WithIterators(int keyGroupId, RhsFactGroupIterator groupIterator, ReIterator<ValueRow[]> mainIterator, ReIterator<ValueRow[]> deltaIterator, RuntimeFactTypeKeyed[] rtFactTypes, ValueRow[][] state) {
            super(keyGroupId, groupIterator, mainIterator, deltaIterator, rtFactTypes, state);
        }

        @Override
        void initFactIterators(ValueRow[] next) {
            setFactIterators(next);
        }
    }

    private static class WithIterables extends RhsKeysGroupIterator {
        WithIterables(int keyGroupId, RhsFactGroupIterator groupIterator, ReIterator<ValueRow[]> mainIterator, ReIterator<ValueRow[]> deltaIterator, RuntimeFactTypeKeyed[] rtFactTypes, ValueRow[][] state) {
            super(keyGroupId, groupIterator, mainIterator, deltaIterator, rtFactTypes, state);
        }

        @Override
        void initFactIterators(ValueRow[] next) {
            setFactIterables(next);
        }
    }
*/

}
