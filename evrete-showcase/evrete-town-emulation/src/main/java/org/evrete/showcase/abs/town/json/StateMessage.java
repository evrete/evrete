package org.evrete.showcase.abs.town.json;

import org.evrete.showcase.abs.town.types.State;
import org.evrete.showcase.shared.JsonMessage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StateMessage extends JsonMessage {
    public final String time;
    public final int cellSize;
    public final boolean full;
    private final Map<String, List<Area>> layers = new HashMap<>();
    private final Map<String, Integer> maxCounts = new HashMap<>();
    public transient int cellCount = 0;

    public StateMessage(boolean full, int cellSize, String time) {
        super("STATE");
        this.time = time;
        this.cellSize = cellSize;
        this.full = full;
    }

    public void add(State state, Area area) {
        layers.computeIfAbsent(state.name(), k -> new LinkedList<>()).add(area);
        cellCount++;
    }

    public void updateMaxCounts(State state, int count) {
        int current = maxCounts.computeIfAbsent(state.name(), k -> -1);
        if (count > current) {
            maxCounts.put(state.name(), count);
        }
    }

    public boolean hasData() {
        return cellCount > 0;
    }
}
