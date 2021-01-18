package org.evrete.showcase.abs.town.types;

public class Location extends XYPoint {
    public int id;

    public Location(int id, XYPoint point) {
        super(point.x, point.y);
        this.id = id;
    }
    //public List<XYPoint> nearestRoads = new ArrayList<>();
}
