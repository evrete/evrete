package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaEvaluator;

class MatchedAlphaEvaluator {
    final AlphaEvaluator matched;
    final boolean direct;

    MatchedAlphaEvaluator(AlphaEvaluator matched, boolean direct) {
        this.matched = matched;
        this.direct = direct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchedAlphaEvaluator match = (MatchedAlphaEvaluator) o;
        return direct == match.direct && matched.equals(match.matched);
    }

    @Override
    public int hashCode() {
        return matched.hashCode() + 37 * Boolean.hashCode(direct);
    }

}
