package org.evrete.api.spi;

import java.util.Properties;

//TODO !!! api docs
public interface CollectionsServiceProvider extends Comparable<CollectionsServiceProvider> {
    CollectionsService instance(Properties properties);

    int order();

    @Override
    default int compareTo(CollectionsServiceProvider o) {
        return Integer.compare(this.order(), o.order());
    }
}
