package org.evrete.runtime.async;

import org.evrete.helper.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class CompleterTest {

    @Test
    void of() {
        int processors = Runtime.getRuntime().availableProcessors();
        if (processors < 4) return;

        ForkJoinExecutor executor = new ForkJoinExecutor(processors);

        Set<String> threadNames = new HashSet<>();
        Consumer<Thread> threadConsumer = thread -> threadNames.add(thread.getName());

        Runnable r1 = new R(200L, threadConsumer);
        Runnable r2 = new R(300L, threadConsumer);
        Runnable r3 = new R(400L, threadConsumer);
        Runnable r4 = new R(500L, threadConsumer);

        Completer completer1 = Completer.of(Arrays.asList(r1, r2, r3, r4));
        completer1.invoke();
        assert threadNames.size() == 4;

        Completer completer2 = Completer.of(Arrays.asList(r1, r2, r3, r4));
        threadNames.clear();
        executor.invoke(completer2);
        assert threadNames.size() == 4;

        executor.shutdown();
    }


    private static class R implements Runnable {
        private final long wait;
        private final Consumer<Thread> threadConsumer;

        R(long wait, Consumer<Thread> threadConsumer) {
            this.wait = wait;
            this.threadConsumer = threadConsumer;
        }

        @Override
        public void run() {
            TestUtils.sleep(wait);
            threadConsumer.accept(Thread.currentThread());
        }
    }
}



