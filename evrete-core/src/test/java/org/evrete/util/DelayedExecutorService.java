package org.evrete.util;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class DelayedExecutorService implements ExecutorService {
    private final ScheduledExecutorService scheduler;
    private final long delay;
    private final TimeUnit unit;
    private final ExecutorService delegate;

    public DelayedExecutorService(long delay, TimeUnit unit) {
        int poolSize = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(poolSize); // For scheduling delays
        this.delegate = Executors.newFixedThreadPool(poolSize);
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        scheduler.schedule(() -> delegate.execute(command), delay, unit);
    }

    @Override
    @NonNull
    public Future<?> submit(@NonNull Runnable task) {
        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> delegate.submit(task), delay, unit);
        return new FutureWrapper<>(scheduledFuture);
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> delegate.submit(task), delay, unit);
        return new FutureWrapper<>(scheduledFuture);
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> delegate.submit(task, result), delay, unit);
        return new FutureWrapper<>(scheduledFuture);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
        scheduler.shutdown();
    }

    @Override
    @NonNull
    public List<Runnable> shutdownNow() {
        scheduler.shutdownNow();
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown() && scheduler.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated() && scheduler.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit) && scheduler.awaitTermination(timeout, unit);
    }

    @Override
    @NonNull
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException("invokeAll not supported in DelayedExecutorService.");
    }

    @Override
    @NonNull
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException("invokeAll with timeout not supported in DelayedExecutorService.");
    }

    @Override
    @NonNull
    public <T> T invokeAny(@NonNull  Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException("invokeAny not supported in DelayedExecutorService.");
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) {
        throw new UnsupportedOperationException("invokeAny with timeout not supported in DelayedExecutorService.");
    }

    private static class FutureWrapper<T> implements Future<T> {
        private final ScheduledFuture<?> scheduledFuture;

        public FutureWrapper(ScheduledFuture<?> scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return scheduledFuture.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return scheduledFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return scheduledFuture.isDone();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get() throws InterruptedException, ExecutionException {
            return (T) ((Future<?>) scheduledFuture.get()).get();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return (T) ((Future<?>) scheduledFuture.get(timeout, unit)).get();
        }
    }
}
