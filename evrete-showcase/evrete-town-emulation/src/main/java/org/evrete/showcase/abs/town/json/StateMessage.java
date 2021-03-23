package org.evrete.showcase.abs.town.json;

import org.evrete.showcase.abs.town.types.LocationState;
import org.evrete.showcase.abs.town.types.UiStateCounter;
import org.evrete.showcase.shared.JsonMessage;

import java.util.*;

public class StateMessage extends JsonMessage {
    public final String time;
    public final int cellSize;
    public final boolean reset;
    private final Map<String, List<Area>> layers = new HashMap<>();
    private final Map<String, Double> total = new LinkedHashMap<>();

    public StateMessage(boolean reset, int cellSize, String time) {
        super("STATE");
        this.time = time;
        this.cellSize = cellSize;
        this.reset = reset;
    }

    public void add(LocationState state, Area area) {
        layers.computeIfAbsent(state.name(), k -> new LinkedList<>()).add(area);
    }

    public void setTotal(UiStateCounter summary) {
        int total = 0;
        for (LocationState state : LocationState.values()) {
            total += summary.getCount(state);
        }


        for (LocationState state : LocationState.values()) {
            this.total.put(state.name(), 1.0 * summary.getCount(state) / total);
        }
    }
}
