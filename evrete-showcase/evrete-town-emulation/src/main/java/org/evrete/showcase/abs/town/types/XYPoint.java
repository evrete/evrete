package org.evrete.showcase.abs.town.types;

public class XYPoint {
    public int x;
    public int y;

    public XYPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public XYPoint(MapPoint point) {
        this.x = (int) (1713625.9961169402 + 19501.10877513415 * point.lng);
        this.y = (int) (1111274.6049598006 - 26311.74400482747 * point.lat);
    }

    //Lng->X – {b: 1713625.9961169402, m: 19501.10877513415} (index.html, line 195)
    //Lat->Y – {b: 1111274.6049598006, m: -26311.74400482747} (index.html, line 197)

    public static int distance2(XYPoint p1, XYPoint p2) {
        return (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XYPoint point = (XYPoint) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return x + 31 * y;
    }
}
