package org.evrete.showcase.chess.types;

import java.util.PriorityQueue;

public class WorkingQueue {
    private final PriorityQueue<ChessTask> queue = new PriorityQueue<>((o1, o2) -> Integer.compare(o2.nextColumn, o1.nextColumn));
    int max = -1;


    public WorkingQueue(ChessTask initial) {
        this.queue.add(initial);
    }

    public ChessTask take() {
        return queue.poll();
    }

    public void process(ChessTask task) {
        queue.addAll(task.buildSubtasks());
        if (queue.size() > max) {
            max = queue.size();
            System.out.println("##### " + max);
        }
    }

    public int size() {
        return queue.size();
    }
}
