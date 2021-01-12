package org.evrete.showcase.shared;

import com.google.gson.Gson;

import javax.servlet.ServletContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        } catch (InterruptedException ignored) {

        }
    }

    public static String readResourceAsString(ServletContext ctx, String path) throws IOException {

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(); InputStream is = ctx.getResourceAsStream(path)) {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

}
