package org.evrete.showcase.chess.types;

import java.util.*;

public class ChessTask {
    public final boolean completed;
    public final boolean failed;
    private final ChessBoard board;
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

        if (board.queenCount == cells.length) {
            this.completed = true;
            this.failed = false;
        } else {
            this.completed = false;
            this.failed = nextCol.isEmpty();
        }
    }

    public Collection<ChessTask> buildSubtasks() {
        if (nextCol.isEmpty()) throw new IllegalStateException();

        Collection<ChessTask> subTasks = new ArrayList<>();
        for (Cell cell : nextCol) {
            ChessBoard copy = this.board.copy();
            copy.toggleQueen(cell.x, cell.y);
            subTasks.add(new ChessTask(copy));
        }

        if (subTasks.isEmpty()) {
            throw new IllegalStateException();
        }

        return subTasks;
    }

    @Override
    public String toString() {
        return "ChessTask{" +
                "completed=" + completed +
                ", failed=" + failed +
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
