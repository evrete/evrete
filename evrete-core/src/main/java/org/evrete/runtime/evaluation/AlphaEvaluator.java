package org.evrete.runtime.evaluation;

import org.evrete.api.EvaluationListener;
import org.evrete.api.EvaluationListeners;
import org.evrete.api.Evaluator;
import org.evrete.api.FieldToValue;
import org.evrete.runtime.ActiveField;

public class AlphaEvaluator implements EvaluationListeners {
    private final int uniqueId;
    private final ActiveField[] activeDescriptor;
    private final EvaluatorWrapper delegate;

    public AlphaEvaluator(int uniqueId, EvaluatorWrapper e, ActiveField[] activeFields) {
        this.uniqueId = uniqueId;
        this.activeDescriptor = activeFields;
        this.delegate = e;
    }

/*
    private AlphaEvaluator(AlphaEvaluator other) {
        this.uniqueId = other.uniqueId;
        this.activeDescriptor = other.activeDescriptor;
        this.delegate = other.delegate;
    }
*/

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

/*
    public boolean test(ValueResolver valueResolver, FieldToValueHandle values) {
        return delegate.test(new IntToValue() {
            @Override
            public Object apply(int i) {
                return valueResolver.getValue(values.apply(activeDescriptor[i]));
            }
        });
        //return delegate.test(i -> values.apply(activeDescriptor[i]));
    }
*/

    public EvaluatorWrapper getDelegate() {
        return delegate;
    }


/*
    @Override
    public AlphaEvaluator copyOf() {
        return new AlphaEvaluator(this);
    }
*/

    public boolean test(FieldToValue values) {
        return delegate.test(i -> values.apply(activeDescriptor[i]));
    }

/*
    @Override
    public int compare(LogicallyComparable other) {
        if (other instanceof AlphaEvaluator) {
            return delegate.compare(((AlphaEvaluator) other).delegate);
        } else {
            return delegate.compare(other);
        }
    }
*/

    int getUniqueId() {
        return uniqueId;
    }

    @Override
    public void addListener(EvaluationListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public String toString() {
        return "AlphaEvaluator{" +
                "id=" + uniqueId +
                ", delegate=" + delegate +
                '}';
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
