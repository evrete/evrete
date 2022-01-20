package org.evrete.runtime.async;

import org.evrete.api.FactHandle;
import org.evrete.api.FactStorage;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinkedDataRWD;
import org.evrete.runtime.*;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ComputeDeltaMemoryTask extends Completer {
    private static final long serialVersionUID = 7921593735990639599L;
    private final Collection<TypeMemoryDeltaTask> subtasks = new LinkedList<>();
    private final transient Mask<MemoryAddress> deleteMask = Mask.addressMask();
    private final Collection<KeyMemoryBucket> bucketsToCommit = new LinkedList<>();

    public ComputeDeltaMemoryTask(FactActionBuffer buffer, SessionMemory memory) {
        for (TypeMemory tm : memory) {
            this.subtasks.add(new TypeMemoryDeltaTask(this, tm, buffer));
        }
    }

    @Override
    protected void execute() {
        tailCall(subtasks, o -> o);
    }

    public Mask<MemoryAddress> getDeleteMask() {
        return deleteMask;
    }

    public Collection<KeyMemoryBucket> getBucketsToCommit() {
        return bucketsToCommit;
    }

    @Override
    protected void onCompletion() {
        Iterator<TypeMemoryDeltaTask> it = subtasks.iterator();
        while (it.hasNext()) {
            TypeMemoryDeltaTask sub = it.next();
            this.deleteMask.or(sub.deleteMask);
            this.bucketsToCommit.addAll(sub.bucketsToCommit);
            it.remove();
        }
    }

    private static class TypeMemoryDeltaTask extends Completer {
        private static final long serialVersionUID = 7844452448442224060L;
        private final transient TypeMemory tm;
        private final transient FactActionBuffer buffer;
        private final transient FactStorage<FactRecord> factStorage;
        private final transient Mask<MemoryAddress> deleteMask = Mask.addressMask();
        // Using a simplified LinkedList for faster GC
        private final transient LinkedDataRWD<RuntimeFact> inserts = new LinkedDataRWD<>();
        private final Collection<BucketInsertTask> bucketInsertTasks = new LinkedList<>();
        private final Collection<KeyMemoryBucket> bucketsToCommit = new LinkedList<>();

        TypeMemoryDeltaTask(Completer completer, TypeMemory tm, FactActionBuffer buffer) {
            super(completer);
            this.tm = tm;
            this.buffer = buffer;
            this.factStorage = tm.getFactStorage();
        }

        @Override
        protected void onCompletion() {

            // Using iterators because LinkedLists are slow to GC
            Iterator<BucketInsertTask> bi = bucketInsertTasks.iterator();
            while (bi.hasNext()) {
                BucketInsertTask task = bi.next();
                if (task.atLeastOneInserted) {
                    KeyMemoryBucket bucket = task.bucket;
                    this.bucketsToCommit.add(bucket);
                }
                bi.remove();
            }

            this.inserts.clear();
        }

        @Override
        protected void execute() {
            ReIterator<AtomicMemoryAction> it = buffer.actions(this.tm.getType());

            while (it.hasNext()) {
                AtomicMemoryAction a = it.next();
                FactHandle handle = a.handle;
                switch (a.action) {
                    case RETRACT:
                        FactRecord record = factStorage.getFact(handle);
                        if (record != null) {
                            deleteMask.or(record.getBucketsMask());
                        }
                        factStorage.delete(handle);
                        break;
                    case INSERT:
                        inserts.add(tm.createFactRuntime(handle, a.getDelta().getLatest()));
                        break;
                    case UPDATE:
                        FactRecordDelta delta = a.getDelta();

                        FactRecord previous = delta.getPrevious();
                        deleteMask.or(previous.getBucketsMask());

                        FactRecord latest = a.getDelta().getLatest();
                        factStorage.update(handle, latest);
                        inserts.add(tm.createFactRuntime(handle, latest));
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            if (inserts.size() > 0) {
                // Performing insert
                for (KeyMemoryBucket bucket : tm) {
                    addToPendingCount(1);
                    BucketInsertTask task = new BucketInsertTask(this, bucket, inserts);
                    this.bucketInsertTasks.add(task);
                    task.fork();
                }
                postInsert();
            }
        }

        private void postInsert() {
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
                            return;
                        } else {
                            // Fact storage is not a pass-by-reference one, so we need to update the record
                            factStorage.update(handle, fact.factRecord);
                        }
                    }
                }
            }
        }
    }


    static class BucketInsertTask extends Completer {
        private static final long serialVersionUID = -1537128295059722535L;
        private final transient KeyMemoryBucket bucket;
        private final transient Iterable<RuntimeFact> inserts;
        private boolean atLeastOneInserted;

        BucketInsertTask(TypeMemoryDeltaTask completer, KeyMemoryBucket bucket, Iterable<RuntimeFact> inserts) {
            super(completer);
            this.bucket = bucket;
            this.inserts = inserts;
        }

        @Override
        protected void execute() {
            this.atLeastOneInserted = bucket.insert(inserts);
        }
    }
}
