package org.evrete.runtime.evaluation;

import org.evrete.api.Evaluator;
import org.evrete.api.EvaluatorHandle;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.Evaluators;

public class AlphaEvaluator {
    private final ActiveField[] activeDescriptor;
    private final EvaluatorHandle delegate;
    private final int index;

    public AlphaEvaluator(int index, EvaluatorHandle e, ActiveField[] activeFields) {
        this.activeDescriptor = activeFields;
        this.delegate = e;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static Match search(Evaluators evaluators, AlphaEvaluator[] scope, EvaluatorHandle subject) {
        for (AlphaEvaluator evaluator : scope) {
            int cmp = evaluators.compare(evaluator.delegate, subject);// evaluator.delegate.compare(subject);
            switch (cmp) {
                case Evaluator.RELATION_EQUALS:
                    return new Match(evaluator, true);
                case Evaluator.RELATION_INVERSE:
                    return new Match(evaluator, false);
                case Evaluator.RELATION_NONE:
                    continue;
                default:
                    throw new IllegalStateException();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "AlphaEvaluator{" +
                "delegate=" + delegate +
                ", index=" + index +
                '}';
    }

    public ActiveField[] getActiveDescriptor() {
        return activeDescriptor;
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
