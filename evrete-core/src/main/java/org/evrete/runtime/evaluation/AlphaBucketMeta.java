package org.evrete.runtime.evaluation;

import java.util.*;

public abstract class AlphaBucketMeta {
    private static final Set<AlphaEvaluator.Match> EMPTY_COMPONENTS = new HashSet<>();
    public final AlphaEvaluator[] alphaEvaluators;
    public final boolean[] requiredValues;
    protected final Set<AlphaEvaluator.Match> key;
    private final int bucketIndex;

    private AlphaBucketMeta(int bucketIndex, Set<AlphaEvaluator.Match> matches) {
        this.bucketIndex = bucketIndex;
        this.key = matches;

        List<AlphaEvaluator.Match> sortedMatches = new ArrayList<>(matches);
        sortedMatches.sort(Comparator.comparingDouble(o -> o.matched.getDelegate().getComplexity()));

        this.alphaEvaluators = new AlphaEvaluator[sortedMatches.size()];
        this.requiredValues = new boolean[sortedMatches.size()];

        int i = 0;
        for (AlphaEvaluator.Match match : sortedMatches) {
            this.alphaEvaluators[i] = match.matched;
            this.requiredValues[i] = match.direct;
            i++;
        }
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
                ", values=" + Arrays.toString(requiredValues) +
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
