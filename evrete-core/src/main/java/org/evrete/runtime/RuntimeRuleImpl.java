package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


public class RuntimeRuleImpl extends AbstractRuntimeRule implements RuntimeRule, EvaluationListeners {
    private static final boolean[] BOOLEANS = new boolean[]{true, false};
    private final RuntimeFactType[] factSources;
    private final AbstractKnowledgeSession<?> runtime;
    private final RuleDescriptor descriptor;
    private final RuntimeLhs lhs;
    private long rhsCallCounter = 0;
    private final RhsGroupNode[] rhsGroupNodes;
    final private FactTypeNode[] factTypeNodes;
    private final Map<String, Integer> nameMapping = new HashMap<>();
    private final RhsContext rhsContext;

    public RuntimeRuleImpl(RuleDescriptor rd, AbstractKnowledgeSession<?> runtime) {
        super(runtime, rd, rd.getLhs().getFactTypes());
        this.descriptor = rd;
        this.runtime = runtime;
        this.factSources = buildTypes(runtime, factTypes);
        this.lhs = new RuntimeLhs(this, rd.getLhs());
        RhsFactGroup[] rhsFactGroups = lhs.getFactGroups();
        this.rhsGroupNodes = new RhsGroupNode[rhsFactGroups.length];
        for (int i = 0; i < rhsFactGroups.length; i++) {
            this.rhsGroupNodes[i] = new RhsGroupNode(rhsFactGroups[i]);
        }

        FactType[] allTypes = getAllFactTypes();
        this.factTypeNodes = new FactTypeNode[allTypes.length];
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

        this.rhsContext = new RhsContextImpl();
    }

    private static RuntimeFactType[] buildTypes(AbstractKnowledgeSession<?> runtime, FactType[] allFactTypes) {
        RuntimeFactType[] factSources = new RuntimeFactType[allFactTypes.length];
        for (FactType factType : allFactTypes) {
            RuntimeFactType iterable = RuntimeFactType.factory(factType, runtime);
            factSources[iterable.getInRuleIndex()] = iterable;
        }
        return factSources;
    }


    void mergeNodeDeltas() {
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.commitDelta();
        }
    }

    final long executeRhs() {
        this.rhsCallCounter = 0;
        this.forEachMode(0, false, rhs.andThen(ctx -> increaseCallCount()));
        return this.rhsCallCounter;
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
        ReIterator<ValueRow[]> iterator = factGroup.keyIterator;
        if (iterator.reset() == 0) return;
        boolean last = group == this.rhsGroupNodes.length - 1;

        while (iterator.hasNext()) {
            ValueRow[] valueRows = iterator.next();
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
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.clear();
        }
    }

    @Override
    public RuntimeRule set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends RuntimeFactType> T resolve(FactType type) {
        return (T) this.factSources[type.getInRuleIndex()];
    }

    public <Z extends RuntimeFactType> Z[] resolve(Class<Z> type, FactType[] types) {
        Z[] resolved = CollectionUtils.array(type, types.length);// new RuntimeFactType[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = resolve(types[i]);
        }
        return resolved;
    }

    private void copyKeyState(ValueRow[] valueRows, FactType[] types) {
        for (int i = 0; i < types.length; i++) {
            ValueRow row = valueRows[i];
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
        for (BetaEndNode node : lhs.getAllBetaEndNodes()) {
            node.forEachConditionNode(n -> n.getExpression().addListener(listener));
        }
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        for (BetaEndNode node : lhs.getAllBetaEndNodes()) {
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

    RuntimeFactType[] getAllFactTypes() {
        return this.factSources;
    }

    private static class RhsGroupNode {
        final RhsFactGroup group;
        final FactType[] types;
        ReIterator<ValueRow[]> keyIterator;
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
        private ValueRow currentKey;

        FactTypeNode(FactType type, RhsFactGroup group) {
            this.type = type;
            this.group = group;
        }

        void setCurrentKey(ValueRow currentKey) {
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
