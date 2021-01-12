package org.evrete.showcase.abs.town.json;

public class Viewport {
    public static int MAX_SIZE = 2048;
    public static int MAX_ZOOM = 31 - Integer.numberOfLeadingZeros(MAX_SIZE);
    // Top left pixel, x-coordinate
    public int x;
    // Top left pixel, y-coordinate
    public int y;
    // Size in pixels
    public int zoom;

    @Override
    public String toString() {
        return "Viewport{" +
                "x=" + x +
                ", y=" + y +
                ", zoom=" + zoom +
                '}';
    }

}
