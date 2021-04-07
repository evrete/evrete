package org.evrete.runtime.evaluation;

import org.evrete.api.*;

public class AlphaEvaluator implements EvaluationListeners {
    private final ActiveField[] activeDescriptor;
    private final EvaluatorWrapper delegate;

    public AlphaEvaluator(int id, EvaluatorWrapper e, ActiveField[] activeFields) {
        this.activeDescriptor = activeFields;
        this.delegate = e;
    }

    public static Match search(AlphaEvaluator[] scope, EvaluatorWrapper subject) {
        for (AlphaEvaluator evaluator : scope) {
            int cmp = evaluator.delegate.compare(subject);
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

    public EvaluatorWrapper getDelegate() {
        return delegate;
    }

    public boolean test(FieldToValue values) {
        return delegate.test(i -> values.apply(activeDescriptor[i]));
    }

    @Override
    public void addListener(EvaluationListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        delegate.removeListener(listener);
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
