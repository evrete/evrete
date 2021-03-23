package org.evrete.showcase.abs.town.types;

import org.evrete.showcase.abs.town.json.Viewport;

public class UiStateCounts {
    private final UiStateCounter[][][] counts = new UiStateCounter[Integer.SIZE - Integer.numberOfLeadingZeros(Viewport.MAX_SIZE)][][];

    public UiStateCounts() {
        for (int z = 0; z < counts.length; z++) {
            int areaSize = 1 << z;
            counts[z] = new UiStateCounter[areaSize][areaSize];
            for (int i = 0; i < areaSize; i++) {
                for (int j = 0; j < areaSize; j++) {
                    counts[z][i][j] = new UiStateCounter();
                }
            }
        }
    }

    public UiStateCounter getCellSummary(int zoom, int zoomedX, int zoomedY) {
        UiStateCounter[][] counts = this.counts[zoom];
        return counts[zoomedX][zoomedY];
    }

    public UiStateCounter getTotal() {
        return counts[0][0][0];
    }

    private void addTo(LocationState state, int x, int y, int count) {
        if (x < 0 || y < 0) throw new IllegalArgumentException();
        for (int zoom = 0; zoom < counts.length; zoom++) {
            UiStateCounter[][] counts = this.counts[zoom];
            int div = Viewport.MAX_SIZE >> zoom;
            int zoomedX = x / div;
            int zoomedY = y / div;
            counts[zoomedX][zoomedY].add(state, count);
        }
    }

    public void addTo(LocationState state, Entity location, int count) {
        addTo(state, location.getNumber("x", -1), location.getNumber("y", -1), count);
    }
}
