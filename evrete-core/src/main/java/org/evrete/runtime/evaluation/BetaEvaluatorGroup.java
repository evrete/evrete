package org.evrete.runtime.evaluation;

import org.evrete.api.ComplexityObject;
import org.evrete.api.Copyable;
import org.evrete.api.EvaluationListener;
import org.evrete.api.EvaluationListeners;
import org.evrete.runtime.BetaEvaluationValues;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;
import org.evrete.util.Bits;

import java.util.*;

public class BetaEvaluatorGroup implements ComplexityObject, Copyable<BetaEvaluatorGroup>, EvaluationListeners {
    public static final BetaEvaluatorGroup[] ZERO_ARRAY = new BetaEvaluatorGroup[0];
    private final BetaEvaluator[] evaluators;
    private final Bits factTypeMask;
    private final Set<FactType> descriptor;
    private final double complexity;

    BetaEvaluatorGroup(Collection<BetaEvaluator> collection) {
        this.factTypeMask = new Bits();
        this.evaluators = collection.toArray(BetaEvaluator.ZERO_ARRAY);
        Arrays.sort(evaluators, Comparator.comparingDouble(ComplexityObject::getComplexity));
        Set<FactType> factTypes = new HashSet<>();
        double comp = 0.0;
        for (BetaEvaluator ei : evaluators) {
            for (BetaFieldReference ref : ei.betaDescriptor()) {
                FactType t = ref.getFactType();
                factTypes.add(t);
                factTypeMask.set(t.getInRuleIndex());
            }
            comp += ei.getComplexity();
        }
        this.complexity = comp;
        this.descriptor = Collections.unmodifiableSet(factTypes);
    }

    private BetaEvaluatorGroup(BetaEvaluatorGroup other) {
        this.factTypeMask = other.factTypeMask;
        this.complexity = other.complexity;
        this.descriptor = other.descriptor;
        this.evaluators = new BetaEvaluator[other.evaluators.length];
        for (int i = 0; i < this.evaluators.length; i++) {
            this.evaluators[i] = other.evaluators[i].copyOf();
        }
    }

    @Override
    public BetaEvaluatorGroup copyOf() {
        return new BetaEvaluatorGroup(this);
    }

    @Override
    public void addListener(EvaluationListener listener) {
        for (BetaEvaluator e : evaluators) {
            e.addListener(listener);
        }
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        for (BetaEvaluator e : evaluators) {
            e.removeListener(listener);
        }
    }

    @Override
    public double getComplexity() {
        return this.complexity;
    }

    public boolean test() {
        for (BetaEvaluator evaluator : evaluators) {
            if (!evaluator.test()) {
                return false;
            }
        }
        return true;
    }

    public void setEvaluationState(BetaEvaluationValues betaEvaluationValues) {
        for (BetaEvaluator evaluator : evaluators) {
            evaluator.setEvaluationState(betaEvaluationValues);
        }
    }

    public Set<FactType> descriptor() {
        return descriptor;
    }

    public int getTotalTypesInvolved() {
        return descriptor.size();
    }

    public Bits getFactTypeMask() {
        return factTypeMask;
    }

    @Override
    public String toString() {
        return Arrays.toString(evaluators);
    }
}
