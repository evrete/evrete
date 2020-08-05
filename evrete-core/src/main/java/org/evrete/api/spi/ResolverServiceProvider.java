package org.evrete.api.spi;

import java.util.Properties;

//TODO !!! api docs
public interface ResolverServiceProvider extends Comparable<ResolverServiceProvider> {
    int order();

    ResolverService instance(Properties properties, ClassLoader classLoader);

    default ResolverService instance() {
        return instance(new Properties(), Thread.currentThread().getContextClassLoader());
    }

    @Override
    default int compareTo(ResolverServiceProvider o) {
        return Integer.compare(this.order(), o.order());
    }
}
