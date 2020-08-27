package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.collections.FastHashSet;
import org.evrete.runtime.*;

import java.util.EnumMap;

public class BetaEndNode extends BetaConditionNode implements KeyIteratorsBundle<ValueRow[]> {
    public static final BetaEndNode[] ZERO_ARRAY = new BetaEndNode[0];
    private final FastHashSet<ValueRow[]> oldKeysNewFacts = new FastHashSet<>();
    private final EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators = new EnumMap<>(KeyMode.class);
    public BetaEndNode(RuntimeRuleImpl rule, ConditionNodeDescriptor nodeDescriptor) {
        super(rule, nodeDescriptor, create(nodeDescriptor.getSources(), rule));
        FactType[] factTypes = nodeDescriptor.getEvalGrouping()[0];
        assert factTypes.length == nodeDescriptor.getTypes().length;

        for(KeyMode mode : KeyMode.values()) {
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
                    modeIterator = new ModeIteratorDelegate(
                            mode,
                            oldKeysNewFacts.iterator()
                    );
                    break;
                default:
                    throw new UnsupportedOperationException();
            }


            this.keyIterators.put(mode, modeIterator);
        }



    }

    private static BetaMemoryNode<?>[] create(NodeDescriptor[] sources, RuntimeRuleImpl rule) {
        BetaMemoryNode<?>[] result = new BetaMemoryNode<?>[sources.length];
        for (int i = 0; i < sources.length; i++) {
            result[i] = create(rule, sources[i]);
        }
        return result;
    }

    @Override
    public void computeDelta(boolean deltaOnly) {
        super.computeDelta(deltaOnly);
        //TODO !!!! check if delta check applies here
        if(deltaOnly) {
            oldKeysNewFacts.clear();
            for(RuntimeFactTypeKeyed entryNode : getEntryNodes()) {
                long l = entryNode.getKeyStorage().deltaKnownKeys().keyIterator().reset();
                if(l > 0) throw new UnsupportedOperationException("TODO !!!");
            }
        }
    }

    @Override
    public EnumMap<KeyMode, ReIterator<ValueRow[]>> keyIterators() {
        return keyIterators;
    }

    public boolean hasDeltaSources() {
        for (RuntimeFactTypeKeyed entryNode : getEntryNodes()) {
            if (entryNode.hasDeltaKeys()) {
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
        public ValueRow[] next() {
            return delegate.next();
        }
    }

}
