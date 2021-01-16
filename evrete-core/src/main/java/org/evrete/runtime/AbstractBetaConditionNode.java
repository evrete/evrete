package org.evrete.runtime;

import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.MappedReIterator;
import org.evrete.runtime.evaluation.BetaEvaluatorGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AbstractBetaConditionNode implements BetaMemoryNode<ConditionNodeDescriptor> {
    private final KeysStore mainStore;
    private final KeysStore deltaStore;
    private final BetaEvaluatorGroup expression;
    private final ConditionNodeDescriptor descriptor;
    private final BetaMemoryNode<?>[] sources;
    private final BetaConditionNode[] conditionSources;
    private final RuntimeFactTypeKeyed[][] grouping;
    private final RuntimeRuleImpl rule;
    private final ReIterator<ValueRow[]> mainIterator;
    private final ReIterator<ValueRow[]> deltaIterator;

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
        SessionMemory memory = rule.getRuntime();
        this.mainStore = memory.newKeysStore(descriptor.getEvalGrouping());
        this.deltaStore = memory.newKeysStore(descriptor.getEvalGrouping());
        this.expression = descriptor.getExpression().copyOf();

        FactType[][] descGrouping = descriptor.getEvalGrouping();
        this.grouping = new RuntimeFactTypeKeyed[descGrouping.length][];
        for (int i = 0; i < descGrouping.length; i++) {
            this.grouping[i] = rule.resolve(RuntimeFactTypeKeyed.class, descGrouping[i]);
        }

        this.mainIterator = new MappedReIterator<>(mainStore.entries(), KeysStore.Entry::key);
        this.deltaIterator = new MappedReIterator<>(deltaStore.entries(), KeysStore.Entry::key);
    }

    public BetaConditionNode[] getConditionSources() {
        return conditionSources;
    }

    @Override
    public KeysStore getDeltaStore() {
        return this.deltaStore;
    }

    public RuntimeFactTypeKeyed[][] getGrouping() {
        return grouping;
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
        return getClass().getSimpleName() + "{" +
                "condition=" + expression +
                '}';
    }

    @Override
    public KeysStore getMainStore() {
        return mainStore;
    }

    public RuntimeRuleImpl getRule() {
        return rule;
    }

    BetaEvaluatorGroup getExpression() {
        return expression;
    }

    ReIterator<ValueRow[]> mainIterator() {
        return mainIterator;
    }

    ReIterator<ValueRow[]> deltaIterator() {
        return deltaIterator;
    }

    private static void forEachConditionNode(AbstractBetaConditionNode node, Consumer<AbstractBetaConditionNode> consumer) {
        consumer.accept(node);
        for (BetaMemoryNode<?> parent : node.getSources()) {
            if (parent.isConditionNode()) {
                forEachConditionNode((AbstractBetaConditionNode) parent, consumer);
            }
        }
    }

    void forEachConditionNode(Consumer<AbstractBetaConditionNode> consumer) {
        forEachConditionNode(this, consumer);
    }


    @Override
    public void clear() {
        deltaStore.clear();
        mainStore.clear();

        for (BetaMemoryNode<?> source : getSources()) {
            source.clear();
        }
    }
}
