package org.evrete.runtime.async;

import org.evrete.api.IntToValueRow;
import org.evrete.api.ValueRow;
import org.evrete.collections.FastHashSet;
import org.evrete.runtime.structure.FactType;

import java.util.function.Function;
import java.util.function.Predicate;

public class ValueRowPredicate implements Predicate<IntToValueRow> {
    private final FactType[] types;
    private final Function<FactType, FastHashSet<ValueRow>> function;

    public ValueRowPredicate(FactType[] types, Function<FactType, FastHashSet<ValueRow>> function) {
        this.types = types;
        this.function = function;
    }

    static ValueRowPredicate[] predicates(FactType[][] grouping, Function<FactType, FastHashSet<ValueRow>> function) {
        ValueRowPredicate[] predicates = new ValueRowPredicate[grouping.length];
        for (int level = 0; level < grouping.length; level++) {
            FactType[] types = grouping[level];
            predicates[level] = new ValueRowPredicate(types, function);
        }
        return predicates;
    }

    @Override
    public boolean test(IntToValueRow intToValueRow) {
        boolean delete = false;
        FastHashSet<ValueRow> criteria;
        for (int typeId = 0; typeId < types.length; typeId++) {
            FactType type = types[typeId];
            criteria = function.apply(type);//deleteTasks.get(type);
            boolean match = criteria != null && criteria.contains(intToValueRow.apply(typeId));
            delete |= match;
        }
        return delete;
    }
}
