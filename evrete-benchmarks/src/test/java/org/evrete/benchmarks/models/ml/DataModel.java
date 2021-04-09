package org.evrete.benchmarks.models.ml;

public class DataModel {
    public int blackHoleData;
    public int computations;

    @SuppressWarnings("unused")
    public void compute(Image img1, Image img2) {
        blackHoleData += img1.hashCode();
        blackHoleData += img2.hashCode();
        computations++;
    }
}
