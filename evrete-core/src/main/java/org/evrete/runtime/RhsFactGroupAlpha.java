package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ReIterator;

//TODO optimize by preconfiguring main and delta iterators
//TODO !!!! use RuntimeAware as parent class
public class RhsFactGroupAlpha extends AbstractRhsFactGroup implements RhsFactGroup {
    private final RhsFactGroupDescriptor descriptor;
    private final RuntimeFactTypePlain[] types;

    RhsFactGroupAlpha(SessionMemory runtime, RhsFactGroupDescriptor descriptor, RuntimeFactTypePlain[] types, FactIterationState[][] factState) {
        super(runtime, factState[descriptor.getFactGroupIndex()]);
        this.descriptor = descriptor;
        this.types = types;
    }

    @Override
    public boolean isAlpha() {
        return true;
    }

    boolean hasDelta() {
        for (RuntimeFactTypePlain plain : types) {
            if (plain.getSource().deltaIterator().reset() > 0) return true;
        }
        return false;
    }

    @Override
    public int getIndex() {
        return descriptor.getFactGroupIndex();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RuntimeFactTypePlain[] getTypes() {
        return types;
    }

    void run(ScanMode mode, Runnable r) {
        switch (mode) {
            case DELTA:
                runDelta(0, false, r);
                return;
            case FULL:
                runFull(0, r);
                return;
            case KNOWN:
                runKnown(0, r);
                return;
            default:
                throw new IllegalStateException();
        }
    }

/*
    private boolean next(int index, ReIterator<FactHandle> it) {
        RuntimeFact fact = it.next();
        if (fact.isDeleted()) {
            it.remove();
            return false;
        } else {
            state[index] = fact;
            return true;
        }
    }
*/


    private void runDelta(int index, boolean hasDelta, Runnable r) {
        PlainMemory memory = types[index].getSource();
        ReIterator<FactHandleVersioned> it;
        FactIterationState state = this.state[index];
        if (index == lastIndex) {
            //Last
            // Main iterator
            it = memory.mainIterator();
            if (hasDelta && it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        r.run();
                    }
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        r.run();
                    }
                }
            }
        } else {
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        runDelta(index + 1, hasDelta, r);
                    }
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        runDelta(index + 1, true, r);
                    }
                }
            }
        }
    }

    private void runFull(int index, Runnable r) {
        PlainMemory memory = types[index].getSource();
        ReIterator<FactHandleVersioned> it;
        FactIterationState state = this.state[index];

        if (index == lastIndex) {
            //Last
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        r.run();
                    }
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        r.run();
                    }
                }
            }
        } else {
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        runFull(index + 1, r);
                    }
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        runFull(index + 1, r);
                    }
                }
            }
        }
    }

    private void runKnown(int index, Runnable r) {
        PlainMemory memory = types[index].getSource();
        final ReIterator<FactHandleVersioned> it = memory.mainIterator();
        FactIterationState state = this.state[index];

        if (index == lastIndex) {
            //Last
            // Main iterator
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        r.run();
                    }
                }
            }
        } else {
            // Main iterator
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    if (next(state, it)) {
                        runKnown(index + 1, r);
                    }
                }
            }
        }
    }

}
