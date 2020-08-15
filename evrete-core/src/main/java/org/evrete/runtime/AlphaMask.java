package org.evrete.runtime;

import org.evrete.api.RuntimeFact;

import java.util.Arrays;

public abstract class AlphaMask {
    private static final AlphaEvaluator[] EMPTY_INDICES = new AlphaEvaluator[0];
    private static final boolean[] EMPTY_VALUES = new boolean[0];
    static final AlphaMask NO_FIELDS_NO_CONDITIONS = new AlphaMask(-777, EMPTY_INDICES, EMPTY_VALUES) {
        @Override
        public boolean test(RuntimeFact fact) {
            throw new UnsupportedOperationException();
        }
    };

    private final int bucketIndex;
    private final AlphaEvaluator[] alphaEvaluators;
    private final boolean[] requiredValues;
    private final int hash;

    private AlphaMask(int bucketIndex, AlphaEvaluator[] alphaEvaluators, boolean[] requiredValues) {
        this.bucketIndex = bucketIndex;
        this.alphaEvaluators = alphaEvaluators;
        this.requiredValues = requiredValues;
        this.hash = hash(alphaEvaluators, requiredValues);
    }

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

    static AlphaMask factory(int bucketIndex, AlphaEvaluator[] alphaConditions, boolean[] requiredValues) {
        if (alphaConditions.length == 0) {
            return new Empty(bucketIndex);
        } else {
            return new Default(bucketIndex, alphaConditions, requiredValues);
        }
    }

/*
    boolean sameData(int[] alphaIndices, boolean[] requiredValues) {
        return sameData(this.alphaIndices, this.requiredValues, alphaIndices, requiredValues);
    }
*/

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
        AlphaMask that = (AlphaMask) o;
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

/*
    static boolean sameData(int[] indices1, boolean[] values1, int[] indices2, boolean[] values2) {
        if (!Arrays.equals(indices1, indices2)) return false;
        for (int i = 0; i < indices1.length; i++) {
            int alphaIdx1 = indices1[i];
            int alphaIdx2 = indices2[i];
            boolean b1 = values1[alphaIdx1];
            boolean b2 = values2[alphaIdx2];
            if (b1 != b2) return false;
        }
        return true;
    }
*/

    static boolean sameData(AlphaEvaluator[] alphaEvaluators1, boolean[] values1, AlphaEvaluator[] alphaEvaluators2, boolean[] values2) {
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

    static boolean sameData(AlphaMask ai1, AlphaMask ai2) {
        return sameData(ai1.alphaEvaluators, ai1.requiredValues, ai2.alphaEvaluators, ai2.requiredValues);
    }

    static int hash(AlphaEvaluator[] alphaIndices, boolean[] requiredValues) {
        int h = 0;
        for (AlphaEvaluator e : alphaIndices) {
            int i = e.getUniqueId();
            h += Integer.hashCode(i) + Boolean.hashCode(requiredValues[i]);
        }
        return h;
    }

    private static final class Empty extends AlphaMask {

        public Empty(int bucketIndex) {
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

    private static final class Default extends AlphaMask {
        public Default(int bucketIndex, AlphaEvaluator[] alphaEvaluators, boolean[] requiredValues) {
            super(bucketIndex, alphaEvaluators, requiredValues);
        }
    }

}