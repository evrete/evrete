package org.evrete.showcase.abs.town.types;

import java.util.Arrays;

public class Summary {
    private final int[] counts = new int[State.values().length];
    public int total = 0;

    void add(State state, int count) {
        this.counts[state.ordinal()] += count;
        this.total += count;
    }

    public int getCount(State state) {
        return counts[state.ordinal()];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Summary summary = (Summary) o;
        return Arrays.equals(counts, summary.counts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(counts);
    }

    @Override
    public String toString() {
        return "Summary{" +
                "total=" + total +
                ", counts=" + Arrays.toString(counts) +
                '}';
    }

    public Summary copy() {
        Summary copy = new Summary();
        copy.total = this.total;
        System.arraycopy(this.counts, 0, copy.counts, 0, this.counts.length);
        return copy;
    }
}
