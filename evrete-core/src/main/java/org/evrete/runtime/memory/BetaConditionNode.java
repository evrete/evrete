package org.evrete.runtime.memory;

import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.runtime.BetaEvaluationContext;
import org.evrete.runtime.RuntimeRule;
import org.evrete.runtime.structure.ConditionNodeDescriptor;

import java.util.function.ObjIntConsumer;

public class BetaConditionNode extends AbstractBetaConditionNode {
    static final BetaConditionNode[] EMPTY_ARRAY = new BetaConditionNode[0];
    private final NodeIterationState state;
    private final boolean nonPlainSources;
    private final ReIterator<KeysStore.Entry>[] entryData;
    private final ReIterator<KeysStore.Entry>[] mainIterators;
    private final ReIterator<KeysStore.Entry>[] deltaIterators;


    @SuppressWarnings("unchecked")
    BetaConditionNode(RuntimeRule rule, ConditionNodeDescriptor descriptor, BetaMemoryNode<?>[] sources) {
        super(rule, descriptor, sources);
        DefaultStateFactory stateFactory = new DefaultStateFactory(this);
        this.state = stateFactory.newIterationState();
        this.nonPlainSources = stateFactory.hasNonPlainSources();
        this.entryData = (ReIterator<KeysStore.Entry>[]) new ReIterator[sources.length];
        this.mainIterators = (ReIterator<KeysStore.Entry>[]) new ReIterator[sources.length];
        this.deltaIterators = (ReIterator<KeysStore.Entry>[]) new ReIterator[sources.length];

        for (int source = 0; source < sources.length; source++) {
            mainIterators[source] = sources[source].getMainStore().entries();
            deltaIterators[source] = sources[source].getDeltaStore().entries();
        }
    }

    public void computeDelta(BetaEvaluationContext ctx) {
        evaluateSources(ctx, false, 0);
    }

    private void evaluateSources(BetaEvaluationContext ctx, boolean hasDelta, int sourceId) {
        ReIterator<KeysStore.Entry> iterator;

        if (sourceId == entryData.length - 1) {
            // Last iteration
            // 1. Main
            iterator = mainIterators[sourceId];
            if (hasDelta && iterator.reset() > 0) {
                this.entryData[sourceId] = iterator;
                evaluate(ctx);
            }

            // 2. Delta
            iterator = deltaIterators[sourceId];
            if (iterator.reset() > 0) {
                this.entryData[sourceId] = iterator;
                evaluate(ctx);
            }
        } else {
            //1. Main
            iterator = mainIterators[sourceId];
            if (iterator.reset() > 0) {
                this.entryData[sourceId] = iterator;
                evaluateSources(ctx, hasDelta, sourceId + 1);
            }

            //2. Delta
            iterator = deltaIterators[sourceId];
            if (iterator.reset() > 0) {
                this.entryData[sourceId] = iterator;
                evaluateSources(ctx, true, sourceId + 1);
            }
        }
    }

    private void evaluate(BetaEvaluationContext ctx) {
        KeysStore destination = getDeltaStore();
        if (nonPlainSources) {
            processInputsNonPlain(ctx, destination);
        } else {
            processInputsPlain(ctx, destination);
        }
    }

    private void processInputsPlain(BetaEvaluationContext ctx, KeysStore destination) {
        recursiveIteration(
                0,
                entryData,
                state::setEvaluationEntry,
                () -> {
                    if (state.evaluate(ctx)) {
                        state.saveTo(destination);
                    }
                });
    }

    private void processInputsNonPlain(BetaEvaluationContext ctx, KeysStore destination) {
        recursiveIteration(
                0,
                entryData,
                state::setEvaluationEntry,
                () -> {
                    if (state.evaluate(ctx)) {
                        processSecondary(state, state.buildSecondary(), destination);
                    }
                });
    }

    private static void processSecondary(NodeIterationState state, ReIterator<KeysStore.Entry>[] secondary, KeysStore destination) {
        recursiveIteration(0, secondary, state::setSecondaryEntry, () -> state.saveTo(destination));
    }

    private static void recursiveIteration(int sourceIndex, ReIterator<KeysStore.Entry>[] entryData, ObjIntConsumer<KeysStore.Entry> consumer, Runnable endRunnable) {
        ReIterator<KeysStore.Entry> it = entryData[sourceIndex];
        it.reset();
        if (sourceIndex == entryData.length - 1) {
            // Last source
            while (it.hasNext()) {
                consumer.accept(it.next(), sourceIndex);
                endRunnable.run();
            }
        } else {
            while (it.hasNext()) {
                consumer.accept(it.next(), sourceIndex);
                recursiveIteration(sourceIndex + 1, entryData, consumer, endRunnable);
            }
        }
    }
}
