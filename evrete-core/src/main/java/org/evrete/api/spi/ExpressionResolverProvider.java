package org.evrete.api.spi;

import java.util.Properties;

//TODO !!! api docs
public interface ExpressionResolverProvider extends Comparable<ExpressionResolverProvider> {
    ExpressionResolver instance(Properties properties, ClassLoader classLoader);

    int order();

    @Override
    default int compareTo(ExpressionResolverProvider o) {
        return Integer.compare(this.order(), o.order());
    }
}
