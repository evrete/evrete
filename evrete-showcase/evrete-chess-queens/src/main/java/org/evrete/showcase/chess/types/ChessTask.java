package org.evrete.showcase.chess.types;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

@SuppressWarnings("WeakerAccess")
public class ChessTask {
    public final boolean completed;
    public final ChessBoard board;
    private final Queue<Cell> nextCol = new LinkedList<>();

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
                break;
            }
        }
        this.completed = board.queenCount == cells.length;
    }

    public ChessTask nextTask() {
        Cell cell = nextCol.poll();
        Objects.requireNonNull(cell);
        ChessBoard copy = this.board.copy();
        copy.toggleQueen(cell.x, cell.y);
        return new ChessTask(copy);
    }

    @SuppressWarnings("unused")
    public boolean isFailed() {
        return !completed && nextCol.isEmpty();
    }
}
