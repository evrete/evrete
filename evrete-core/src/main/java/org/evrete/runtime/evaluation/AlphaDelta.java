package org.evrete.runtime.evaluation;

import org.evrete.runtime.FieldsKey;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * A wrapper class representing a change in exiting alpha conditions.
 */
public class AlphaDelta {
    private final FieldsKey key;
    private final AlphaBucketMeta newAlphaMeta;
    private final AlphaEvaluator[] newEvaluators;

    AlphaDelta(FieldsKey key, AlphaBucketMeta newAlphaMeta, Collection<AlphaEvaluator> newEvaluators) {
        this.key = key;
        this.newAlphaMeta = newAlphaMeta;
        this.newEvaluators = newEvaluators.toArray(new AlphaEvaluator[0]);
        Arrays.sort(this.newEvaluators, Comparator.comparingInt(AlphaEvaluator::getUniqueId));
    }

    public FieldsKey getKey() {
        return key;
    }

    public AlphaBucketMeta getNewAlphaMeta() {
        return newAlphaMeta;
    }

    public AlphaEvaluator[] getNewEvaluators() {
        return newEvaluators;
    }

    @Override
    public String toString() {
        return "AlphaDelta{" +
                "key=" + key +
                ", newAlphaMeta=" + newAlphaMeta +
                ", newEvaluators=" + Arrays.toString(newEvaluators) +
                '}';
    }
}
