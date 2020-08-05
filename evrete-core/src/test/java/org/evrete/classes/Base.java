package org.evrete.classes;

@SuppressWarnings("unused")
public abstract class Base {
    private String id;

    public int i;
    private float f;
    public double d;
    private long l;
    public short s;


    Base(String id) {
        this.id = id;
    }

    Base() {
        this("");
    }

    Base(int i) {
        this.i = i;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    @SuppressWarnings("MethodMayBeStatic")
    public boolean testHello(String s) {
        return "Hello world".equals(s);
    }


    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public long getL() {
        return l;
    }

    public void setL(long l) {
        this.l = l;
    }

    public short getS() {
        return s;
    }

    public void setS(short s) {
        this.s = s;
    }


    public void setAllNumeric(int i) {
        this.i = i;
        this.d = i;
        this.f = i;
        this.l = i;
        this.s = (short) i;
    }

    public synchronized void waitNs(long i) {
        long start = System.nanoTime();
        long end;
        do {
            end = System.nanoTime();
        } while (start + i >= end);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", i=" + i +
                '}';
    }
}
