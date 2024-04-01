package org.evrete.runtime;

import java.util.Collection;
import java.util.List;

class DeltaMemoryStatus {
    private final Mask<MemoryAddress> deleteMask;
    private final Collection<KeyMemoryBucket> bucketsToCommit;
    private final List<RuntimeRuleImpl> agenda;


    DeltaMemoryStatus(Mask<MemoryAddress> deleteMask, Collection<KeyMemoryBucket> bucketsToCommit, List<RuntimeRuleImpl> agenda) {
        this.deleteMask = deleteMask;
        this.bucketsToCommit = bucketsToCommit;
        this.agenda = agenda;
    }

    Mask<MemoryAddress> getDeleteMask() {
        return deleteMask;
    }

    List<RuntimeRuleImpl> getAgenda() {
        return agenda;
    }

    void commitDeltas() {
        for (KeyMemoryBucket b : bucketsToCommit) {
            b.commitBuffer();
        }
    }
}
