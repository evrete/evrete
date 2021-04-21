package org.evrete.runtime;

import org.evrete.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


public class RuntimeRuleImpl extends AbstractRuntimeRule implements RuntimeRule {
    private static final boolean[] BOOLEANS = new boolean[]{true, false};
    private final AbstractRuleSession<?> runtime;
    private final RuleDescriptor descriptor;
    private final RuntimeLhs lhs;
    private final RhsGroupNode[] rhsGroupNodes;
    final private RhsFactType[] factTypeNodes;
    private final Map<String, Integer> nameMapping = new HashMap<>();
    private final RhsContext rhsContext;
    private final BetaEndNode[] endNodes;
    private long rhsCallCounter = 0;

    public RuntimeRuleImpl(RuleDescriptor rd, AbstractRuleSession<?> runtime) {
        super(runtime, rd, rd.getLhs().getFactTypes());
        this.descriptor = rd;
        this.runtime = runtime;
        //this.factSources = buildTypes(runtime, factTypes);
        this.lhs = new RuntimeLhs(this, rd.getLhs());
        RhsFactGroup[] rhsFactGroups = lhs.getFactGroups();
        this.factTypeNodes = new RhsFactType[rd.factTypes.length];

        for (RhsFactGroup group : rhsFactGroups) {
            for (FactType factType : group.types()) {
                int idx = factType.getInRuleIndex();
                assert factTypeNodes[idx] == null;
                this.factTypeNodes[idx] = new RhsFactType(runtime.getMemory(), factType, group);
                if (nameMapping.put(factType.getName(), idx) != null) {
                    throw new IllegalStateException();
                }
            }
        }


        this.rhsGroupNodes = new RhsGroupNode[rhsFactGroups.length];
        for (int i = 0; i < rhsFactGroups.length; i++) {
            RhsFactGroup g = rhsFactGroups[i];
            FactType[] types = g.types();
            if (types.length == 1) {
                this.rhsGroupNodes[i] = new RhsGroupNodeSingle(g, factTypeNodes);
            } else {
                this.rhsGroupNodes[i] = new RhsGroupNodeMulti(g, factTypeNodes);
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
        ReIterator<MemoryKey> iterator = factGroup.keyIterator;
        if (iterator.reset() == 0) return;
        boolean last = group == this.rhsGroupNodes.length - 1;

        if (last) {
            while (iterator.hasNext()) {
                factGroup.copyKeyState(iterator);
                forEachFact(0, consumer);
            }
        } else {
            while (iterator.hasNext()) {
                factGroup.copyKeyState(iterator);
                forEachKey(group + 1, consumer);
            }
        }
    }

    private void forEachFact(int type, Consumer<RhsContext> consumer) {
        boolean last = type == factTypeNodes.length - 1;
        RhsFactType entry = this.factTypeNodes[type];
        ReIterator<FactHandleVersioned> it = entry.factIterator;
        if (it.reset() == 0) return;

        if (last) {
            while (it.hasNext()) {
                FactHandleVersioned handle = it.next();
                if (entry.setCurrentFact(handle)) {
                    consumer.accept(rhsContext);
                } else {
                    it.remove();
                }
            }
        } else {
            while (it.hasNext()) {
                FactHandleVersioned handle = it.next();
                if (entry.setCurrentFact(handle)) {
                    forEachFact(type + 1, consumer);
                } else {
                    it.remove();
                }
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

    public RuleDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public AbstractRuleSession<?> getRuntime() {
        return runtime;
    }

    @Override
    public RuntimeRule addImport(RuleScope scope, String imp) {
        super.addImport(scope, imp);
        return this;
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

    static abstract class RhsGroupNode {
        final RhsFactGroup group;
        ReIterator<MemoryKey> keyIterator;

        RhsGroupNode(RhsFactGroup group) {
            this.group = group;
        }

        final void initIterator(boolean delta) {
            this.keyIterator = group.keyIterator(delta);
        }

        abstract void copyKeyState(ReIterator<MemoryKey> iterator);

    }

    static class RhsGroupNodeMulti extends RhsGroupNode {
        final RhsFactType[] myFactTypeNodes;

        RhsGroupNodeMulti(RhsFactGroup group, RhsFactType[] factTypeNodes) {
            super(group);
            FactType[] types = group.types();
            this.myFactTypeNodes = new RhsFactType[types.length];
            for (int i = 0; i < types.length; i++) {
                this.myFactTypeNodes[i] = factTypeNodes[types[i].getInRuleIndex()];
            }
        }

        void copyKeyState(ReIterator<MemoryKey> iterator) {
            for (RhsFactType t : myFactTypeNodes) {
                t.setCurrentKey(iterator.next());
            }
        }
    }

    static class RhsGroupNodeSingle extends RhsGroupNode {
        final RhsFactType factTypeNode;

        RhsGroupNodeSingle(RhsFactGroup group, RhsFactType[] factTypeNodes) {
            super(group);
            this.factTypeNode = factTypeNodes[group.types()[0].getInRuleIndex()];
        }

        void copyKeyState(ReIterator<MemoryKey> iterator) {
            this.factTypeNode.setCurrentKey(iterator.next());
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
