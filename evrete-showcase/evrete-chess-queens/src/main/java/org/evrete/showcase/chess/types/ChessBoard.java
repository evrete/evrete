package org.evrete.showcase.chess.types;

import java.util.Arrays;
import java.util.StringJoiner;

public class ChessBoard {
    public final Cell[][] cells;
    public int queenCount = 0;

    public ChessBoard(int size) {
        this.cells = new Cell[size][];
        for (int x = 0; x < size; x++) {
            this.cells[x] = new Cell[size];
            for (int y = 0; y < size; y++) {
                this.cells[x][y] = new Cell(x, y);
            }
        }
    }

    public ChessBoard toggleQueen(int x, int y) {
        Cell cell = cells[x][y];
        if (cell.queen) {
            cell.queen = false;
            this.queenCount--;
            cell.hits--;
            this._hit(x, y, -1);
        } else {
            cell.queen = true;
            this.queenCount++;
            cell.hits++;
            this._hit(x, y, +1);
        }

        return this;
    }


    ChessBoard copy() {
        int size = this.cells.length;
        ChessBoard b = new ChessBoard(size);
        b.queenCount = this.queenCount;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                b.cells[x][y] = this.cells[x][y].copy();
            }
        }

        return b;
    }


    private void _hit(int x, int y, int amount) {
        int size = this.cells.length;

        // Fill horizontal hits (right and left)
        for (int x1 = x + 1; x1 < size; x1++) {
            this.cells[x1][y].hits += amount;
        }
        for (int x1 = x - 1; x1 >= 0; x1--) {
            this.cells[x1][y].hits += amount;
        }

        // Fill vertical hits (right and left)
        for (int y1 = y + 1; y1 < size; y1++) {
            this.cells[x][y1].hits += amount;
        }
        for (int y1 = y - 1; y1 >= 0; y1--) {
            this.cells[x][y1].hits += amount;
        }

        // Fill diagonal hits 1
        for (int y1 = y + 1, x1 = x + 1; y1 < size && x1 < size; y1++, x1++) {
            this.cells[x1][y1].hits += amount;
        }
        for (int y1 = y - 1, x1 = x - 1; y1 >= 0 && x1 >= 0; y1--, x1--) {
            this.cells[x1][y1].hits += amount;
        }
        // Fill diagonal hits 2
        for (int y1 = y - 1, x1 = x + 1; y1 >= 0 && x1 < size; y1--, x1++) {
            this.cells[x1][y1].hits += amount;
        }
        for (int y1 = y + 1, x1 = x - 1; y1 < size && x1 >= 0; y1++, x1--) {
            this.cells[x1][y1].hits += amount;
        }

    }

    @Override
    public String toString() {
        StringJoiner s = new StringJoiner("\n");
        for (Cell[] cell : cells) {
            s.add(Arrays.toString(cell));
        }
        return s.toString();
    }
}
