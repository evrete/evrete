package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaBucketMeta;

interface MetaChangeListener {
    void onNewActiveField(TypeMemoryState newState, ActiveField newField);

    void onNewAlphaBucket(TypeMemoryState newState, FieldsKey key, AlphaBucketMeta meta);

}
