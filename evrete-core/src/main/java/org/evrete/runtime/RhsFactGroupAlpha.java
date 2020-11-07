package org.evrete.runtime;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;

//TODO !!!! optimize by preconfiguring main and delta iterators
public class RhsFactGroupAlpha implements RhsFactGroup {
    private final RhsFactGroupDescriptor descriptor;
    private final RuntimeFactTypePlain[] types;
    private final int lastIndex;
    private final RuntimeFact[] state;

    private long computedCount = -1;
    private boolean hasDelta;

    public RhsFactGroupAlpha(RuntimeRuleImpl rule, RhsFactGroupDescriptor descriptor, RuntimeFact[][] factState) {
        this.descriptor = descriptor;
        this.types = rule.resolve(RuntimeFactTypePlain.class, descriptor.getTypes());
        this.state = factState[descriptor.getFactGroupIndex()];
        this.lastIndex = types.length - 1;
    }

    @Override
    public boolean isAlpha() {
        return true;
    }

    public long getComputedFactCount() {
        if (computedCount < 0) {
            throw new IllegalStateException("Active state not computed");
        } else {
            return computedCount;
        }
    }

    public boolean hasDelta() {
        if (computedCount < 0) {
            throw new IllegalStateException("Active state not computed");
        } else {
            return hasDelta;
        }
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

    @Override
    public boolean isInActiveState() {
        this.computedCount = 1L;
        boolean deltaAvailable = false;
        for (RuntimeFactTypePlain plain : types) {
            plain.isInActiveState();
            PlainMemory memory = plain.getSource();
            long deltaCount = memory.deltaIterator().reset();
            deltaAvailable |= (deltaCount > 0);
            long totalMemoryObjects = deltaCount + memory.mainIterator().reset();
            this.computedCount *= totalMemoryObjects;
        }

        this.hasDelta = deltaAvailable;
        return deltaAvailable;
    }


    @Override
    public void resetState() {
        resetState(types);
        this.computedCount = -1;
        this.hasDelta = false;
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

    private void set(int index, RuntimeFact fact) {
        state[index] = fact;
    }

    private void runDelta(int index, boolean hasDelta, Runnable r) {
        PlainMemory memory = types[index].getSource();
        ReIterator<RuntimeFact> it;

        if (index == lastIndex) {
            //Last
            // Main iterator
            it = memory.mainIterator();
            if (hasDelta && it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    r.run();
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    r.run();
                }
            }
        } else {
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    runDelta(index + 1, hasDelta, r);
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    runDelta(index + 1, true, r);
                }
            }
        }
    }

    private void runFull(int index, Runnable r) {
        PlainMemory memory = types[index].getSource();
        ReIterator<RuntimeFact> it;

        if (index == lastIndex) {
            //Last
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    r.run();
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    r.run();
                }
            }
        } else {
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    runFull(index + 1, r);
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    runFull(index + 1, r);
                }
            }
        }
    }

    private void runKnown(int index, Runnable r) {
        PlainMemory memory = types[index].getSource();
        ReIterator<RuntimeFact> it;

        if (index == lastIndex) {
            //Last
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    r.run();
                }
            }
        } else {
            // Main iterator
            it = memory.mainIterator();
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    set(index, it.next());
                    runKnown(index + 1, r);
                }
            }
        }
    }

}
