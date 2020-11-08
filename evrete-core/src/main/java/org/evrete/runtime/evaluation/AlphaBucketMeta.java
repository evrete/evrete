package org.evrete.runtime.evaluation;

import org.evrete.api.RuntimeFact;

import java.util.Arrays;
import java.util.function.Predicate;

public abstract class AlphaBucketMeta implements Predicate<RuntimeFact> {
    private static final AlphaEvaluator[] EMPTY_INDICES = new AlphaEvaluator[0];
    private static final boolean[] EMPTY_VALUES = new boolean[0];
    public static final AlphaBucketMeta NO_FIELDS_NO_CONDITIONS = new AlphaBucketMeta(0, EMPTY_INDICES, EMPTY_VALUES) {
        @Override
        public boolean test(RuntimeFact fact) {
            return true;
        }
    };

    private final int bucketIndex;
    private final AlphaEvaluator[] alphaEvaluators;
    private final boolean[] requiredValues;
    private final int hash;

    private AlphaBucketMeta(int bucketIndex, AlphaEvaluator[] alphaEvaluators, boolean[] requiredValues) {
        this.bucketIndex = bucketIndex;
        this.alphaEvaluators = alphaEvaluators;
        this.requiredValues = requiredValues;
        this.hash = hash(alphaEvaluators, requiredValues);
    }

    static AlphaBucketMeta factory(int bucketIndex, AlphaEvaluator[] alphaConditions, boolean[] requiredValues) {
        if (alphaConditions.length == 0) {
            return new Empty(bucketIndex);
        } else {
            return new Default(bucketIndex, alphaConditions, requiredValues);
        }
    }

    private static boolean sameData(AlphaEvaluator[] alphaEvaluators1, boolean[] values1, AlphaEvaluator[] alphaEvaluators2, boolean[] values2) {
        if (!Arrays.equals(alphaEvaluators1, alphaEvaluators2)) return false;
        for (int i = 0; i < alphaEvaluators1.length; i++) {
            int alphaIdx1 = alphaEvaluators1[i].getUniqueId();
            int alphaIdx2 = alphaEvaluators2[i].getUniqueId();
            boolean b1 = values1[alphaIdx1];
            boolean b2 = values2[alphaIdx2];
            if (b1 != b2) return false;
        }
        return true;
    }

    private static boolean sameData(AlphaBucketMeta ai1, AlphaBucketMeta ai2) {
        return sameData(ai1.alphaEvaluators, ai1.requiredValues, ai2.alphaEvaluators, ai2.requiredValues);
    }

    private static int hash(AlphaEvaluator[] alphaIndices, boolean[] requiredValues) {
        int h = 0;
        for (AlphaEvaluator e : alphaIndices) {
            int i = e.getUniqueId();
            h += Integer.hashCode(i) + Boolean.hashCode(requiredValues[i]);
        }
        return h;
    }

    @Override
    public boolean test(RuntimeFact fact) {
        boolean[] tests = fact.getAlphaTests();
        for (AlphaEvaluator e : alphaEvaluators) {
            int i = e.getUniqueId();
            if (tests[i] != requiredValues[i]) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return alphaEvaluators.length == 0;
    }

    boolean sameData(AlphaEvaluator[] alphaEvaluators, boolean[] requiredValues) {
        return sameData(this.alphaEvaluators, this.requiredValues, alphaEvaluators, requiredValues);
    }

    public final int getBucketIndex() {
        return bucketIndex;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlphaBucketMeta that = (AlphaBucketMeta) o;
        return hash == that.hash && sameData(this, that);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "{bucket=" + bucketIndex +
                ", indices=" + Arrays.toString(alphaEvaluators) +
                ", values=" + Arrays.toString(requiredValues) +
                '}';
    }

    private static final class Empty extends AlphaBucketMeta {

        Empty(int bucketIndex) {
            super(bucketIndex, EMPTY_INDICES, EMPTY_VALUES);
        }

        @Override
        public final boolean isEmpty() {
            return true;
        }

        @Override
        public final boolean test(RuntimeFact fact) {
            return true;
        }
    }

    private static final class Default extends AlphaBucketMeta {
        Default(int bucketIndex, AlphaEvaluator[] alphaEvaluators, boolean[] requiredValues) {
            super(bucketIndex, alphaEvaluators, requiredValues);
        }
    }

}
