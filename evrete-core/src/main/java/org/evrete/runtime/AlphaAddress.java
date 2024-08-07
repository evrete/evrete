package org.evrete.runtime;

import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.util.Indexed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents an indexed unique combination of alpha conditions. While the alpha conditions are bound to the same
 * fact type, this class is globally indexed to be used in {@link Mask} matching.
 *
 */
//TODO check if it's ok w/o pre-hashing and equals/hashCode (ALL indexed types !!!!!)
public class AlphaAddress extends PreHashed implements Indexed {
    private final int index;
    private final TypeAlphaConditions typeAlphaConditions;

    public AlphaAddress(int index, TypeAlphaConditions typeAlphaConditions) {
        super(index);
        this.index = index;
        this.typeAlphaConditions = typeAlphaConditions;
    }

    private Mask<AlphaConditionHandle> getMask() {
        return typeAlphaConditions.getMask();
    }

    public ActiveType.Idx getType() {
        return typeAlphaConditions.getType();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "{" +
                "idx=" + index +
                ", alpha=" + typeAlphaConditions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlphaAddress that = (AlphaAddress) o;
        return index == that.index;
    }

    public boolean matches(Mask<AlphaConditionHandle> alphaConditionResults) {
        return alphaConditionResults.containsAll(getMask());
    }

    public static Collection<AlphaAddress> matchingLocations(Mask<AlphaConditionHandle> alphaConditionResults, Set<AlphaAddress> scope) {
        List<AlphaAddress> matching = new ArrayList<>(scope.size());
        for (AlphaAddress alphaAddress : scope) {
            if (alphaAddress.matches(alphaConditionResults)) {
                matching.add(alphaAddress);
            }
        }
        return matching;

    }
}
