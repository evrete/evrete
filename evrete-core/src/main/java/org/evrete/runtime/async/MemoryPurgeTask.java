package org.evrete.runtime.async;

import org.evrete.api.FactHandleVersioned;
import org.evrete.runtime.KeyMemoryBucket;
import org.evrete.runtime.SessionMemory;
import org.evrete.runtime.TypeMemory;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Predicate;

public class MemoryPurgeTask extends Completer {
    private static final long serialVersionUID = 7911593735991639599L;
    private final Collection<TypeMemoryTask> subtasks = new LinkedList<>();
    private final transient Mask<MemoryAddress> keyPurgeMask = Mask.addressMask();

    public MemoryPurgeTask(SessionMemory memory, Mask<MemoryAddress> factPurgeMask) {
        for (TypeMemory tm : memory) {
            Predicate<FactHandleVersioned> predicate = handle -> !tm.factExists(handle);
            for (KeyMemoryBucket bucket : tm) {
                if (factPurgeMask.get(bucket.address)) {
                    this.subtasks.add(new TypeMemoryTask(this, bucket, predicate));
                }
            }
        }
    }

    public Mask<MemoryAddress> getKeyPurgeMask() {
        return keyPurgeMask;
    }

    @Override
    protected void execute() {
        tailCall(subtasks, o -> o);
    }

    @Override
    protected void onCompletion() {
        for (TypeMemoryTask sub : subtasks) {
            if (sub.hasEmptyKeys) {
                this.keyPurgeMask.set(sub.bucket.address);
            }
        }
    }

    static class TypeMemoryTask extends Completer {
        private static final long serialVersionUID = 3628304099034857930L;
        private final transient KeyMemoryBucket bucket;
        private final transient Predicate<FactHandleVersioned> predicate;
        private boolean hasEmptyKeys = false;

        TypeMemoryTask(MemoryPurgeTask parent, KeyMemoryBucket bucket, Predicate<FactHandleVersioned> predicate) {
            super(parent);
            this.bucket = bucket;
            this.predicate = predicate;
        }

        private void setHasEmptyKeys() {
            this.hasEmptyKeys = true;
        }

        @Override
        protected void execute() {
            bucket.purgeDeleted(predicate, k -> setHasEmptyKeys());
        }
    }
}
