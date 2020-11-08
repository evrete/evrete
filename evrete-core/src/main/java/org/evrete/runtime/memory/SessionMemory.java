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
    //private final Buffer buffer;
    private final RuntimeRules ruleStorage;
    private final FastHashMap<Type<?>, TypeMemory> typedMemories;
    private final ActionCounter actionCounter = new ActionCounter();

    protected SessionMemory(KnowledgeImpl parent) {
        super(parent);
        //this.buffer = new Buffer();
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

    protected void reportMemories(String name) {
        typedMemories.forEachValue(new Consumer<TypeMemory>() {
            @Override
            public void accept(TypeMemory typeMemory) {
                System.out.println(name + " : " + typeMemory);
            }
        });
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
        //buffer.clear();
        typedMemories.forEachValue(TypeMemory::clear);
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.clear();
        }
    }

    @Override
    public final void insert(Collection<?> objects) {
        memoryAction(Action.INSERT, objects);
    }

    @Override
    public void insert(String factType, Collection<?> objects) {
        if (objects == null || objects.isEmpty()) return;
        Type<?> t = getTypeResolver().getType(factType);
        if (t != null) {
            TypeMemory tm = get(t);
            for (Object o : objects) {
                memoryAction(Action.INSERT, tm, o);
            }
        }
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

    public boolean hasMemoryChanges(Action... actions) {
        return actionCounter.hasActions(actions);
    }

    public boolean hasMemoryChanges() {
        return actionCounter.hasActions(Action.values());
    }

    public boolean hasAction(Action action) {
        return actionCounter.hasActions(action);
    }

    /*
    public Buffer getBuffer() {
        return buffer;
    }
*/

    public SharedBetaFactStorage getBetaFactStorage(FactType factType) {
        Type<?> t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();

        return get(t).get(fields).get(mask);
    }

    @Override
    public final void delete(Collection<?> objects) {
        memoryAction(Action.RETRACT, objects);
    }

    @Override
    public void update(Collection<?> objects) {
        memoryAction(Action.UPDATE, objects);
    }

    protected void destroy() {
        typedMemories.clear();
    }

    private void memoryAction(Action action, Collection<?> objects) {
        if (objects == null || objects.isEmpty()) return;
        TypeResolver resolver = getTypeResolver();
        for (Object o : objects) {
            Type<?> type = resolver.resolve(o);
            memoryAction(action, get(type), o);
        }
    }

    private void memoryAction(Action action, TypeMemory tm, Object o) {
        if (action == Action.UPDATE) {
            memoryAction(Action.RETRACT, tm, o);
            memoryAction(Action.INSERT, tm, o);
        } else {
            tm.doAction(action, o);
            this.actionCounter.increment(action);
        }
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

    private void processInputBuffer(Action... actions) {
        for (Action action : actions) {
            buildMemoryDeltas(action);
        }

        this.ruleStorage.updateBetaMemories();
    }

/*
    protected List<RuntimeRule> processInputBuffer() {
        processInputBuffer(Action.values());
        return ruleStorage.activeRules();
    }

    protected List<RuntimeRule> agenda() {
        return ruleStorage.activeRules();
    }
*/

    public Agenda getAgenda() {
        return ruleStorage.activeRules();
    }

    protected void buildMemoryDeltas(Action action) {
        switch (action) {
            case RETRACT:
                typedMemories.forEachValue(tm -> tm.processInputBuffer(action));
                this.ruleStorage.updateBetaMemories(action);
                break;
            case INSERT:
                typedMemories.forEachValue(tm -> tm.processInputBuffer(action));
                this.ruleStorage.updateBetaMemories(action);
                break;
            case UPDATE:
                throw new IllegalStateException();
        }
        actionCounter.reset(action);

/*
        if(actionCounter.hasAction(action)) {
            System.out.println("%%%%%% 1 " + action);
            typedMemories.forEachValue(tm -> tm.processInputBuffer(action));
            actionCounter.reset(action);
        }
        if(updateRules) {
            this.ruleStorage.updateBetaMemories();
        }
*/
    }

    public TypeMemory get(Type<?> t) {
        TypeMemory m = typedMemories.get(t);
        if (m == null) {
            throw new IllegalArgumentException("No type memory created for " + t);
        } else {
            return m;
        }
    }

    public void appendToBuffer(ActionQueue<Object> actions) {
        for (Action action : Action.values()) {
            Collection<Object> objects = actions.get(action);
            memoryAction(action, objects);
        }
    }


    protected void commitMemoryDeltas() {
        // TODO can be paralleled
        typedMemories.forEachValue(TypeMemory::commitMemoryDeltas);
    }

}
