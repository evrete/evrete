package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.BetaEvaluator;

import java.util.function.Consumer;

public class BetaConditionNode extends AbstractBetaConditionNode {
    static final BetaConditionNode[] EMPTY_ARRAY = new BetaConditionNode[0];
    private final MemoryKeyNode[] evaluationState;
    private final SourceMeta[] sourceMetas;
    private final BetaEvaluator expression;
    private final CachingEvaluator cachingEvaluator;

    BetaConditionNode(RuntimeRuleImpl rule, ConditionNodeDescriptor descriptor, BetaMemoryNode[] sources) {
        super(rule, descriptor, sources);
        this.expression = descriptor.getExpression().copyOf();
        ValueResolver valueResolver = rule.getRuntime().memory.memoryFactory.getValueResolver();
        FactType[] allFactTypes = rule.getFactTypes();
        this.evaluationState = new MemoryKeyNode[allFactTypes.length];

        this.cachingEvaluator = new CachingEvaluator(this.expression);

        for (FactType type : allFactTypes) {
            MemoryKeyNode keyMeta;
            if (expression.getFactTypeMask().get(type.getInRuleIndex())) {
                // This fact type is a part of condition evaluation
                keyMeta = new ConditionMemoryKeyNode(type, valueResolver, expression, cachingEvaluator);
            } else {
                // This is a pass-through type, no field value reads are required
                keyMeta = new MemoryKeyNode();
            }
            this.evaluationState[type.getInRuleIndex()] = keyMeta;
        }

        this.sourceMetas = new SourceMeta[sources.length];
        for (int i = 0; i < sources.length; i++) {
            sourceMetas[i] = new SourceMeta(sources[i]);
        }

        BetaEvaluationValues conditionValues = ref -> evaluationState[ref.getFactType().getInRuleIndex()].value(ref.getFieldIndex());
        this.expression.setEvaluationState(conditionValues);
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

    public void computeDelta(boolean deltaOnly) {
        forEachKeyMode(0, false, false, new KeyMode[this.sourceMetas.length], deltaOnly);
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
        MemoryKeyCollection destination = getStore(destinationMode);
        for (int i = 0; i < sourceMetas.length; i++) {
            if (!sourceMetas[i].setIterator(sourceModes[i])) {
                return;
            }
        }
        // Reset cached states
        for (MemoryKeyNode meta : evaluationState) {
            meta.clear();
        }
        // Evaluate current mode selection
        forEachMemoryKey(0, destination);
    }

    private void forEachMemoryKey(int sourceIndex, MemoryKeyCollection destination) {
        SourceMeta meta = this.sourceMetas[sourceIndex];
        ReIterator<MemoryKey> it = meta.currentIterator;
        if (it.reset() == 0) return;
        FactType[] types = meta.factTypes;
        boolean last = sourceIndex == this.sourceMetas.length - 1;

        while (it.hasNext()) {
            setState(it, types);
            if (last) {
                if (cachingEvaluator.test()) {
                    for (FactType type : getDescriptor().getTypes()) {
                        destination.add(evaluationState[type.getInRuleIndex()].currentKey);
                    }
                }
            } else {
                forEachMemoryKey(sourceIndex + 1, destination);
            }
        }
    }

    private void setState(ReIterator<MemoryKey> it, FactType[] types) {
        for (FactType type : types) {
            this.evaluationState[type.getInRuleIndex()].setKey(it.next());
        }
    }

    BetaEvaluator getExpression() {
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
        ReIterator<MemoryKey> currentIterator;
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

    private static class MemoryKeyNode {
        MemoryKey currentKey;

        MemoryKeyNode() {
        }

        void clear() {
            this.currentKey = null;
        }

        public void setKey(MemoryKey key) {
            this.currentKey = key;
        }

        Object value(int fieldIndex) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ConditionMemoryKeyNode extends MemoryKeyNode {
        private final FieldNode[] fieldNodes;

        ConditionMemoryKeyNode(FactType type, ValueResolver valueResolver, BetaEvaluator evaluator, CachingEvaluator cachingEvaluator) {
            ActiveField[] fields = type.getFields().getFields();
            this.fieldNodes = new FieldNode[fields.length];

            for (int i = 0; i < fields.length; i++) {
                ActiveField field = fields[i];
                FieldNode fieldNode;
                if (evaluator.evaluatesField(field)) {
                    fieldNode = new ConditionFieldNode(valueResolver, cachingEvaluator);
                } else {
                    fieldNode = new FieldNode(valueResolver);
                }
                this.fieldNodes[i] = fieldNode;
            }
        }

        void clear() {
            super.clear();
            for (FieldNode fn : fieldNodes) fn.clear();
        }

        Object value(int fieldIndex) {
            return fieldNodes[fieldIndex].value;
        }

        public void setKey(MemoryKey key) {
            if (key != this.currentKey) {
                for (int i = 0; i < fieldNodes.length; i++) {
                    fieldNodes[i].update(key.get(i));
                }
                this.currentKey = key;
            }
        }
    }

    private static class FieldNode {
        final ValueResolver valueResolver;
        protected Object value;
        ValueHandle lastHandle;

        FieldNode(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
        }

        final void clear() {
            this.lastHandle = null;
            this.value = null;
        }

        void update(ValueHandle handle) {
            if (handle != lastHandle) {
                value = valueResolver.getValue(handle);
                lastHandle = handle;
            }
        }
    }

    private static class ConditionFieldNode extends FieldNode {
        private final CachingEvaluator evaluator;

        ConditionFieldNode(ValueResolver valueResolver, CachingEvaluator evaluator) {
            super(valueResolver);
            this.evaluator = evaluator;
        }

        void update(ValueHandle handle) {
            if (handle != lastHandle) {
                value = valueResolver.getValue(handle);
                lastHandle = handle;
                evaluator.valuesChanged();
            }
        }
    }

    private static class CachingEvaluator {
        private final BetaEvaluator delegate;
        private boolean cached = false;
        private boolean lastResponse;

        CachingEvaluator(BetaEvaluator delegate) {
            this.delegate = delegate;
        }

        void valuesChanged() {
            cached = false;
        }

        boolean test() {
            if (!cached) {
                lastResponse = delegate.test();
                cached = true;
            }
            return lastResponse;
        }
    }
}
