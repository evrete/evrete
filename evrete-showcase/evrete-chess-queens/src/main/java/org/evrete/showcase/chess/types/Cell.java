package org.evrete.showcase.chess.types;

public class Cell {
    public boolean queen;
    public int hits;
    public int x;
    public int y;

    public Cell(int x, int y) {
        this.queen = false;
        this.hits = 0;
        this.x = x;
        this.y = y;
    }

    public Cell(Cell other) {
        this.queen = other.queen;
        this.hits = other.hits;
        this.x = other.x;
        this.y = other.y;
    }

    @Override
    public String toString() {
        return queen ? "Q" : (hits > 0 ? "x" : " ");
    }

    public Cell copy() {
        return new Cell(this);
    }
}
