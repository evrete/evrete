package org.evrete.showcase.abs.town.types;

public class MapPoint {
    public double lat;
    public double lng;

    public MapPoint(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public MapPoint(XYPoint point) {
        this.lng = -87.87325965275137 + 0.00005127905698392554 * point.x;
        this.lat = 42.23492742943774 - 0.000038005613226009095 * point.y;
    }
    //X->Lng – {b: -87.87325965275137, m: 0.00005127905698392554} (index.html, line 194)
    //Y->Lat – {b: 42.23492742943774, m: -0.000038005613226009095} (index.html, line 196)

}
