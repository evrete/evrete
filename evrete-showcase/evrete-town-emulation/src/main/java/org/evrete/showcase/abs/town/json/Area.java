package org.evrete.showcase.abs.town.json;

import org.evrete.showcase.abs.town.types.XYPoint;

public class Area extends XYPoint {
    public final String id;
    public final int count;

    public Area(String id, int x, int y, int count) {
        super(x, y);
        this.id = id;
        this.count = count;
    }
}
