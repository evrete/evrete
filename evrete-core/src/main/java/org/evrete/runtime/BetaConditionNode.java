package org.evrete.runtime;

import org.evrete.api.IntToMemoryKey;
import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.runtime.evaluation.BetaEvaluatorGroup;

import java.util.function.Consumer;

public class BetaConditionNode extends AbstractBetaConditionNode {
    static final BetaConditionNode[] EMPTY_ARRAY = new BetaConditionNode[0];
    private final MemoryKey[] evaluationState;
    private final SourceMeta[] sourceMetas;
    private final IntToMemoryKey saveFunction;
    private final BetaEvaluatorGroup expression;


    BetaConditionNode(RuntimeRuleImpl rule, ConditionNodeDescriptor descriptor, BetaMemoryNode[] sources) {
        super(rule, descriptor, sources);
        this.evaluationState = new MemoryKey[rule.getDescriptor().factTypes.length];

        FactType[] myTypes = descriptor.getTypes();
        this.saveFunction = value -> evaluationState[myTypes[value].getInRuleIndex()];
        this.sourceMetas = new SourceMeta[sources.length];
        for (int i = 0; i < sources.length; i++) {
            sourceMetas[i] = new SourceMeta(sources[i]);
        }

        this.expression = descriptor.getExpression().copyOf();
        this.expression.setEvaluationState(rule.getRuntime().memory.memoryFactory.getValueResolver(), (factType, fieldIndex) -> evaluationState[factType.getInRuleIndex()].get(fieldIndex));
    }

    @Override
    public void commitDelta() {
        throw new UnsupportedOperationException();
    }

    private static void forEachConditionNode(BetaConditionNode node, Consumer<BetaConditionNode> consumer) {
        consumer.accept(node);
        for (BetaMemoryNode parent : node.getSources()) {
            if (parent.getDescriptor().isConditionNode()) {
                forEachConditionNode((BetaConditionNode) parent, consumer);
            }
        }
    }


/*
    private void debug() {
        System.out.println("Node:\t" + this + "\tsources:\t" + Arrays.toString(getSources()));
        System.out.println("\t\t\t\t\t\t\t\ttypes:\t\t" + Arrays.toString(getDescriptor().getTypes()));
        for (KeyMode keyMode : KeyMode.values()) {
            System.out.println("\t" + keyMode);
            ReIterator<MemoryKey[]> it = iterator(keyMode);
            it.reset();
            int counter = 0;
            while (it.hasNext()) {
                MemoryKey[] rows = it.next();
                System.out.println("\t\t" + counter + "\t" + Arrays.toString(rows));
                counter++;
            }
        }
        System.out.println("\n");
    }
*/

    public void computeDelta(boolean deltaOnly) {
        forEachKeyMode(0, false, false, new KeyMode[this.sourceMetas.length], deltaOnly);
        //debug();
    }

    private void forEachKeyMode(int sourceIndex, boolean hasDelta, boolean hasKnownKeys, KeyMode[] modes, boolean deltaOnly) {
        for (KeyMode mode : KeyMode.values()) {
            boolean newHasDelta = hasDelta || mode.isDeltaMode();
            boolean newHasKnownKeys = hasKnownKeys || (mode == KeyMode.KNOWN_UNKNOWN);
            modes[sourceIndex] = mode;
            if (sourceIndex == sourceMetas.length - 1) {
                if (newHasDelta || (!deltaOnly)) {
                    KeyMode destinationMode = newHasKnownKeys ? KeyMode.KNOWN_UNKNOWN : KeyMode.UNKNOWN_UNKNOWN;
                    forEachModeSelection(destinationMode, modes);
                }
            } else {
                forEachKeyMode(sourceIndex + 1, newHasDelta, newHasKnownKeys, modes, deltaOnly);
            }
        }
    }

    private void forEachModeSelection(KeyMode destinationMode, KeyMode[] sourceModes) {
        ZStoreI destination = getStore(destinationMode);
        for (int i = 0; i < sourceMetas.length; i++) {
            if (!sourceMetas[i].setIterator(sourceModes[i])) {
                return;
            }
        }
        // Evaluate current mode selection
        forEachMemoryKey(0, destination);
    }

    private void forEachMemoryKey(int sourceIndex, ZStoreI destination) {
        SourceMeta meta = this.sourceMetas[sourceIndex];
        ReIterator<MemoryKey[]> it = meta.currentIterator;
        if (it.reset() == 0) return;
        FactType[] types = meta.factTypes;
        boolean last = sourceIndex == this.sourceMetas.length - 1;

        while (it.hasNext()) {
            setState(it, types);
            if (last) {
                if (expression.test()) {
                    destination.save(saveFunction);
                }
            } else {
                forEachMemoryKey(sourceIndex + 1, destination);
            }
        }
    }

    private void setState(ReIterator<MemoryKey[]> it, FactType[] types) {
        MemoryKey[] keys = it.next();
        for (int i = 0; i < types.length; i++) {
            MemoryKey row = keys[i];
            this.evaluationState[types[i].getInRuleIndex()] = row;
        }
    }

    BetaEvaluatorGroup getExpression() {
        return expression;
    }

    void forEachConditionNode(Consumer<BetaConditionNode> consumer) {
        forEachConditionNode(this, consumer);
    }

    @Override
    public String toString() {
        return "{" +
                "node=" + expression +
                '}';
    }

    private static class SourceMeta {
        final BetaMemoryNode source;
        final FactType[] factTypes;
        ReIterator<MemoryKey[]> currentIterator;
        KeyMode currentMode;

        SourceMeta(BetaMemoryNode source) {
            this.source = source;
            this.factTypes = source.getDescriptor().getTypes();
        }

        boolean setIterator(KeyMode mode) {
            this.currentMode = mode;
            this.currentIterator = source.iterator(mode);
            return this.currentIterator.reset() > 0;
        }
    }

}
