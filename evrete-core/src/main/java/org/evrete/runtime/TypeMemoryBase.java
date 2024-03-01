package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.FactHandle;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FactStorage;
import org.evrete.api.Type;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.Objects;
import java.util.function.Consumer;

class TypeMemoryBase extends MemoryComponent {
    final FactStorage<FactRecord> factStorage;
    final Type<?> type;
    private final ArrayOf<KeyMemoryBucket> memoryBuckets;

    TypeMemoryBase(SessionMemory sessionMemory, int type) {
        super(sessionMemory);
        this.memoryBuckets = new ArrayOf<>(KeyMemoryBucket.class);
        Type<?> t = getType(type);
        this.type = t;
        String identityMethod = configuration.getProperty(Configuration.OBJECT_COMPARE_METHOD);
        switch (identityMethod) {
            case Configuration.IDENTITY_METHOD_EQUALS:
                this.factStorage = memoryFactory.newFactStorage(t, FactRecord.class, (o1, o2) -> Objects.equals(o1.instance, o2.instance));
                break;
            case Configuration.IDENTITY_METHOD_IDENTITY:
                this.factStorage = memoryFactory.newFactStorage(t, FactRecord.class, (o1, o2) -> o1.instance == o2.instance);
                break;
            default:
                throw new IllegalArgumentException("Invalid identity method '" + identityMethod + "' in the configuration. Expected values are '" + Configuration.IDENTITY_METHOD_EQUALS + "' or '" + Configuration.IDENTITY_METHOD_IDENTITY + "'");
        }
    }

    FactRecord getStoredRecord(FactHandle handle) {
        return factStorage.getFact(handle);
    }

    public boolean factExists(FactHandleVersioned handle) {
        FactRecord fact = factStorage.getFact(handle.getHandle());
        return fact != null && fact.getVersion() == handle.getVersion();
    }

    public FactStorage<FactRecord> getFactStorage() {
        return factStorage;
    }


    public void forEach(Consumer<? super KeyMemoryBucket> consumer) {
        memoryBuckets.forEach(consumer);
    }

    public final Type<?> getType() {
        return type;
    }

    @Override
    protected void clearLocalData() {
        this.factStorage.clear();
    }

    void destroy() {
        this.memoryBuckets.clear();
    }

    public ArrayOf<KeyMemoryBucket> getMemoryBuckets() {
        return memoryBuckets;
    }

    KeyMemoryBucket touchMemory(MemoryAddress address) {
        return getCreate(address);
    }

    private KeyMemoryBucket getCreate(MemoryAddress address) {
        return memoryBuckets
                .computeIfAbsent(
                        address.getBucketIndex(),
                        k -> KeyMemoryBucket.factory(TypeMemoryBase.this, address)
                );
    }

    KeyMemoryBucket getMemoryBucket(MemoryAddress bucket) {
        int bucketIndex = bucket.getBucketIndex();
        if (bucketIndex >= memoryBuckets.length()) {
            throw new IllegalArgumentException("No alpha bucket created for " + bucket);
        } else {
            KeyMemoryBucket storage = memoryBuckets.get(bucketIndex);
            if (storage == null) {
                throw new IllegalArgumentException("No alpha bucket created for " + bucket);
            } else {
                return storage;
            }
        }
    }

    @Override
    public String toString() {
        return "TypeMemory{" + type.getName() +
                '}';
    }
}
