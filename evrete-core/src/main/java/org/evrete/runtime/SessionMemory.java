package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SessionMemory extends MemoryComponent implements Iterable<TypeMemory> {
    private static final Logger LOGGER = Logger.getLogger(SessionMemory.class.getName());
    private final ArrayOf<TypeMemory> typedMemories;
    private final MemoryFactory memoryFactory;
    private final Configuration configuration;

    protected SessionMemory(Configuration configuration, MemoryFactory memoryFactory) {
        super(memoryFactory, configuration);
        //super(parent);
        this.configuration = configuration;
        this.typedMemories = new ArrayOf<>(new TypeMemory[]{});
        this.memoryFactory = memoryFactory;
    }

    @Override
    protected void forEachChildComponent(Consumer<MemoryComponent> consumer) {
        typedMemories.forEach(consumer);
    }

    @Override
    protected void clearLocalData() {
        //TODO override or provide a message
        throw new UnsupportedOperationException();
    }

    void forEachFactEntry(BiConsumer<FactHandle, Object> consumer) {
        typedMemories.forEach(tm -> tm.forEachValidEntry(consumer));
    }

    @Override
    public Iterator<TypeMemory> iterator() {
        return typedMemories.iterator();
    }

    void forEachMemory(Consumer<TypeMemory> consumer) {
        typedMemories.forEach(consumer);
    }

    //TODO !!! duplicate method
    ReIterator<TypeMemory> typeMemories() {
        return typedMemories.iterator();
    }


    <Z> KeysStore newKeysStore(Z[][] grouping) {
        return memoryFactory.newKeyStore(grouping);
    }

    SharedPlainFactStorage newSharedPlainStorage() {
        return memoryFactory.newPlainStorage();
    }

    SharedBetaFactStorage newSharedKeyStorage(FieldsKey fieldsKey) {
        return memoryFactory.newBetaStorage(fieldsKey);
    }

    FactStorage<FactRecord> newFactStorage(Type<?> type, BiPredicate<FactRecord, FactRecord> identityFunction) {
        return memoryFactory.newFactStorage(type, FactRecord.class, identityFunction);
    }

    void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        Type<?> t = key.getType();
        get(t).touchMemory(key, alphaMeta);
    }

    //@Override
    public FactHandle insert(Object fact) {
        throw new UnsupportedOperationException();
        //return insert(getTypeResolver().resolve(fact), fact);
    }

    //@Override
    public FactHandle insert(String type, Object fact) {
        throw new UnsupportedOperationException();
        //return insert(getTypeResolver().getType(type), fact);
    }

/*
    private FactHandle insert(Type<?> type, Object fact) {
        if(type == null) {
            LOGGER.warning("Unknown type of object " + fact + ", insert skipped.");
            return null;
        }
        TypeMemory tm = get(type);
        return tm.bufferInsert(fact);
    }
*/

    private TypeMemory get(FactHandle handle) {
        return typedMemories.getChecked(handle.getTypeId());
    }

    //@Override
/*
    public void update(FactHandle handle, Object newValue) {
        TypeMemory tm = get(handle);
        tm.bufferUpdate((FactHandle) handle, newValue);
    }

    //@Override
    public void delete(FactHandle handle) {
        TypeMemory tm = get(handle);
        tm.bufferDelete(handle);
    }
*/

/*
    boolean processBuffer() {
        boolean hasInserts = false;




        return hasInserts;
    }

    void processDeleteBuffer() {
        typedMemories.forEach(TypeMemory::processDeleteBuffer);
    }

    void processInsertBuffer() {
        typedMemories.forEach(TypeMemory::processInsertBuffer);
    }
*/

    //@Override
    protected synchronized void onNewActiveField(ActiveField newField) {
        Type<?> t = newField.getDeclaringType();
        TypeMemory tm = get(t);
        tm.onNewActiveField(newField);
    }

    //@Override
    protected void onNewAlphaBucket(AlphaDelta delta) {
        Type<?> t = delta.getKey().getType();
        TypeMemory tm = typedMemories.get(t.getId());
        if (tm == null) {
            tm = new TypeMemory(SessionMemory.this, t);
            typedMemories.set(t.getId(), tm);
        } else {
            tm.onNewAlphaBucket(delta);
        }
    }

    SharedBetaFactStorage getBetaFactStorage(FactType factType) {
        Type<?> t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();

        return get(t).get(fields).get(mask);
    }

    void destroy() {
        typedMemories.clear();
    }

    public TypeMemory get(Type<?> t) {
        TypeMemory m = typedMemories.get(t.getId());
        if (m == null) {
            m = new TypeMemory(this, t);
            typedMemories.set(t.getId(), m);
        }
        return m;
    }

    public TypeMemory get(int typeId) {
        TypeMemory m = typedMemories.get(typeId);
        if (m == null) {
            throw new IllegalStateException("Unknown type id: " + typeId);
        }
        return m;
    }


    //TODO !!!!! delete
    void debug() {
        System.out.println(typedMemories.toString());
    }
}
