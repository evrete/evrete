package org.evrete.runtime.evaluation;

import org.evrete.api.ComplexityObject;
import org.evrete.runtime.FactType;
import org.evrete.runtime.FactTypeField;
import org.evrete.util.Bits;

import java.util.*;

public class EvaluatorGroup implements ComplexityObject {
    public static final EvaluatorGroup[] ZERO_ARRAY = new EvaluatorGroup[0];
    private final EvaluatorInternal[] evaluators;
    private final Bits typeMask = new Bits();
    private final Set<FactType> descriptor;
    private final double complexity;

    EvaluatorGroup(Collection<EvaluatorInternal> collection) {
        this.evaluators = collection.toArray(EvaluatorInternal.ZERO_ARRAY);
        Arrays.sort(evaluators, Comparator.comparingDouble(ComplexityObject::getComplexity));
        Set<FactType> factTypes = new HashSet<>();
        double comp = 0.0;
        for (EvaluatorInternal ei : evaluators) {
            for (FactTypeField ref : ei.descriptor()) {
                FactType t = ref.getFactType();
                factTypes.add(t);
                typeMask.set(t.getInRuleIndex());
            }
            comp += ei.getComplexity();
        }
        this.complexity = comp;
        this.descriptor = Collections.unmodifiableSet(factTypes);
    }

    protected EvaluatorGroup(EvaluatorGroup other) {
        this.evaluators = other.evaluators;
        this.complexity = other.complexity;
        this.descriptor = other.descriptor;
    }

    @Override
    public double getComplexity() {
        return this.complexity;
    }

    public Set<FactType> descriptor() {
        return descriptor;
    }

    public EvaluatorInternal[] getEvaluators() {
        return evaluators;
    }

    public Bits getTypeMask() {
        return typeMask;
    }

    @Override
    public String toString() {
        return "EvaluatorGroup{" +
                "evaluators=" + Arrays.toString(evaluators) +
                ", complexity=" + complexity +
                '}';
    }
}
