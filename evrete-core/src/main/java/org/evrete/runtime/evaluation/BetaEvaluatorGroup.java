package org.evrete.runtime.evaluation;

import org.evrete.api.ActiveField;
import org.evrete.api.ComplexityObject;
import org.evrete.api.EvaluationListener;
import org.evrete.runtime.BetaEvaluationValues;
import org.evrete.runtime.FactType;
import org.evrete.util.Bits;

import java.util.*;

public class BetaEvaluatorGroup implements BetaEvaluator {
    public static final BetaEvaluatorGroup[] ZERO_ARRAY = new BetaEvaluatorGroup[0];
    private final BetaEvaluatorSingle[] evaluators;
    private final Bits factTypeMask;
    private final Set<FactType> descriptor;
    private final double complexity;

    BetaEvaluatorGroup(Collection<BetaEvaluatorSingle> collection) {
        this.factTypeMask = new Bits();
        this.evaluators = collection.toArray(BetaEvaluatorSingle.ZERO_ARRAY);
        Arrays.sort(evaluators, Comparator.comparingDouble(ComplexityObject::getComplexity));
        Set<FactType> factTypes = new HashSet<>();
        double comp = 0.0;
        for (BetaEvaluatorSingle ei : evaluators) {
            factTypes.addAll(ei.factTypes());
            factTypeMask.or(ei.getFactTypeMask());
            comp += ei.getComplexity();
        }
        this.complexity = comp;
        this.descriptor = Collections.unmodifiableSet(factTypes);
    }

    private BetaEvaluatorGroup(BetaEvaluatorGroup other) {
        this.factTypeMask = other.factTypeMask;
        this.complexity = other.complexity;
        this.descriptor = other.descriptor;
        this.evaluators = new BetaEvaluatorSingle[other.evaluators.length];
        for (int i = 0; i < this.evaluators.length; i++) {
            this.evaluators[i] = other.evaluators[i].copyOf();
        }
    }


    @Override
    public boolean evaluatesField(ActiveField field) {
        for (BetaEvaluatorSingle e : this.evaluators) {
            if (e.evaluatesField(field)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BetaEvaluatorGroup copyOf() {
        return new BetaEvaluatorGroup(this);
    }

    @Override
    public void addListener(EvaluationListener listener) {
        for (BetaEvaluatorSingle e : evaluators) {
            e.addListener(listener);
        }
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        for (BetaEvaluatorSingle e : evaluators) {
            e.removeListener(listener);
        }
    }

    @Override
    public double getComplexity() {
        return this.complexity;
    }

    public boolean test() {
        for (BetaEvaluatorSingle evaluator : evaluators) {
            if (!evaluator.test()) {
                return false;
            }
        }
        return true;
    }

    public void setEvaluationState(BetaEvaluationValues betaEvaluationValues) {
        for (BetaEvaluatorSingle evaluator : evaluators) {
            evaluator.setEvaluationState(betaEvaluationValues);
        }
    }

    @Override
    public Set<FactType> factTypes() {
        return descriptor;
    }

    @Override
    public Bits getFactTypeMask() {
        return factTypeMask;
    }

    @Override
    public String toString() {
        return Arrays.toString(evaluators);
    }
}
