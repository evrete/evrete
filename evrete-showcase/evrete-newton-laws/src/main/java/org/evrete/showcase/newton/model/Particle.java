package org.evrete.showcase.newton.model;

public class Particle {
    public int id;
    public int color;
    public double mass;
    public Vector position = new Vector();
    public Vector velocity = new Vector();
    public Vector acceleration = new Vector();


    public static int mixColors(Particle p1, Particle p2) {
        int newColor = 0;

        for (int b = 0; b < 3; b++) {
            int byteValue1 = (p1.color >> 8 * b) & 0xFF;
            int byteValue2 = (p2.color >> 8 * b) & 0xFF;
            int weighted = (int) ((byteValue1 * p1.mass + byteValue2 * p2.mass) / (p1.mass + p2.mass));
            newColor += weighted << (8 * b);

        }
        return newColor;

    }

    public double getRadius() {
        return Math.pow(mass, 1.0 / 3);
    }
}
