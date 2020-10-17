package org.evrete.showcase.shared;

import com.google.gson.Gson;

public class Utils {

    public static String toJson(Object o) {
        return new Gson().toJson(o);
    }

    public static <T> T fromJson(String src, Class<T> t) {
        return new Gson().fromJson(src, t);
    }

    public static void delay(int ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
