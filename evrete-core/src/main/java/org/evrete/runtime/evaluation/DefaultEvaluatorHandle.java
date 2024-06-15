package org.evrete.runtime.evaluation;

import org.evrete.api.EvaluatorHandle;
import org.evrete.api.WorkUnit;
import org.evrete.util.Indexed;

public class DefaultEvaluatorHandle implements EvaluatorHandle, WorkUnit, Indexed {
    static final DefaultEvaluatorHandle[] EMPTY_ARRAY = new DefaultEvaluatorHandle[0];

    private final int id;
    private final double complexity;

    private static final long serialVersionUID = -3034551649898925600L;

    @SuppressWarnings("unused") // Serialization requirement
    private DefaultEvaluatorHandle() {
        this(-1, WorkUnit.DEFAULT_COMPLEXITY);
    }

    public DefaultEvaluatorHandle(int index, double complexity) {
        this.id = index;
        this.complexity = complexity;
    }

    @Override
    public int getIndex() {
        return id;
    }

    public static int compare(DefaultEvaluatorHandle o1, DefaultEvaluatorHandle o2) {
        int cmp1 =  Double.compare(o1.complexity, o2.complexity);
        if (cmp1 == 0) {
            return Integer.compare(o1.id, o2.id);
        } else {
            return cmp1;
        }
    }

    @Override
    public double getComplexity() {
        return complexity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultEvaluatorHandle that = (DefaultEvaluatorHandle) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "{" +
                "uid=" + id +
                ", complexity=" + complexity +
                '}';
    }
}
