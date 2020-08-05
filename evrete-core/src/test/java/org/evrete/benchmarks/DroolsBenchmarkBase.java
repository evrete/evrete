package org.evrete.benchmarks;

import java.util.HashMap;
import java.util.Map;

public class DroolsBenchmarkBase {
    static final int objectCount = 4096;
    static final Map<String, Integer> UNIQUENESS_MAPPING = new HashMap<>();
    static final String ONE_TO_1 = "1/1  ";
    static final String ONE_TO_2 = "1/2  ";
    static final String ONE_TO_4 = "1/4  ";
    static final String ONE_TO_8 = "1/8  ";
    static final String ONE_TO_16 = "1/16 ";
    static final String ONE_TO_32 = "1/32 ";
    static final String ONE_TO_64 = "1/64 ";
    static final String ONE_TO_128 = "1/128";
    static final String ONE_TO_256 = "1/256";
    static final String ONE_TO_512 = "1/512";

    static {
        UNIQUENESS_MAPPING.put(ONE_TO_1, 1);
        UNIQUENESS_MAPPING.put(ONE_TO_2, 2);
        UNIQUENESS_MAPPING.put(ONE_TO_4, 4);
        UNIQUENESS_MAPPING.put(ONE_TO_8, 8);
        UNIQUENESS_MAPPING.put(ONE_TO_16, 16);
        UNIQUENESS_MAPPING.put(ONE_TO_32, 32);
        UNIQUENESS_MAPPING.put(ONE_TO_64, 64);
        UNIQUENESS_MAPPING.put(ONE_TO_128, 128);
        UNIQUENESS_MAPPING.put(ONE_TO_256, 256);
        UNIQUENESS_MAPPING.put(ONE_TO_512, 512);
    }

    public enum Implementation {
        Evrete,
        Drools
    }

}
