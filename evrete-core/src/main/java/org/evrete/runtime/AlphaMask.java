package org.evrete.runtime;

import org.evrete.api.RuntimeFact;

import java.util.Arrays;

public abstract class AlphaMask {
    private static final int[] EMPTY_INDICES = new int[0];
    private static final boolean[] EMPTY_VALUES = new boolean[0];
    static final AlphaMask NO_FIELDS_NO_CONDITIONS = new AlphaMask(-777, EMPTY_INDICES, EMPTY_VALUES) {
        @Override
        public boolean test(RuntimeFact fact) {
            throw new UnsupportedOperationException();
        }
    };

    private final int bucketIndex;
    private final int[] alphaIndices;
    private final boolean[] requiredValues;
    private final int hash;

    private AlphaMask(int bucketIndex, int[] alphaIndices, boolean[] requiredValues) {
        this.bucketIndex = bucketIndex;
        this.alphaIndices = alphaIndices;
        this.requiredValues = requiredValues;
        this.hash = hash(alphaIndices, requiredValues);
    }

    public boolean test(RuntimeFact fact) {
        boolean[] tests = fact.getAlphaTests();
        for (int i : alphaIndices) {
            if (tests[i] != requiredValues[i]) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return alphaIndices.length == 0;
    }

    static AlphaMask factory(int bucketIndex, int[] alphaIndices, boolean[] requiredValues) {
        if (alphaIndices.length == 0) {
            return new Empty(bucketIndex);
        } else {
            return new Default(bucketIndex, alphaIndices, requiredValues);
        }
    }

    boolean sameData(int[] alphaIndices, boolean[] requiredValues) {
        return sameData(this.alphaIndices, this.requiredValues, alphaIndices, requiredValues);
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
                ", indices=" + Arrays.toString(alphaIndices) +
                ", values=" + Arrays.toString(requiredValues) +
                '}';
    }

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

    static boolean sameData(AlphaMask ai1, AlphaMask ai2) {
        return sameData(ai1.alphaIndices, ai1.requiredValues, ai2.alphaIndices, ai2.requiredValues);
    }

    static int hash(int[] alphaIndices, boolean[] requiredValues) {
        int h = 0;
        for (int i : alphaIndices) {
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
        public Default(int bucketIndex, int[] alphaIndices, boolean[] requiredValues) {
            super(bucketIndex, alphaIndices, requiredValues);
        }
    }

}
