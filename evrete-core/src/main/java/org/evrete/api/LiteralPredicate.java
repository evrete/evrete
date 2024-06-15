package org.evrete.api;

/**
 * Represents a literal condition
 */
public interface LiteralPredicate extends WorkUnit {
    String getSource();

    static LiteralPredicate of(final String source, final double complexity) {
        return new LiteralPredicate() {
            @Override
            public String getSource() {
                return source;
            }

            @Override
            public double getComplexity() {
                return complexity;
            }

            @Override
            public String toString() {
                return "{source='" + source + "'" +
                        ", complexity=" + complexity +
                        "}";
            }
        };
    }
}
