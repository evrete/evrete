package org.evrete.runtime.evaluation;

import org.evrete.api.EvaluatorHandle;
import org.evrete.runtime.ActiveField;

public class AlphaEvaluator {
    private final ActiveField[] descriptor;
    private final EvaluatorHandle delegate;
    private final int index;

    public AlphaEvaluator(int index, EvaluatorHandle e, ActiveField[] activeFields) {
        this.descriptor = activeFields;
        this.delegate = e;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }


    @Override
    public String toString() {
        return "AlphaEvaluator{" +
                "delegate=" + delegate +
                ", index=" + index +
                '}';
    }

    public ActiveField[] getDescriptor() {
        return descriptor;
    }

    public EvaluatorHandle getDelegate() {
        return delegate;
    }

    public static class Match {
        final AlphaEvaluator matched;
        final boolean direct;

        public Match(AlphaEvaluator matched, boolean direct) {
            this.matched = matched;
            this.direct = direct;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Match match = (Match) o;
            return direct == match.direct && matched.equals(match.matched);
        }

        @Override
        public int hashCode() {
            return matched.hashCode() + 37 * Boolean.hashCode(direct);
        }
    }

}
