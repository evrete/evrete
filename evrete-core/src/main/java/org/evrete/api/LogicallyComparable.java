package org.evrete.api;

public interface LogicallyComparable {
    int RELATION_NONE = 0;
    int RELATION_EQUALS = 1;
    int RELATION_INVERSE = -1;

    /**
     * @param other predicate
     * @return positive integer (&gt;0) if predicates are logically the same,
     * negative integer (&lt;0) if predicates are logically inverted  (like a == 2 and a != 2)
     * zero (0) if there is no knowledge about the two or if they are independent
     */
    int compare(LogicallyComparable other);
}
