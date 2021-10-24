package org.evrete.dsl;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TestUtils {

    public static void testFile(Object f) {
        new File(f.toString()).exists();
    }

    /**
     * Returns the Java version as an int value.
     *
     * @return the Java version as an int value (8, 9, etc.)
     * @since 12130
     */
    static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        // Allow these formats:
        // 1.8.0_72-ea
        // 9-ea
        // 9
        // 9.0.1
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
    }

    public static class EnvHelperData {
        private static final Map<String, List<Object>> data = new HashMap<>();
        private static int count = 0;

        public static void reset() {
            data.clear();
            count = 0;
        }

        public static void add(String property, Object val) {
            data.computeIfAbsent(property, k-> new ArrayList<>()).add(val);
            count++;
        }

        static int total() {
            return count;
        }

        static int total(String property) {
            List<Object> l = data.get(property);
            return l == null ? 0 : l.size();
        }

    }

    public static class PhaseHelperData {
        static final EnumMap<Phase, AtomicInteger> EVENTS = new EnumMap<>(Phase.class);

        static {
            reset();
        }

        public static void reset() {
            for (Phase phase : Phase.values()) {
                EVENTS.put(phase, new AtomicInteger());
            }
        }

        public static int count(Phase phase) {
            return EVENTS.get(phase).get();
        }

        public static void event(Phase... phases) {
            for (Phase phase : phases) {
                EVENTS.get(phase).incrementAndGet();
            }
        }

        static int total() {
            int total = 0;
            for (AtomicInteger i : EVENTS.values()) {
                total += i.get();
            }
            return total;
        }
    }
}
