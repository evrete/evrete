package org.evrete.showcase.newton.model;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Particle {
    final Map<String, Vector> vectors = new HashMap<>();
    public int id;
    public int color;
    public double mass;

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

    @Override
    public String toString() {
        return "{id=" + id +
                ", color=" + color +
                ", mass=" + mass +
                ", vectors=" + vectors +
                '}';
    }

    public double getRadius() {
        return Math.pow(mass, 1.0 / 3);
    }

    public void set(String name, Vector v) {
        this.vectors.put(name, v);
    }

    public Vector get(String name) {
        Vector v = vectors.get(name);
        if (v == null) {
            throw new IllegalStateException("Vector '" + name + "' is not set");
        } else {
            return v;
        }
    }
}
