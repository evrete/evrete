package org.evrete.runtime.evaluation;

import org.evrete.util.Bits;

import java.util.*;

public abstract class AlphaBucketMeta {
    private static final Set<AlphaEvaluator.Match> EMPTY_COMPONENTS = new HashSet<>();
    public final AlphaEvaluator[] alphaEvaluators;
    private final Bits expectedValues = new Bits();
    final Set<AlphaEvaluator.Match> key;
    private final int bucketIndex;

    private AlphaBucketMeta(int bucketIndex, Set<AlphaEvaluator.Match> matches) {
        this.bucketIndex = bucketIndex;
        this.key = matches;

        List<AlphaEvaluator.Match> sortedMatches = new ArrayList<>(matches);
        sortedMatches.sort(Comparator.comparingDouble(o -> o.matched.getDelegate().getComplexity()));

        this.alphaEvaluators = new AlphaEvaluator[sortedMatches.size()];

        int i = 0;
        for (AlphaEvaluator.Match match : sortedMatches) {
            this.alphaEvaluators[i++] = match.matched;
            if (match.direct) {
                this.expectedValues.set(match.matched.getIndex());
            }
        }
    }

    //TODO !!! simplify, use int[] array instead
    //TODO !!! create a separate implementation for evaluators.length == 1;
    public boolean test(Bits mask) {
        int idx;
        for (AlphaEvaluator e : alphaEvaluators) {
            idx = e.getIndex();
            if (mask.get(idx) != expectedValues.get(idx)) {
                return false;
            }
        }
        return true;
    }

    public static AlphaBucketMeta factory(int bucketIndex, Set<AlphaEvaluator.Match> matches) {
        if (matches.isEmpty()) {
            return new Empty(bucketIndex);
        } else {
            return new Default(bucketIndex, matches);
        }
    }

    public abstract boolean sameKey(Set<AlphaEvaluator.Match> other);

    public abstract boolean isEmpty();

    public final int getBucketIndex() {
        return bucketIndex;
    }

    @Override
    public String toString() {
        return "{bucket=" + bucketIndex +
                ", indices=" + Arrays.toString(alphaEvaluators) +
                ", values=" + expectedValues +
                '}';
    }

    private static final class Empty extends AlphaBucketMeta {

        Empty(int bucketIndex) {
            super(bucketIndex, EMPTY_COMPONENTS);
        }

        @Override
        public final boolean isEmpty() {
            return true;
        }

        @Override
        public boolean sameKey(Set<AlphaEvaluator.Match> other) {
            return other.isEmpty();
        }

        @Override
        public boolean test(Bits mask) {
            return true;
        }
    }

    private static final class Default extends AlphaBucketMeta {
        Default(int bucketIndex, Set<AlphaEvaluator.Match> matches) {
            super(bucketIndex, matches);
        }

        @Override
        public boolean sameKey(Set<AlphaEvaluator.Match> other) {
            return this.key.equals(other);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
