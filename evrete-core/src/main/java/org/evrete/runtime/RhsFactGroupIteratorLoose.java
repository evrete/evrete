package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;

//TODO !!!! optimize by preconfiguring main and delta iterators
public class RhsFactGroupIteratorLoose extends RhsFactGroupIterator {
    private final RuntimeFactTypePlain[] types;
    private final int lastIndex;
    private final RuntimeFact[] state;

    public RhsFactGroupIteratorLoose(int factGroupId, RuntimeFactTypePlain[] types, RuntimeFact[][] factState) {
        this.types = types;
        this.state = factState[factGroupId];
        this.lastIndex = types.length - 1;
    }

    @Override
    public void forEachFact() {
        run(0, false);
    }

    @Override
    public <I extends ReIterator<RuntimeFact>> void setIterators(I[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I extends ReIterable<RuntimeFact>> void setIterables(I[] data) {
        throw new UnsupportedOperationException();
    }

    private void run(int index, boolean hasDelta) {
        PlainMemory memory = types[index].getSource();
        ReIterator<RuntimeFact> it;


        if (index == lastIndex) {
            //Last
            NestedFactRunnable delegate = getDelegate();
            // Main iterator
            it = memory.mainIterator();
            if (hasDelta) {
                while (it.hasNext()) {
                    state[index] = it.next();
                    delegate.forEachFact();
                }
            }

            // Delta iterator
            it = memory.deltaIterator();
            while (it.hasNext()) {
                state[index] = it.next();
                delegate.forEachFact();
            }
        } else {
            // Main iterator
            it = memory.mainIterator();
            while (it.hasNext()) {
                state[index] = it.next();
                run(index+1, hasDelta);
            }

            // Delta iterator
            it = memory.deltaIterator();
            while (it.hasNext()) {
                state[index] = it.next();
                run(index+1, true);
            }
        }
    }
}
