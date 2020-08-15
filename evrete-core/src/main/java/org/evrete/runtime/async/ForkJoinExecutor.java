package org.evrete.runtime.async;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class ForkJoinExecutor {
    private final ForkJoinPool delegate;
    private static final AtomicInteger poolCounter = new AtomicInteger(0);

    public ForkJoinExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ForkJoinExecutor(int parallelism) {
        this.delegate = new ForkJoinPool(parallelism, new EvreteForkJoinWorkerThreadFactory(), null, false);
    }

    public void shutdown() {
        delegate.shutdown();
    }

    /**
     * Performs the given task, waiting for its completion.
     *
     * @param task the task
     */
    public void invoke(Completer task) {
        delegate.invoke(task);
    }

    private static final class EvreteForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private final AtomicInteger threadCounter = new AtomicInteger();
        private final int poolId;

        public EvreteForkJoinWorkerThreadFactory() {
            this.poolId = poolCounter.getAndIncrement();
        }

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new EvreteWorkerThread(pool, poolId, threadCounter);
        }
    }

    private static class EvreteWorkerThread extends ForkJoinWorkerThread {
        private static final String THREAD_NAME_FORMAT = "evrete-pool-%d-thread-%d";

        public EvreteWorkerThread(ForkJoinPool pool, int poolId, AtomicInteger threadCounter) {
            super(pool);
            setName(String.format(THREAD_NAME_FORMAT, poolId, threadCounter.incrementAndGet()));
        }
    }
}
