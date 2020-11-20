package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.collections.LinearHashMap;
import org.evrete.runtime.*;
import org.evrete.runtime.async.RuleHotDeploymentTask;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SessionMemory extends AbstractRuntime<StatefulSession> implements WorkingMemory {
    private static final Logger LOGGER = Logger.getLogger(SessionMemory.class.getName());
    //private final Buffer buffer;
    private final RuntimeRules ruleStorage;
    private final LinearHashMap<Type<?>, TypeMemory> typedMemories;

    protected SessionMemory(KnowledgeImpl parent) {
        super(parent);
        //this.buffer = new Buffer();
        this.ruleStorage = new RuntimeRules(this);
        this.typedMemories = new LinearHashMap<>(getTypeResolver().getKnownTypes().size());
        // Deploy existing rules
        for (RuleDescriptor descriptor : getRuleDescriptors()) {
            deployRule(descriptor, false);
        }
    }


    public ReIterator<TypeMemory> typeMemories() {
        return typedMemories.valueIterator();
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

    public RuntimeRules getRuleStorage() {
        return ruleStorage;
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
    public void insert(Object fact) {
        memoryAction(Action.INSERT, fact);
    }

    @Override
    public void insert(String factType, Object fact) {
        Type<?> t = getTypeResolver().getType(factType);
        if (t == null) {
            LOGGER.warning("Unknown type '" + factType + "', insert skipped");
        } else {
            TypeMemory tm = get(t);
            tm.doAction(Action.INSERT, fact);
        }
    }

    /*
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
*/

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

    //TODO !!!! optimize !!!!
    public boolean hasMemoryChanges() {
        ReIterator<TypeMemory> it = typedMemories.valueIterator();
        while (it.hasNext()) {
            TypeMemory tm = it.next();
            if (tm.hasMemoryChanges()) {
                return true;
            }
        }
        return false;
    }

    //TODO !!!! optimize !!!!
    public boolean hasAction(Action action) {
        ReIterator<TypeMemory> it = typedMemories.valueIterator();
        while (it.hasNext()) {
            TypeMemory tm = it.next();
            if (tm.hasMemoryChanges(action)) {
                return true;
            }
        }
        return false;
    }

/*

    public boolean hasMemoryChanges() {
        return actionCounter.hasActions(Action.values());
    }

*/

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

/*
    @Override
    public final void delete(Collection<?> objects) {
        memoryAction(Action.RETRACT, objects);
    }
*/

/*
    @Override
    public void update(Collection<?> objects) {
        memoryAction(Action.UPDATE, objects);
    }
*/

    protected void destroy() {
        typedMemories.clear();
    }

/*
    private void memoryAction(Action action, Collection<?> objects) {
        if (objects == null || objects.isEmpty()) return;
        TypeResolver resolver = getTypeResolver();
        for (Object o : objects) {
            Type<?> type = resolver.resolve(o);
            memoryAction(action, type, o);
        }
    }
*/

/*
    private void memoryAction(Action action, Type<?> t, Object o) {
        TypeMemory tm = get(t);
        tm.doAction(action, o);
    }
*/

    private void memoryAction(Action action, Object o) {
        Type<?> t = getTypeResolver().resolve(o);
        if (t == null) {
            LOGGER.warning("Unknown object type of " + o + ", action " + action + "  skipped");
        } else {
            get(t).doAction(action, o);
        }
    }

    @Override
    public <T> void forEachMemoryObject(String type, Consumer<T> consumer) {
        Type<?> t = getTypeResolver().getType(type);
        if (t != null) {
            TypeMemory tm = typedMemories.get(t);
            tm.forEachMemoryObject(consumer);
        }
    }

    @Override
    public void forEachMemoryObject(Consumer<Object> consumer) {
        typedMemories.forEachValue(tm -> tm.forEachObjectUnchecked(consumer));
    }

    public List<RuntimeRule> getRules() {
        return ruleStorage.asList();
    }

/*
    private void processInputBuffer(Action... actions) {
        for (Action action : actions) {
            buildMemoryDeltas(action);
        }

        this.ruleStorage.updateBetaMemories();
    }
*/

/*
    protected List<RuntimeRule> processInputBuffer() {
        processInputBuffer(Action.values());
        return ruleStorage.activeRules();
    }

    protected List<RuntimeRule> agenda() {
        return ruleStorage.activeRules();
    }
*/

/*
    public Agenda getAgenda() {
        return ruleStorage.activeRules();
    }
*/

    protected void buildMemoryDeltas1(Action action) {
/*
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
*/
        //actionCounter.reset(action);

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
            ReIterator<Object> it = actions.get(action);
            while (it.hasNext()) {
                memoryAction(action, it.next());
            }
        }
    }

    @Override
    public void update(Object fact) {
        memoryAction(Action.UPDATE, fact);
    }

    @Override
    public void delete(Object fact) {
        memoryAction(Action.RETRACT, fact);
    }

/*
    protected void commitMemoryDeltas() {
        // TODO can be paralleled
        typedMemories.forEachValue(TypeMemory::commitMemoryDeltas);
    }
*/

}
