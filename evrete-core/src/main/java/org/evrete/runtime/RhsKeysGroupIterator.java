package org.evrete.runtime;

import org.evrete.api.EachRunnable;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;

import java.util.function.BooleanSupplier;

public abstract class RhsKeysGroupIterator implements EachRunnable, Runnable {
    private static final BooleanSupplier TRUE_PREDICATE = () -> true;
    private final ReIterator<ValueRow[]> mainIterator;
    private final ReIterator<ValueRow[]> deltaIterator;
    private final RhsFactGroupIterator groupIterator;
    private Runnable runnable;
    private final ValueRow[][] state;
    private final int keyGroupId;
    private BooleanSupplier statePredicate = TRUE_PREDICATE;

    RhsKeysGroupIterator(int keyGroupId, RhsFactGroupIterator groupIterator, ReIterator<ValueRow[]> mainIterator, ReIterator<ValueRow[]> deltaIterator, ValueRow[][] state) {
        this.mainIterator = mainIterator;
        this.deltaIterator = deltaIterator;
        this.groupIterator = groupIterator;
        this.keyGroupId = keyGroupId;
        this.state = state;
    }

    static RhsKeysGroupIterator factory(int keyGroupId, RhsFactGroupDescriptor groupDescriptor, RhsFactGroupIterator groupIterator, ReIterator<ValueRow[]> mainIterator, ReIterator<ValueRow[]> deltaIterator, ValueRow[][] state) {
        return groupDescriptor.isAllUniqueKeysAndAlpha() ?
                new WithIterators(keyGroupId, groupIterator, mainIterator, deltaIterator, state)
                :
                new WithIterables(keyGroupId, groupIterator, mainIterator, deltaIterator, state);

    }

    @Override
    public void run() {
        runForEach(runnable);
    }

    public void addStateKeyPredicate(BooleanSupplier predicate) {
        if (this.statePredicate == TRUE_PREDICATE) {
            this.statePredicate = predicate;
        } else {
            BooleanSupplier old = this.statePredicate;
            this.statePredicate = () -> old.getAsBoolean() && predicate.getAsBoolean();
        }
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public ReIterator<ValueRow[]> getMainIterator() {
        return mainIterator;
    }

    public ReIterator<ValueRow[]> getDeltaIterator() {
        return deltaIterator;
    }

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

    void setFactIterables(ValueRow[] next) {
        groupIterator.setIterables(next);
    }

    void setFactIterators(ValueRow[] next) {
        groupIterator.setIterators(next);
    }

    abstract void initFactIterators(ValueRow[] next);

    private static class WithIterators extends RhsKeysGroupIterator {
        WithIterators(int keyGroupId, RhsFactGroupIterator groupIterator, ReIterator<ValueRow[]> mainIterator, ReIterator<ValueRow[]> deltaIterator, ValueRow[][] state) {
            super(keyGroupId, groupIterator, mainIterator, deltaIterator, state);
        }

        @Override
        void initFactIterators(ValueRow[] next) {
            setFactIterators(next);
        }
    }

    private static class WithIterables extends RhsKeysGroupIterator {
        WithIterables(int keyGroupId, RhsFactGroupIterator groupIterator, ReIterator<ValueRow[]> mainIterator, ReIterator<ValueRow[]> deltaIterator, ValueRow[][] state) {
            super(keyGroupId, groupIterator, mainIterator, deltaIterator, state);
        }

        @Override
        void initFactIterators(ValueRow[] next) {
            setFactIterables(next);
        }
    }


}
