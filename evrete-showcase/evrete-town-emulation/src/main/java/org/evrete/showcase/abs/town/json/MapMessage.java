package org.evrete.showcase.abs.town.json;

import org.evrete.showcase.abs.town.types.XYPoint;
import org.evrete.showcase.shared.JsonMessage;

import java.util.List;

public class MapMessage extends JsonMessage {
    public String category;
    public List<XYPoint> points;

    public MapMessage(String category, List<XYPoint> points) {
        super("MAP_DATA");
        this.points = points;
        this.category = category;
    }

}
