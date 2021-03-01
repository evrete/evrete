package org.evrete.runtime;

import org.evrete.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


public class RuntimeRuleImpl extends AbstractRuntimeRule implements RuntimeRule, EvaluationListeners {
    private static final boolean[] BOOLEANS = new boolean[]{true, false};
    private final AbstractKnowledgeSession<?> runtime;
    private final RuleDescriptor descriptor;
    private final RuntimeLhs lhs;
    private final RhsGroupNode[] rhsGroupNodes;
    final private FactTypeNode[] factTypeNodes;
    private final Map<String, Integer> nameMapping = new HashMap<>();
    private final RhsContext rhsContext;
    private final BetaEndNode[] endNodes;
    private long rhsCallCounter = 0;

    public RuntimeRuleImpl(RuleDescriptor rd, AbstractKnowledgeSession<?> runtime) {
        super(runtime, rd, rd.getLhs().getFactTypes());
        this.descriptor = rd;
        this.runtime = runtime;
        //this.factSources = buildTypes(runtime, factTypes);
        this.lhs = new RuntimeLhs(this, rd.getLhs());
        RhsFactGroup[] rhsFactGroups = lhs.getFactGroups();
        this.rhsGroupNodes = new RhsGroupNode[rhsFactGroups.length];
        for (int i = 0; i < rhsFactGroups.length; i++) {
            this.rhsGroupNodes[i] = new RhsGroupNode(rhsFactGroups[i]);
        }

        this.factTypeNodes = new FactTypeNode[rd.factTypes.length];
        for (RhsFactGroup group : rhsFactGroups) {
            for (FactType factType : group.types()) {
                int idx = factType.getInRuleIndex();
                assert factTypeNodes[idx] == null;
                this.factTypeNodes[idx] = new FactTypeNode(factType, group);
                if (nameMapping.put(factType.getVar(), idx) != null) {
                    throw new IllegalStateException();
                }
            }
        }

        this.endNodes = lhs.getEndNodes().toArray(new BetaEndNode[0]);
        this.rhsContext = new RhsContextImpl();

    }


    void mergeNodeDeltas() {
        for (BetaEndNode endNode : lhs.getEndNodes()) {
            endNode.commitDelta();
        }
    }

    final long executeRhs() {
        this.rhsCallCounter = 0;
        this.forEachMode(0, false, rhs.andThen(ctx -> increaseCallCount()));
        return this.rhsCallCounter;
    }

    public BetaEndNode[] getEndNodes() {
        return endNodes;
    }

    private void forEachMode(int group, boolean hasDelta, Consumer<RhsContext> consumer) {
        boolean last = group == this.rhsGroupNodes.length - 1;
        RhsGroupNode factGroup = this.rhsGroupNodes[group];
        for (boolean b : BOOLEANS) {
            factGroup.initIterator(b);
            boolean newHasDelta = b || hasDelta;
            if (last) {
                if (newHasDelta) {
                    iterateKeys(0, consumer);
                }
            } else {
                forEachMode(group + 1, newHasDelta, consumer);
            }
        }
    }

    private void iterateKeys(int group, Consumer<RhsContext> consumer) {
        RhsGroupNode factGroup = this.rhsGroupNodes[group];
        FactType[] types = factGroup.types;
        ReIterator<MemoryKey[]> iterator = factGroup.keyIterator;
        if (iterator.reset() == 0) return;
        boolean last = group == this.rhsGroupNodes.length - 1;

        while (iterator.hasNext()) {
            MemoryKey[] valueRows = iterator.next();
            copyKeyState(valueRows, types);
            if (last) {
                iterateFacts(0, consumer);
            } else {
                iterateKeys(group + 1, consumer);
            }
        }
    }

    private void iterateFacts(int type, Consumer<RhsContext> consumer) {
        boolean last = type == factTypeNodes.length - 1;
        FactTypeNode entry = this.factTypeNodes[type];
        ReIterator<FactHandleVersioned> it = entry.factIterator;
        if (it.reset() == 0) return;
        while (it.hasNext()) {
            FactHandleVersioned handle = it.next();
            if (setFactState(entry, handle)) {
                if (last) {
                    consumer.accept(rhsContext);
                } else {
                    iterateFacts(type + 1, consumer);
                }
            } else {
                it.remove();
            }
        }
    }

    private boolean setFactState(FactTypeNode state, FactHandleVersioned v) {
        FactHandle handle = v.getHandle();
        FactRecord fact = runtime.getMemory().get(state.type.getType().getId()).getFact(handle);
        if (fact == null || fact.getVersion() != v.getVersion()) {
            return false;
        } else {
            state.handle = handle;
            state.value = fact.instance;
            return true;
        }

    }


    private void increaseCallCount() {
        this.rhsCallCounter++;
    }

    public void clear() {
        for (BetaEndNode endNode : lhs.getEndNodes()) {
            endNode.clear();
        }
    }

    @Override
    public RuntimeRule set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    private void copyKeyState(MemoryKey[] valueRows, FactType[] types) {
        for (int i = 0; i < types.length; i++) {
            MemoryKey row = valueRows[i];
            FactType type = types[i];
            this.factTypeNodes[type.getInRuleIndex()].setCurrentKey(row);
        }
    }

    public RuleDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public AbstractKnowledgeSession<?> getRuntime() {
        return runtime;
    }

    @Override
    public RuntimeRule addImport(String imp) {
        super.addImport(imp);
        return this;
    }

    @Override
    public void addListener(EvaluationListener listener) {
        for (BetaEndNode node : lhs.getEndNodes()) {
            node.forEachConditionNode(n -> n.getExpression().addListener(listener));
        }
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        for (BetaEndNode node : lhs.getEndNodes()) {
            node.forEachConditionNode(n -> n.getExpression().removeListener(listener));
        }
    }

    public RuntimeLhs getLhs() {
        return lhs;
    }

    @Override
    public String toString() {
        return "RuntimeRule{" +
                "name='" + getName() +
                "'}";
    }

    private static class RhsGroupNode {
        final RhsFactGroup group;
        final FactType[] types;
        ReIterator<MemoryKey[]> keyIterator;
        boolean currentDelta;

        RhsGroupNode(RhsFactGroup group) {
            this.group = group;
            this.types = group.types();
        }

        void initIterator(boolean delta) {
            this.currentDelta = delta;
            this.keyIterator = group.keyIterator(delta);
        }

        @Override
        public String toString() {
            return "{" +
                    "source=" + group.toString() +
                    ", delta=" + currentDelta +
                    '}';
        }
    }

    private static class FactTypeNode {
        final FactType type;
        final RhsFactGroup group;
        FactHandle handle;
        Object value;
        private ReIterator<FactHandleVersioned> factIterator;
        private MemoryKey currentKey;

        FactTypeNode(FactType type, RhsFactGroup group) {
            this.type = type;
            this.group = group;
        }

        void setCurrentKey(MemoryKey currentKey) {
            this.currentKey = currentKey;
            this.factIterator = group.factIterator(type, currentKey);
        }

        @Override
        public String toString() {
            return currentKey.toString();
        }
    }

    private class RhsContextImpl implements RhsContext {

        @Override
        public RhsContext insert(Object obj) {
            runtime.insert(obj);
            return this;
        }

        @Override
        //TODO check if field values have _really_ changed
        public final RhsContext update(Object obj) {
            Objects.requireNonNull(obj);
            for (FactTypeNode state : factTypeNodes) {
                if (state.value == obj) {
                    runtime.update(state.handle, state.value);
                    return this;
                }
            }
            throw new IllegalArgumentException("Fact " + obj + " not found in current RHS context");
        }

        @Override
        public final RhsContext delete(Object obj) {
            Objects.requireNonNull(obj);
            for (FactTypeNode state : factTypeNodes) {
                if (state.value == obj) {
                    //state.typeMemory.bufferDelete(state.handle);
                    runtime.delete(state.handle);
                    return this;
                }
            }
            throw new IllegalArgumentException("Fact " + obj + " not found in current RHS context");
        }

        @Override
        public RuntimeRule getRule() {
            return RuntimeRuleImpl.this;
        }

        @Override
        public Object getObject(String name) {
            Integer idx = nameMapping.get(name);
            if (idx == null) throw new IllegalArgumentException("Unknown type reference: " + name);
            return factTypeNodes[idx].value;
        }
    }
}
