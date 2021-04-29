package org.evrete.runtime.async;

import org.evrete.api.FactHandle;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FactStorage;
import org.evrete.runtime.*;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

public class MemoryDeltaTask extends Completer {
    private static final long serialVersionUID = 7911593735990639599L;
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final Iterator<TypeMemory> typeMemories;
    private final AbstractRuleSession<?> runtime;

    public MemoryDeltaTask(AbstractRuleSession<?> runtime, Iterator<TypeMemory> typeMemories) {
        this.typeMemories = typeMemories;
        this.runtime = runtime;
    }

    @Override
    protected void execute() {
        while (typeMemories.hasNext()) {
            TypeMemory item = typeMemories.next();

            Completer c = new TypeMemoryDeltaTask(this, runtime, item);
            addToPendingCount(1);
            if (typeMemories.hasNext()) {
                c.fork();
            } else {
                // Execute the tail in current thread
                c.compute();
            }
        }
    }


    static class TypeMemoryDeltaTask extends Completer {
        private static final long serialVersionUID = 7844452444442224060L;
        private final transient TypeMemory tm;
        private final transient MemoryActionBuffer buffer;
        private final transient FactStorage<FactRecord> factStorage;
        private final AbstractRuleSession<?> runtime;

        TypeMemoryDeltaTask(Completer completer, AbstractRuleSession<?> runtime, TypeMemory tm) {
            super(completer);
            this.tm = tm;
            this.buffer = tm.getBuffer();
            this.factStorage = tm.getFactStorage();
            this.runtime = runtime;
        }

        @Override
        protected void execute() {
            processBuffer();
        }


        public void processBuffer() {
            Iterator<AtomicMemoryAction> it = buffer.actions();
            Collection<RuntimeFact> inserts = new LinkedList<>();

            while (it.hasNext()) {
                AtomicMemoryAction a = it.next();
                switch (a.action) {
                    case RETRACT:
                        FactRecord record = factStorage.getFact(a.handle);
                        if (record != null) {
                            runtime.deltaMemoryManager.onDelete(record.getBucketsMask());
                        }
                        factStorage.delete(a.handle);
                        break;
                    case INSERT:
                        inserts.add(tm.createFactRuntime(new FactHandleVersioned(a.handle), a.factRecord));
                        break;
                    case UPDATE:
                        FactRecord previous = factStorage.getFact(a.handle);
                        if (previous == null) {
                            LOGGER.warning("Unknown fact handle " + a.handle + ". Update operation skipped.");
                        } else {
                            FactRecord factRecord = a.factRecord;
                            runtime.deltaMemoryManager.onDelete(previous.getBucketsMask());

                            //TODO !!! fix this versioning mess
                            FactHandle handle = a.handle;
                            int newVersion = previous.getVersion() + 1;
                            factRecord.updateVersion(newVersion);
                            factStorage.update(handle, factRecord);
                            FactHandleVersioned versioned = new FactHandleVersioned(handle, newVersion);
                            inserts.add(tm.createFactRuntime(versioned, factRecord));
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            if (!inserts.isEmpty()) {
                // Performing insert

                tm.forEachBucket(bucket -> {
                    if (bucket.insert(inserts)) {
                        runtime.deltaMemoryManager.onInsert(bucket.address);
                    }
                });
                // After insert, each RuntimeFact's record contains an updated mask of all the memory buckets
                // where that fact has gotten into. For a remote fact storage implementation we need to update
                // its entries.

                // Checking what kind of storage we're dealing with
                for (RuntimeFact fact : inserts) {
                    Mask<MemoryAddress> mask = fact.factRecord.getBucketsMask();
                    if (mask.cardinality() > 0) {
                        // The fact has passed at least one alpha-condition and was saved in one or more buckets.
                        FactHandle handle = fact.factHandle.getHandle();
                        FactRecord record = factStorage.getFact(handle);
                        if (record != null) {
                            if (record.getBucketsMask().equals(mask)) {
                                // If the same mask is stored in the fact storage, then we have nothing to do
                                break;
                            } else {
                                // Fact storage is not a pass-by-reference one, so we need to update the record
                                factStorage.update(handle, fact.factRecord);
                            }
                        }
                    }
                }
            }

            buffer.clear();
        }

    }
}
