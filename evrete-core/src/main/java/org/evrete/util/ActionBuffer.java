package org.evrete.util;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActionBuffer {

    // A thread-safe map to keep track of running tasks
    private final Map<CompletableFuture<?>, Boolean> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Method to submit a new task
    public void submitTask(Runnable task) {
        CompletableFuture<Void> futureTask = CompletableFuture.runAsync(task, executor);
        tasks.put(futureTask, Boolean.TRUE);

        // Remove from the map when the task completes
        futureTask.thenRun(() -> tasks.remove(futureTask));
    }

    // Method to return a CompletableFuture<Void> that completes when all internal tasks are complete
    public CompletableFuture<Void> allTasksComplete() {
        CompletableFuture<?>[] futuresArray;
        synchronized (this) {
            if (tasks.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            futuresArray = tasks.keySet().toArray(new CompletableFuture<?>[0]);
        }
        return CompletableFuture.allOf(futuresArray);
    }

    public static void main(String[] args) {
        ActionBuffer actionBuffer = new ActionBuffer();

        // Submit some example tasks
        actionBuffer.submitTask(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
            System.out.println("Task 1 complete");
        });

        actionBuffer.submitTask(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
            System.out.println("Task 2 complete");
        });

        // Await all tasks completion
        actionBuffer.allTasksComplete().thenRun(() -> System.out.println("All tasks complete"));

    }
}
