package org.evrete.benchmarks.models.ml;


import org.evrete.benchmarks.RuleEngines;

@SuppressWarnings("ALL")
public class ImageProcessor {
    public int blackHoleData;
    public int computations;

    @SuppressWarnings("unused")
    public void compute(Image img1, Image img2) {
        RuleEngines.rhsLoad();
        computations++;
    }

    public boolean test(String label1, String label2) {
        RuleEngines.lhsLoad();
        return label1.equals(label2);
    }
}
