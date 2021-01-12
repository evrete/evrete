package org.evrete.showcase.abs.town.types;

public enum State {
    HOME(new int[]{0, 127, 0}), // Green
    WORKING(new int[]{127, 0, 0}), // Red
    SHOPPING(new int[]{0, 0, 127}); // Blue

    private final int[] colors;

    State(int[] colors) {
        this.colors = colors;
    }

    public int[] getColors() {
        return colors;
    }
}
