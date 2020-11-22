package org.evrete.runtime;

import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.runtime.memory.ActionQueue;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;


public class RuntimeRuleImpl extends AbstractRuntimeRule implements RuntimeRule {
    private final RuntimeFactType[] factSources;
    private final SessionMemory memory;
    private final RuleDescriptor descriptor;

    private final RuntimeLhs lhs;
    private final Set<Type<?>> allTypes = new HashSet<>();
    private int rhsCallCounter = 0;

    public RuntimeRuleImpl(RuleDescriptor rd, SessionMemory memory) {
        super(memory, rd, rd.getLhs().getGroupFactTypes());
        this.descriptor = rd;
        this.memory = memory;
        FactType[] allFactTypes = descriptor.getLhs().getAllFactTypes();
        this.factSources = buildTypes(memory, allFactTypes);

        for (RuntimeFactType t : factSources) {
            allTypes.add(t.getType());
        }
        this.lhs = RuntimeLhs.factory(this, rd.getLhs());
    }

    private static RuntimeFactType[] buildTypes(SessionMemory runtime, FactType[] allFactTypes) {
        RuntimeFactType[] factSources = new RuntimeFactType[allFactTypes.length];
        for (FactType factType : allFactTypes) {
            RuntimeFactType iterable = RuntimeFactType.factory(factType, runtime);
            factSources[iterable.getInRuleIndex()] = iterable;
        }
        return factSources;
    }

    public void mergeNodeDeltas() {
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.mergeDelta();
        }
    }

    public boolean dependsOn(Type<?> type) {
        return allTypes.contains(type);
    }

    public final int executeRhs(ActionQueue<Object> destinationBuffer) {
        this.rhsCallCounter = 0;
        this.lhs.setBuffer(destinationBuffer);
        this.lhs.forEach(rhs.andThen(rhsContext -> increaseCallCount()));
        return this.rhsCallCounter;
    }

    private void increaseCallCount() {
        this.rhsCallCounter++;
    }

    public void clear() {
        //TODO don't forget aggregate nodes once they're back
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

    public RuntimeFactType[] getAllFactTypes() {
        return this.factSources;
    }

    public RuleDescriptor getDescriptor() {
        return descriptor;
    }

    //TODO !! replace with getRuntime call
    public SessionMemory getMemory() {
        return memory;
    }

    @Override
    public StatefulSession getRuntime() {
        return (StatefulSession) memory;
    }

    @Override
    public RuntimeRule addImport(String imp) {
        super.addImport(imp);
        return this;
    }

    public RuntimeLhs getLhs() {
        return lhs;
    }

    @Override
    public String toString() {
        return "RuntimeRule{" +
                "name=" + getName() +
                '}';
    }

}
