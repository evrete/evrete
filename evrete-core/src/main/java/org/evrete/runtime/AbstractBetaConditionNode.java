package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.MemoryKeyCollection;
import org.evrete.api.ReIterator;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBetaConditionNode implements BetaMemoryNode {
    private final ConditionNodeDescriptor descriptor;
    private final BetaMemoryNode[] sources;
    private final BetaConditionNode[] conditionSources;
    private final RuntimeRuleImpl rule;
    private final MemoryKeyCollection[] stores = new MemoryKeyCollection[KeyMode.values().length];

    private boolean mergeToMain = true;

    AbstractBetaConditionNode(RuntimeRuleImpl rule, ConditionNodeDescriptor descriptor, BetaMemoryNode[] sources) {
        this.sources = sources;
        List<BetaConditionNode> conditionNodeList = new ArrayList<>(sources.length);
        for (BetaMemoryNode source : sources) {
            if (source.getDescriptor().isConditionNode()) {
                conditionNodeList.add((BetaConditionNode) source);
            }
        }
        this.conditionSources = conditionNodeList.toArray(BetaConditionNode.EMPTY_ARRAY);
        this.rule = rule;
        this.descriptor = descriptor;
        for (KeyMode keyMode : KeyMode.values()) {
            MemoryKeyCollection store = rule.getRuntime().memory.memoryFactory.newMemoryKeyCollection(descriptor.getTypes());
            stores[keyMode.ordinal()] = keyMode == KeyMode.OLD_OLD ?
                    new MemoryKeyCollectionWrapper(store, keyMode)
                    :
                    store
            ;
        }
    }


    void commitDelta1() {
        MemoryKeyCollection delta1 = getStore(KeyMode.NEW_NEW);
        MemoryKeyCollection delta2 = getStore(KeyMode.OLD_NEW);
        if (mergeToMain) {
            MemoryKeyCollection main = getStore(KeyMode.OLD_OLD);
            ReIterator<MemoryKey> it = delta1.iterator();
            while (it.hasNext()) {
                MemoryKey key = it.next();
                //key.setMetaValue(KeyMode.MAIN.ordinal());
                main.add(key);
            }
            //main.append(delta1);
        }
        delta1.clear();
        delta2.clear();
    }

    void setMergeToMain(boolean mergeToMain) {
        this.mergeToMain = mergeToMain;
    }

    MemoryKeyCollection getStore(KeyMode mode) {
        return stores[mode.ordinal()];
    }

    AbstractRuleSession<?> getRuntime() {
        return rule.getRuntime();
    }

    public BetaConditionNode[] getConditionSources() {
        return conditionSources;
    }

    BetaMemoryNode[] getSources() {
        return sources;
    }

    @Override
    public ConditionNodeDescriptor getDescriptor() {
        return descriptor;
    }


    public ReIterator<MemoryKey> iterator(KeyMode mode) {
        return getStore(mode).iterator();
    }


    @Override
    public void clear() {
        for (MemoryKeyCollection s : stores) {
            s.clear();
        }
        for (BetaMemoryNode source : getSources()) {
            source.clear();
        }
    }

    private static class MemoryKeyCollectionWrapper implements MemoryKeyCollection {
        MemoryKeyCollection delegate;
        KeyMode forcedMode;

        MemoryKeyCollectionWrapper(MemoryKeyCollection delegate, KeyMode forcedMode) {
            this.delegate = delegate;
            this.forcedMode = forcedMode;
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public void add(MemoryKey key) {
            delegate.add(key);
        }

        @Override
        public ReIterator<MemoryKey> iterator() {
            return new MemoryKeyIterator(delegate.iterator(), forcedMode);
        }
    }
}
