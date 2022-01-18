package org.evrete.benchmarks.helper;

import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;

public class OutLogger {
    private static final ThreadLocal<List<String>> data = ThreadLocal.withInitial(LinkedList::new);

    @SuppressWarnings("unused")
    public static synchronized void out(String data) {
        OutLogger.data.get().add(data);
    }

    @SuppressWarnings("unused")
    public static void out(Object... objects) {
        StringJoiner joiner = new StringJoiner(" ");
        for (Object o : objects) {
            joiner.add(o.toString());
        }
        out(joiner.toString());
    }

    public static void reset() {
        data.get().clear();
    }

    public static void assertCount(int count) {
        List<String> l = data.get();

        assert l.size() == count : "Actual " + l.size() + " vs expected " + count;
    }

    public static List<String> getData() {
        return data.get();
    }
}
