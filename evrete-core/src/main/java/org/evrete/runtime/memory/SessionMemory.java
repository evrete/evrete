package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.collections.FastHashMap;
import org.evrete.runtime.*;
import org.evrete.runtime.async.RuleHotDeploymentTask;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public abstract class SessionMemory extends AbstractRuntime<StatefulSession> implements WorkingMemory {
    private final BufferSafe buffer;
    private final RuntimeRules ruleStorage;
    private final FastHashMap<Type<?>, TypeMemory> typedMemories;

    protected SessionMemory(KnowledgeImpl parent) {
        super(parent);
        this.buffer = new BufferSafe();
        this.ruleStorage = new RuntimeRules(this);
        this.typedMemories = new FastHashMap<>(getTypeResolver().getKnownTypes().size());
        // Deploy existing rules
        for (RuleDescriptor descriptor : getRuleDescriptors()) {
            deployRule(descriptor, false);
        }
    }


    public void reSortRules() {
        ruleStorage.sort(getRuleComparator());
    }

    @Override
    public void setRuleComparator(Comparator<Rule> ruleComparator) {
        super.setRuleComparator(ruleComparator);
        reSortRules();
    }

    @Override
    protected TypeResolver newTypeResolver() {
        return getParentContext().getTypeResolver().copyOf();
    }

    @Override
    public final Kind getKind() {
        return Kind.SESSION;
    }

    @Override
    public RuntimeRule deployRule(RuleDescriptor descriptor) {
        return deployRule(descriptor, true);
    }

    private synchronized RuntimeRuleImpl deployRule(RuleDescriptor descriptor, boolean hotDeployment) {
        for (FactType factType : descriptor.getLhs().getAllFactTypes()) {
            touchMemory(factType.getFields(), factType.getAlphaMask());
        }
        RuntimeRuleImpl rule = ruleStorage.addRule(descriptor);
        if (hotDeployment) {
            getExecutor().invoke(new RuleHotDeploymentTask(rule));
        }
        reSortRules();
        return rule;
    }

    private void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        Type<?> t = key.getType();
        typedMemories
                .computeIfAbsent(t, k -> new TypeMemory(this, t))
                .touchMemory(key, alphaMeta);
    }

    @Override
    public void clear() {
        buffer.clear();
        typedMemories.forEachValue(TypeMemory::clear);
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.clear();
        }
    }

    @Override
    public final void insert(Collection<?> objects) {
        if (objects == null) return;
        this.buffer.add(getTypeResolver(), Action.INSERT, objects);
    }

    @Override
    public void insert(String factType, Collection<?> objects) {
        if (objects == null) return;
        this.buffer.insert(getTypeResolver(), factType, objects);
    }

    @Override
    protected synchronized void onNewActiveField(ActiveField newField) {
        Type<?> t = newField.getDeclaringType();
        TypeMemory tm = typedMemories.get(t);
        if (tm == null) {
            tm = new TypeMemory(this, t);
            typedMemories.put(t, tm);
        } else {
            tm.onNewActiveField(newField);
        }
    }

    @Override
    protected void onNewAlphaBucket(AlphaDelta delta) {
        Type<?> t = delta.getKey().getType();
        TypeMemory tm = typedMemories.get(t);
        if (tm == null) {
            tm = new TypeMemory(this, t);
            typedMemories.put(t, tm);
        } else {
            tm.onNewAlphaBucket(delta);
        }
    }

    public BufferSafe getBuffer() {
        return buffer;
    }

    public SharedBetaFactStorage getBetaFactStorage(FactType factType) {
        Type<?> t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();

        return get(t).get(fields).get(mask);
    }

    @Override
    public final void delete(Collection<?> objects) {
        if (objects == null) return;
        this.buffer.add(getTypeResolver(), Action.RETRACT, objects);
    }

    @Override
    public void update(Collection<?> objects) {
        if (objects == null) return;
        this.buffer.add(getTypeResolver(), Action.UPDATE, objects);
    }

    protected void destroy() {
        buffer.clear();
        typedMemories.clear();
    }

    @Override
    public <T> void forEachMemoryObject(String type, Consumer<T> consumer) {
        Type<?> t = getTypeResolver().getType(type);
        TypeMemory tm = typedMemories.get(t);
        if (tm != null) {
            tm.forEachMemoryObject(consumer);
        }
    }

    @Override
    public void forEachMemoryObject(Consumer<Object> consumer) {
        typedMemories.forEachValue(mem -> mem.forEachObjectUnchecked(consumer));
    }

    public List<RuntimeRule> getRules() {
        return ruleStorage.asList();
    }

    protected List<RuntimeRule> processInput(Buffer buffer, Action... actions) {

        for (Action action : actions) {
            buffer.takeAll(
                    action,
                    (type, iterator) -> {
                        TypeMemory tm = get(type);
                        tm.processInput(action, iterator);
                    }
            );
        }

        // 3. Perform async updates on rules' beta nodes
        ruleStorage.updateBetaMemories(actions);

        return ruleStorage.activeRules();
    }

    public TypeMemory get(Type<?> t) {
        TypeMemory m = typedMemories.get(t);
        if (m == null) {
            throw new IllegalArgumentException("No type memory created for " + t);
        } else {
            return m;
        }
    }

    protected void commitMemoryDeltas() {
        // TODO can be paralleled
        typedMemories.forEachValue(TypeMemory::commitMemoryDeltas);
    }
}
