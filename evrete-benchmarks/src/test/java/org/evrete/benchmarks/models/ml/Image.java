package org.evrete.benchmarks.models.ml;

public class Image {
    public final String label;

    public Image(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
