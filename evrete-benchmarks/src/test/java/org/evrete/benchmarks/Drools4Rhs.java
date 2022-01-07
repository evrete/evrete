package org.evrete.benchmarks;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Drools4Rhs {
    private static final List<String> data = new LinkedList<>();

    @SuppressWarnings("unused")
    public static void out(String data) {
        Drools4Rhs.data.add(data);
    }

    public static void reset() {
        data.clear();
    }

    public static void assertCount(int count) {
        assert data.size() == count : "Actual " + data.size() + " vs expected " + count;
    }

    static List<String> getData() {
        return new ArrayList<>(data);
    }
}
