package org.evrete.showcase.stock;

import com.google.gson.Gson;

public class Utils {

    static String toJson(Object o) {
        return new Gson().toJson(o);
    }

    static <T> T fromJson(String src, Class<T> t) {
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
