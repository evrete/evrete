package org.evrete.runtime;

import org.evrete.api.FactHandle;

import java.util.concurrent.CompletableFuture;

class FutureFactHandle implements FactHandle {
    private final CompletableFuture<DefaultFactHandle> future;

    FutureFactHandle(CompletableFuture<DefaultFactHandle> future) {
        this.future = future;
    }

    DefaultFactHandle get() {
        return future.join();
    }

    @Override
    public long getId() {
        return get().getId();
    }
}
