package org.evrete.helper;

import java.util.function.Consumer;

public interface IterableOver<T> {

    void forEach(Consumer<T> consumer);
}
