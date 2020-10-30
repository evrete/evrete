package org.evrete.showcase.newton.types;

@SuppressWarnings("unused")
public class Vector {
    public double x;
    public double y;

    public Vector() {
        this(0.0, 0.0);
    }

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector multiply(double k) {
        return new Vector(this.x * k, this.y * k);
    }

    public Vector plus(Vector v) {
        return new Vector(this.x + v.x, this.y + v.y);
    }

    public Vector minus(Vector v) {
        return new Vector(this.x - v.x, this.y - v.y);
    }

    public Vector unitVector() {
        double size = size();
        if (size == 0) throw new IllegalStateException();
        return multiply(1.0 / size);
    }


    public double size() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    @Override
    public String toString() {
        return "{x=" + x +
                ", y=" + y +
                '}';
    }
}
