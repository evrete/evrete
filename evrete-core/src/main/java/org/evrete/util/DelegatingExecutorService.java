package org.evrete.util;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A wrapper for an {@link ExecutorService} that supports an externally supplied or internally created instance.
 * If a {@code null} {@code ExecutorService} is provided, the wrapper will create its own instance.
 * The {@code shutdown} and {@code shutdownNow} methods will only operate on the internally
 * created {@code ExecutorService}.
 */
public class DelegatingExecutorService implements ExecutorService {
    private static final Logger LOGGER = Logger.getLogger(DelegatingExecutorService.class.getName());
    private final ExecutorService delegate;
    private final boolean externallySupplied;


    /**
     * Constructs a {@code DelegatingExecutorService} with the specified {@link ExecutorService}.
     *
     * @param delegate the {@link ExecutorService} to delegate to
     */
    public DelegatingExecutorService(@NonNull ExecutorService delegate) {
        this.delegate = delegate;
        this.externallySupplied = true;
    }

    /**
     * Constructs a {@code DelegatingExecutorService} with internal {@link ExecutorService}.
     *
     * @param threads  number of thread to use
     */
    public DelegatingExecutorService(int threads) {
        this.delegate = Executors.newFixedThreadPool(threads, new CustomThreadFactory());
        this.externallySupplied = false;
    }

    @Override
    public void shutdown() {
        if (!externallySupplied) {
            // Shutdown internally created ExecutorService
            delegate.shutdown();
        }
    }

    @Override
    @NonNull
    public List<Runnable> shutdownNow() {
        if (externallySupplied) {
            // Should not shut down externally supplied ExecutorService
            LOGGER.info("ShutdownNow should be manually called on externally supplied ExecutorService.");
            return List.of();
        } else {
            // Shut down internally created ExecutorService
            return delegate.shutdownNow();
        }
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        return delegate.submit(task, result);
    }

    @Override
    @NonNull
    public Future<?> submit(@NonNull Runnable task) {
        return delegate.submit(task);
    }

    @Override
    @NonNull
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    @NonNull
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    @NonNull
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        delegate.execute(command);
    }

    static class CustomThreadFactory implements ThreadFactory {
        private static final String PREFIX = "evrete-thread";
        private final AtomicInteger threadCount = new AtomicInteger(0);
        private final ThreadGroup group;

        CustomThreadFactory() {
            group = new ThreadGroup(PREFIX + "-group");
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            String threadName = String.format("%s-%d", PREFIX, threadCount.getAndIncrement());
            return new Thread(group, r, threadName, 0);
        }
    }

}
