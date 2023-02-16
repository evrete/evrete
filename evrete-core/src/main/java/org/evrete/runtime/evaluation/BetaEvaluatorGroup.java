package org.evrete.runtime.evaluation;

import org.evrete.api.EvaluatorHandle;
import org.evrete.api.WorkUnit;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FactType;
import org.evrete.util.Mask;

import java.util.*;

public class BetaEvaluatorGroup implements BetaEvaluator {
    private final EvaluatorHandle[] constituents;
    private final BetaEvaluatorSingle[] evaluators;
    private final Mask<FactType> factTypeMask;
    private final Set<FactType> descriptor;
    private final double complexity;

    BetaEvaluatorGroup(Collection<BetaEvaluatorSingle> collection) {
        this.factTypeMask = Mask.factTypeMask();
        this.evaluators = collection.toArray(BetaEvaluatorSingle.ZERO_ARRAY);
        Arrays.sort(evaluators, Comparator.comparingDouble(WorkUnit::getComplexity));
        Set<FactType> factTypes = new HashSet<>();
        double comp = 0.0;
        this.constituents = new EvaluatorHandle[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            BetaEvaluatorSingle ei = evaluators[i];
            factTypes.addAll(ei.factTypes());
            factTypeMask.or(ei.getFactTypeMask());
            this.constituents[i] = ei.constituents()[0];
            comp += ei.getComplexity();
        }

        this.complexity = comp;
        this.descriptor = Collections.unmodifiableSet(factTypes);
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
    public EvaluatorHandle[] constituents() {
        return constituents;
    }

    @Override
    public double getComplexity() {
        return this.complexity;
    }

    @Override
    public Set<FactType> factTypes() {
        return descriptor;
    }

    @Override
    public Mask<FactType> getFactTypeMask() {
        return factTypeMask;
    }

    @Override
    public String toString() {
        return Arrays.toString(evaluators);
    }
}
