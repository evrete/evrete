package org.evrete.showcase.chess.types;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class ChessTask {
    public final boolean completed;
    //public final boolean failed;
    public final ChessBoard board;
    private final Queue<Cell> nextCol = new LinkedList<>();
    int nextColumn = -1;

    public ChessTask(ChessBoard board) {
        this.board = board;
        // Analyze the board
        Cell[][] cells = board.cells;
        int size = board.cells.length;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Cell cell = cells[i][j];
                if (cell.hits == 0) {
                    nextCol.add(cell);
                }
            }
            if (nextCol.size() > 0) {
                nextColumn = nextCol.peek().x;
                break;
            }
        }
        this.completed = board.queenCount == cells.length;
    }

    public boolean hasTasks() {
        return !nextCol.isEmpty();
    }

    public ChessTask nextTask() {
        Cell cell = nextCol.poll();
        Objects.requireNonNull(cell);
        ChessBoard copy = this.board.copy();
        copy.toggleQueen(cell.x, cell.y);
        return new ChessTask(copy);
    }

/*
    public Collection<ChessTask> buildSubtasks() {
        if (nextCol.isEmpty()) throw new IllegalStateException();

        Collection<ChessTask> subTasks = new ArrayList<>();
        while (hasTasks()) {
            subTasks.add(nextTask());
        }

        if (subTasks.isEmpty()) {
            throw new IllegalStateException();
        }

        return subTasks;
    }
*/

    public boolean isFailed() {
        return !completed && nextCol.isEmpty();
    }

    @Override
    public String toString() {
        return "ChessTask{" +
                "completed=" + completed +
                ", failed=" + isFailed() +
                ", board=\n" + board +
                "\n}";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessTask task = (ChessTask) o;
        return nextColumn == task.nextColumn &&
                board.equals(task.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, nextColumn);
    }
}
