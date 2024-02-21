package org.evrete.api;

import org.evrete.api.annotations.NonNull;

/**
 * <p>
 * A convenience wrapper for literal expressions. It combines the expression itself and its surrounding context,
 * e.g. a {@link Rule}
 * </p>
 */
public interface LiteralExpression {
    String getSource();

    Rule getContext();

    @NonNull
    static LiteralExpression of(final String source, final Rule context) {
        return new LiteralExpression() {
            @Override
            public String getSource() {
                return source;
            }

            @Override
            public Rule getContext() {
                return context;
            }

        };
    }
}
