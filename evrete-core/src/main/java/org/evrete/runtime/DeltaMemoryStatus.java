package org.evrete.runtime;

import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.Collection;
import java.util.List;

public class DeltaMemoryStatus {
    private final Mask<MemoryAddress> deleteMask;
    private final Collection<KeyMemoryBucket> bucketsToCommit;
    private List<RuntimeRuleImpl> agenda;

    public DeltaMemoryStatus(Mask<MemoryAddress> deleteMask, Collection<KeyMemoryBucket> bucketsToCommit) {
        this.deleteMask = deleteMask;
        this.bucketsToCommit = bucketsToCommit;
    }

    Mask<MemoryAddress> getDeleteMask() {
        return deleteMask;
    }

    List<RuntimeRuleImpl> getAgenda() {
        return agenda;
    }

    void setAgenda(List<RuntimeRuleImpl> agenda) {
        this.agenda = agenda;
    }

    Mask<MemoryAddress> getInsertMask() {
        Mask<MemoryAddress> mask = Mask.addressMask();
        for(KeyMemoryBucket v : bucketsToCommit) {
            mask.set(v.address);
        }
        return mask;
    }

    void commitDeltas() {
        for(KeyMemoryBucket b : bucketsToCommit){
            b.commitBuffer();
        }
    }
}
