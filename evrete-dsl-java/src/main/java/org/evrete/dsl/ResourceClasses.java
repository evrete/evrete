package org.evrete.dsl;

import org.evrete.api.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

class ResourceClasses  {
    final ClassLoader classLoader;
    final Collection<Class<?>> classes;
    final Closeable closeable;

    ResourceClasses(ClassLoader classLoader, Collection<Class<?>> classes, @Nullable Closeable closeable) {
        this.classLoader = classLoader;
        this.classes = classes;
        this.closeable = closeable;
    }

    ResourceClasses(ClassLoader classLoader, Collection<Class<?>> classes) {
        this(classLoader, classes, null);
    }

    public void closeResources() throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }
}
