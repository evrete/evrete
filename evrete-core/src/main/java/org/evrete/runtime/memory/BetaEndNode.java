package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.collections.CollectionReIterator;
import org.evrete.runtime.*;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

//TODO !!! optimize by sharing the value array and pre-built functions
public class BetaEndNode extends BetaConditionNode implements KeyReIterators<ValueRow[]> {
    public static final BetaEndNode[] ZERO_ARRAY = new BetaEndNode[0];
    private final List<ValueRow[]> oldKeysNewFacts;
    private final EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators = new EnumMap<>(KeyMode.class);
    private final RuntimeFactTypeKeyed[] entryNodes;

    public BetaEndNode(RuntimeRuleImpl rule, ConditionNodeDescriptor nodeDescriptor) {
        super(rule, nodeDescriptor, create(nodeDescriptor.getSources(), rule));
        FactType[] factTypes = nodeDescriptor.getEvalGrouping()[0];
        assert factTypes.length == nodeDescriptor.getTypes().length;

        //this.oldKeysNewFacts = rule.getMemory().newKeysStore(getDescriptor().getEvalGrouping());
        this.oldKeysNewFacts = new LinkedList<>();
        for (KeyMode mode : KeyMode.values()) {
            RhsKeyIterator modeIterator;
            switch (mode) {
                case NEW_KEYS_NEW_FACTS:
                    modeIterator = new ModeIteratorDelegate(
                            mode,
                            deltaIterator()
                    );
                    break;
                case KNOWN_KEYS_KNOWN_FACTS:
                    modeIterator = new ModeIteratorDelegate(
                            mode,
                            mainIterator()
                    );
                    break;
                case KNOWN_KEYS_NEW_FACTS:
                    ReIterator<ValueRow[]> listReIterator = new CollectionReIterator<>(oldKeysNewFacts);
                    modeIterator = new ModeIteratorDelegate(
                            mode,
                            listReIterator
                    );
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            this.keyIterators.put(mode, modeIterator);
        }
        this.entryNodes = getEntryNodes();
    }

    private static BetaMemoryNode<?>[] create(NodeDescriptor[] sources, RuntimeRuleImpl rule) {
        BetaMemoryNode<?>[] result = new BetaMemoryNode<?>[sources.length];
        for (int i = 0; i < sources.length; i++) {
            result[i] = create(rule, sources[i]);
        }
        return result;
    }

    // TODO !!!!! optimize, create a set of involved types
    public boolean dependsOn(Type<?> type) {
        for (RuntimeFactTypeKeyed entry : entryNodes) {
            if (entry.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static BetaMemoryNode<?> create(RuntimeRuleImpl rule, NodeDescriptor desc) {
        if (desc.isConditionNode()) {
            return new BetaConditionNode(
                    rule, (ConditionNodeDescriptor) desc,
                    create(desc.getSources(), rule)
            );
        } else {
            EntryNodeDescriptor descriptor = (EntryNodeDescriptor) desc;
            return new BetaEntryNode(descriptor, rule.resolve(descriptor.getFactType()));
        }
    }

    @Override
    public void computeDelta(boolean deltaOnly) {
        // Merge previous delta


        super.computeDelta(deltaOnly);
        // TODO create a single Terminal node which would keep only delta
        // Cleaning deleted items
        ReIterator<KeysStore.Entry> deleteIterator = getMainStore().entries();
        if (deleteIterator.reset() > 0) {
            while (deleteIterator.hasNext()) {
                if (!isEntryNonDeleted(deleteIterator.next())) {
                    deleteIterator.remove();
                }
            }
        }

        //TODO !!!! check if delta check applies here
        if (deltaOnly) {
            ValueRow[] array = new ValueRow[entryNodes.length];
            this.oldKeysNewFacts.clear();
            computeOldKeysNewFacts(0, false, array);
            //System.out.println("Computed delta " + oldKeysNewFacts);
        }
    }

    private void computeOldKeysNewFacts(int index, boolean oldKeysNewFactsPresent, ValueRow[] array) {
        KeyReIterators<ValueRow> entry = entryNodes[index].getKeyIterators();
        ReIterator<ValueRow> knownKeysKnownFacts = entry.keyIterator(KeyMode.KNOWN_KEYS_KNOWN_FACTS);
        ReIterator<ValueRow> knownKeysNewFacts = entry.keyIterator(KeyMode.KNOWN_KEYS_NEW_FACTS);
        ReIterator<ValueRow> newKeysNewFacts = entry.keyIterator(KeyMode.NEW_KEYS_NEW_FACTS);
        ReIterator<ValueRow> it;
        if (index == entryNodes.length - 1) {
            // The last entry
            it = knownKeysNewFacts;
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    array[index] = it.next();
                    testAndSave(array);
                }
            }

            it = knownKeysKnownFacts;
            if (oldKeysNewFactsPresent && it.reset() > 0) {
                while (it.hasNext()) {
                    array[index] = it.next();
                    testAndSave(array);
                }
            }

            it = newKeysNewFacts;
            if (oldKeysNewFactsPresent && it.reset() > 0) {
                while (it.hasNext()) {
                    array[index] = it.next();
                    testAndSave(array);
                }
            }
        } else {
            // A middle entry
            it = knownKeysNewFacts;
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    array[index] = it.next();
                    computeOldKeysNewFacts(index + 1, true, array);
                }
            }

            it = knownKeysKnownFacts;
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    array[index] = it.next();
                    computeOldKeysNewFacts(index + 1, oldKeysNewFactsPresent, array);
                }
            }

            it = newKeysNewFacts;
            if (it.reset() > 0) {
                while (it.hasNext()) {
                    array[index] = it.next();
                    computeOldKeysNewFacts(index + 1, oldKeysNewFactsPresent, array);
                }
            }
        }
    }

    //TODO !!! optimize
    private void testAndSave(ValueRow[] array) {
        KeysStore main = getMainStore();
        KeysStore delta = getDeltaStore();
        if (main.hasKey(value -> array[value]) || delta.hasKey(value -> array[value])) {
            oldKeysNewFacts.add(Arrays.copyOf(array, array.length));
        }
    }

    public void clear() {
        super.clear();
        this.oldKeysNewFacts.clear();
    }

    @Override
    public EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators() {
        return keyIterators;
    }

    public RuntimeFactTypeKeyed[] getEntryNodes() {
        return getGrouping()[0];
    }

    private abstract static class ModeIterator implements RhsKeyIterator {
        private final KeyMode mode;

        public ModeIterator(KeyMode mode) {
            this.mode = mode;
        }

        @Override
        public final KeyMode getMode() {
            return this.mode;
        }
    }

    private static class ModeIteratorDelegate extends ModeIterator {
        private final ReIterator<ValueRow[]> delegate;

        public ModeIteratorDelegate(KeyMode mode, ReIterator<ValueRow[]> delegate) {
            super(mode);
            this.delegate = delegate;
        }

        @Override
        public long reset() {
            return delegate.reset();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public void remove() {
            delegate.remove();
        }

        @Override
        public ValueRow[] next() {
            return delegate.next();
        }
    }

}
