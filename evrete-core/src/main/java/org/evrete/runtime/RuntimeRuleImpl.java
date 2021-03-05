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
    final private RhsFactType[] factTypeNodes;
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

        this.factTypeNodes = new RhsFactType[rd.factTypes.length];
        for (RhsFactGroup group : rhsFactGroups) {
            for (FactType factType : group.types()) {
                int idx = factType.getInRuleIndex();
                assert factTypeNodes[idx] == null;
                this.factTypeNodes[idx] = new RhsFactType(runtime.getMemory(), factType, group);
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
        // Reset state if any
        for (RhsFactType type : this.factTypeNodes) {
            type.resetState();
        }
        this.forEachFactGroup(0, false, rhs.andThen(ctx -> increaseCallCount()));
        return this.rhsCallCounter;
    }

    public BetaEndNode[] getEndNodes() {
        return endNodes;
    }

    private void forEachFactGroup(int group, boolean hasDelta, Consumer<RhsContext> consumer) {
        boolean last = group == this.rhsGroupNodes.length - 1;
        RhsGroupNode factGroup = this.rhsGroupNodes[group];
        for (boolean b : BOOLEANS) {
            factGroup.initIterator(b);
            boolean newHasDelta = b || hasDelta;
            if (last) {
                if (newHasDelta) {
                    forEachKey(0, consumer);
                }
            } else {
                forEachFactGroup(group + 1, newHasDelta, consumer);
            }
        }
    }

    private void forEachKey(int group, Consumer<RhsContext> consumer) {
        RhsGroupNode factGroup = this.rhsGroupNodes[group];
        FactType[] types = factGroup.types;
        ReIterator<MemoryKey[]> iterator = factGroup.keyIterator;
        if (iterator.reset() == 0) return;
        boolean last = group == this.rhsGroupNodes.length - 1;

        while (iterator.hasNext()) {
            MemoryKey[] valueRows = iterator.next();
            copyKeyState(valueRows, types);
            if (last) {
                forEachFact(0, consumer);
            } else {
                forEachKey(group + 1, consumer);
            }
        }
    }

    private void forEachFact(int type, Consumer<RhsContext> consumer) {
        boolean last = type == factTypeNodes.length - 1;
        RhsFactType entry = this.factTypeNodes[type];
        ReIterator<FactHandleVersioned> it = entry.factIterator;
        if (it.reset() == 0) return;
        while (it.hasNext()) {
            FactHandleVersioned handle = it.next();
            if (entry.setCurrentFact(handle)) {
                if (last) {
                    consumer.accept(rhsContext);
                } else {
                    forEachFact(type + 1, consumer);
                }
            } else {
                it.remove();
            }
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

    RuntimeLhs getLhs() {
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
            for (RhsFactType state : factTypeNodes) {
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
            for (RhsFactType state : factTypeNodes) {
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
