package org.evrete.dsl;

import org.evrete.dsl.annotation.RuleSet;

import java.io.Serializable;
import java.util.Comparator;

class RuleComparator implements Comparator<RuleMethod>, Serializable {
    private static final long serialVersionUID = 227921134207025747L;
    private final RuleSet.Sort nameSort;

    RuleComparator(RuleSet.Sort nameSort) {
        this.nameSort = nameSort;
    }

    @Override
    public int compare(RuleMethod o1, RuleMethod o2) {
        int cmp = Integer.compare(o1.getSalience(), o2.getSalience());
        if (cmp == 0) {
            final int modifier;
            switch (nameSort) {
                case BY_NAME:
                    modifier = 1;
                    break;
                case BY_NAME_INVERSE:
                    modifier = -1;
                    break;
                default:
                    throw new IllegalStateException();
            }

            cmp = modifier * o1.getRuleName().compareTo(o2.getRuleName());
        }
        return cmp;
    }
}
