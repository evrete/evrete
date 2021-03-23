package org.evrete.showcase.abs.town.types;

import java.security.SecureRandom;
import java.util.List;

public final class RandomUtils {
    private final static SecureRandom random = new SecureRandom();

    public static boolean randomBoolean(double probability) {
        return random.nextDouble() < probability;
    }

    public static int randomGaussian1(int mean, int stdDev, int maximum) {
        double rand = random.nextGaussian() * stdDev + mean;
        if (rand <= 0 || rand > maximum) {
            return randomGaussian1(mean, stdDev, maximum);
        } else {
            return (int) rand;
        }
    }

    public static int random(int range) {
        return random.nextInt(range);
    }


    static double nextDouble() {
        return random.nextDouble();
    }

    static <T> T randomListElement(List<T> data) {
        return data.get(random.nextInt(data.size()));
    }
}
