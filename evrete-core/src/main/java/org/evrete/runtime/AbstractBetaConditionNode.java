package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.MappedReIterator;
import org.evrete.runtime.evaluation.BetaEvaluatorGroup;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractBetaConditionNode implements BetaMemoryNode<ConditionNodeDescriptor> {
    private final BetaEvaluatorGroup expression;
    private final ConditionNodeDescriptor descriptor;
    private final BetaMemoryNode<?>[] sources;
    private final BetaConditionNode[] conditionSources;
    private final RuntimeRuleImpl rule;
    private final EnumMap<KeyMode, KeysStore> stores = new EnumMap<>(KeyMode.class);

    AbstractBetaConditionNode(RuntimeRuleImpl rule, ConditionNodeDescriptor descriptor, BetaMemoryNode<?>[] sources) {
        this.sources = sources;
        List<BetaConditionNode> conditionNodeList = new ArrayList<>(sources.length);
        for (BetaMemoryNode<?> source : sources) {
            if (source.isConditionNode()) {
                conditionNodeList.add((BetaConditionNode) source);
            }
        }
        this.conditionSources = conditionNodeList.toArray(BetaConditionNode.EMPTY_ARRAY);
        this.rule = rule;
        this.descriptor = descriptor;
        MemoryFactory memoryFactory = rule.getRuntime().getMemoryFactory();

        for (KeyMode keyMode : KeyMode.values()) {
            KeysStore store = memoryFactory.newKeyStore(descriptor.getEvalGrouping());
            stores.put(keyMode, store);
        }
        this.expression = descriptor.getExpression().copyOf();
    }

    private static void forEachConditionNode(AbstractBetaConditionNode node, Consumer<AbstractBetaConditionNode> consumer) {
        consumer.accept(node);
        for (BetaMemoryNode<?> parent : node.getSources()) {
            if (parent.isConditionNode()) {
                forEachConditionNode((AbstractBetaConditionNode) parent, consumer);
            }
        }
    }

    @Override
    public KeysStore getStore(KeyMode mode) {
        return stores.get(mode);
    }

    public AbstractKnowledgeSession<?> getRuntime() {
        return rule.getRuntime();
    }

    public BetaConditionNode[] getConditionSources() {
        return conditionSources;
    }

    public BetaMemoryNode<?>[] getSources() {
        return sources;
    }

    @Override
    public ConditionNodeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return "{" +
                "node=" + expression +
                '}';
    }

    BetaEvaluatorGroup getExpression() {
        return expression;
    }

    ReIterator<ValueRow[]> iterator(KeyMode mode) {
        return new MappedReIterator<>(getStore(mode).entries(), KeysStore.Entry::key);
    }

    void forEachConditionNode(Consumer<AbstractBetaConditionNode> consumer) {
        forEachConditionNode(this, consumer);
    }

    @Override
    public void clear() {
        stores.values().forEach(KeysStore::clear);
        for (BetaMemoryNode<?> source : getSources()) {
            source.clear();
        }
    }
}
