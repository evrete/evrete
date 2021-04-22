package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaBucketMeta;

interface MetaChangeListener {
    void onNewActiveField(ActiveField newField);

    void onNewAlphaBucket(int type, FieldsKey key, AlphaBucketMeta meta);

}
