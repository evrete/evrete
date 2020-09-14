package org.evrete.runtime;

import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.api.ValueRow;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.Buffer;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


public class RuntimeRuleImpl extends AbstractRuntimeRule implements RuntimeRule, ActivationSubject {
    private final RuntimeFactType[] factSources;
    private final RuntimeFactTypeKeyed[] betaFactSources;
    private final SessionMemory memory;
    private final Function<FactType, Predicate<ValueRow>> deletedKeys;
    private final RuleDescriptor descriptor;

    private final RuntimeLhs lhs;
    private final Buffer ruleBuffer;
    private final Buffer memoryBuffer;

    public RuntimeRuleImpl(RuleDescriptor rd, SessionMemory memory) {
        super(memory, rd, rd.getLhs().getGroupFactTypes());
        this.descriptor = rd;
        this.memory = memory;
        FactType[] allFactTypes = descriptor.getLhs().getAllFactTypes();
        this.factSources = buildTypes(memory, allFactTypes);
        this.deletedKeys = factType -> {
            RuntimeFactTypeKeyed t = (RuntimeFactTypeKeyed) factSources[factType.getInRuleIndex()];
            return valueRow -> t.getKeyStorage().isKeyDeleted(valueRow);
        };

        List<RuntimeFactTypeKeyed> betaNodes = new ArrayList<>(factSources.length);
        for (RuntimeFactType t : factSources) {
            if (t.isBetaNode()) {
                betaNodes.add((RuntimeFactTypeKeyed) t);
            }
        }
        this.betaFactSources = betaNodes.toArray(new RuntimeFactTypeKeyed[0]);


        ///
        this.ruleBuffer = new Buffer();
        this.memoryBuffer = memory.getBuffer();
        this.lhs = RuntimeLhs.factory(this, rd.getLhs(), ruleBuffer);
    }

    private static RuntimeFactType[] buildTypes(SessionMemory runtime, FactType[] allFactTypes) {
        RuntimeFactType[] factSources = new RuntimeFactType[allFactTypes.length];
        for (FactType factType : allFactTypes) {
            RuntimeFactType iterable = RuntimeFactType.factory(factType, runtime);
            factSources[iterable.getInRuleIndex()] = iterable;
        }
        return factSources;
    }

    public final void executeRhs() {
        assert isInActiveState();
        this.lhs.forEach(rhs);
        memoryBuffer.takeAllFrom(ruleBuffer);
    }

    public void clear() {
        //TODO don't forget aggregate nodes once they're back
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.clear();
        }
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

    //TODO !!! investigate and optimize
    public boolean isDeleteDeltaAvailable() {
        boolean delta = false;
        for (RuntimeFactTypeKeyed ft : this.betaFactSources) {
            if (ft.isDeleteDeltaAvailable()) {
                delta = true;
            }
        }
        return delta;
    }

    public Function<FactType, Predicate<ValueRow>> getDeletedKeys() {
        return deletedKeys;
    }

    @Override
    public boolean isInActiveState() {
        return lhs.isInActiveState();
    }

    @Override
    public void resetState() {
        lhs.resetState();
        // Merge deltas if available
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.mergeDelta();
        }
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

    /*
    public Collection<BetaEndNode> getAllBetaEndNodes() {
        return lhs.getAllBetaEndNodes();
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return lhs.getAggregateConditionedGroups();
    }
*/

    @Override
    public String toString() {
        return "RuntimeRule{" +
                "name=" + getName() +
                '}';
    }

}
