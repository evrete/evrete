package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.collections.FastHashMap;
import org.evrete.runtime.*;
import org.evrete.runtime.async.*;
import org.evrete.runtime.structure.FactType;
import org.evrete.runtime.structure.RuleDescriptor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class SessionMemory extends AbstractRuntime<StatefulSession, RuntimeRule> implements WorkingMemory, MemoryChangeListener {
    private final BufferSafe buffer;
    private final RuntimeRules ruleStorage;
    private final FastHashMap<Type, TypeMemory> typedMemories;

    protected SessionMemory(KnowledgeImpl parent) {
        super(parent);
        this.buffer = new BufferSafe();
        this.ruleStorage = new RuntimeRules(this);
        this.typedMemories = new FastHashMap<>(getTypeResolver().getKnownTypes().size());
        // Deploy existing rules
        for (RuleDescriptor descriptor : getRuleDescriptors()) {
            deployRule(descriptor);
        }
    }

    @Override
    public final Kind getKind() {
        return Kind.SESSION;
    }


    @Override
    public RuleDescriptor compileRule(RuleBuilder<StatefulSession> builder) {
        return buildDescriptor(builder);
    }

    @Override
    public synchronized RuntimeRule deployRule(RuleDescriptor descriptor) {
        // Initializing missing memory structures if any
        for (FactType factType : descriptor.getRootLhsDescriptor().getAllFactTypes()) {
            Type t = factType.getType();
            FieldsKey key = factType.getFields();
            AlphaMask mask = factType.getAlphaMask();
            TypeMemory tm = getCreateTypeMemory(t);
            if (key.size() > 0) {
                tm.getCreate(key).init(mask);
            } else {
                tm.init(mask);
            }
        }


        return ruleStorage.addRule(descriptor);
    }

    @Override
    public void clear() {
        buffer.clear();
        typedMemories.forEachValue(TypeMemory::clear);
    }

    @Override
    public final void insert(Collection<?> objects) {
        if (objects == null) return;
        this.buffer.add(getTypeResolver(), Action.INSERT, objects);
    }

    RuntimeRules getRuleStorage() {
        return ruleStorage;
    }

    public BufferSafe getBuffer() {
        return buffer;
    }

    public SharedBetaFactStorage getBetaFactStorage(FactType factType) {
        Type t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaMask mask = factType.getAlphaMask();

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
        //factMap.clear();
        typedMemories.clear();
    }

    @Override
    public <T> void forEachMemoryObject(String type, Consumer<T> consumer) {
        Type t = getTypeResolver().getType(type);
        TypeMemory s = typedMemories.get(t);
        if (s != null) {
            s.forEachMemoryObject(consumer);
        }
    }

    @Override
    public void forEachMemoryObject(Consumer<Object> consumer) {
        typedMemories.forEachValue(mem -> mem.forEachObjectUnchecked(consumer));
    }

    @Override
    public List<RuntimeRule> getRules() {
        return ruleStorage.asList();
    }

    protected void handleBuffer(FireContext ctx) {
        onBeforeChange();
        List<Completer> tasksQueue = new LinkedList<>();

        // 1. Deletes
        handleDeletes(tasksQueue);

        // 2. Do inserts
        handleInserts(ctx, tasksQueue);

        // 3. Execute all tasks orderly
        ForkJoinExecutor executor = getExecutor();
        for (Completer task : tasksQueue) {
            executor.invoke(task);
            //task.invokeDirect();
        }

        onAfterChange();
    }

    @Override
    public void onAfterChange() {
        buffer.clear();
        ruleStorage.onAfterChange();
        typedMemories.forEachValue(MemoryChangeListener::onAfterChange);
    }

    @Override
    public void onBeforeChange() {
        ruleStorage.onBeforeChange();
        typedMemories.forEachValue(MemoryChangeListener::onBeforeChange);
    }

    private void handleDeletes(List<Completer> tasksQueue) {
        buffer.takeAll(
                Action.RETRACT,
                (type, iterator) -> {
                    TypeMemory tm = getCreateTypeMemory(type);
                    while (iterator.hasNext()) {
                        tm.deleteSingle(iterator.next());
                    }
                    tm.commitDelete();
                }
        );

        Collection<RuntimeRule> ruleDeleteChanges = new LinkedList<>();
        for (RuntimeRule rule : ruleStorage) {
            if (rule.isDeleteDeltaAvailable()) {
                ruleDeleteChanges.add(rule);
            }
        }
        if (!ruleDeleteChanges.isEmpty()) {
            tasksQueue.add(new RuleMemoryDeleteTask(ruleDeleteChanges));
        }
    }

    private void handleInserts(FireContext ctx, List<Completer> tasksQueue) {
        buffer.takeAll(
                Action.INSERT,
                (type, iterator) -> {
                    TypeMemory tm = get(type);
                    while (iterator.hasNext()) {
                        tm.insertSingle(iterator.next());
                    }
                    tm.commitInsert();
                }
        );


        // Ordered task 1 - update end nodes
        Collection<RuntimeRule> ruleInsertChanges = new LinkedList<>();

        for (RuntimeRule rule : ruleStorage) {
            if (rule.isInsertDeltaAvailable()) {
                ruleInsertChanges.add(rule);
            }
        }

        if (!ruleInsertChanges.isEmpty()) {
            tasksQueue.add(new RuleMemoryInsertTask(ctx, ruleInsertChanges));
        }


        // Ordered task 2 - update aggregate nodes
        Collection<RuntimeAggregateLhsJoined> aggregateGroups = ruleStorage.getAggregateLhsGroups();
        if (!aggregateGroups.isEmpty()) {
            tasksQueue.add(new AggregateComputeTask(aggregateGroups));
        }

    }

    public TypeMemory getCreateTypeMemory(Type t) {
        return typedMemories.computeIfAbsent(t, k -> new TypeMemory(this, t));
    }

    public TypeMemory get(Type t) {
        TypeMemory m = typedMemories.get(t);
        if (m == null) {
            throw new IllegalArgumentException("No type memory created for " + t);
        } else {
            return m;
        }
    }

    protected boolean hasMemoryTasks() {
        return buffer.hasTasks();
    }
}
