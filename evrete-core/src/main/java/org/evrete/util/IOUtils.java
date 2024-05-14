package org.evrete.util;

import java.io.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Utility class for input/output operations.
 */
public final class IOUtils {
    public static byte[] toByteArray(InputStream is) {
        try {
            return toByteArrayChecked(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String toString(Reader reader) throws IOException {
        char[] arr = new char[8192];
        StringBuilder buffer = new StringBuilder();
        int numRead;
        while ((numRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numRead);
        }
        return buffer.toString();
    }

    public static byte[] toByteArrayChecked(InputStream is) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[8192];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                bos.write(data, 0, nRead);
            }

            bos.flush();
            return bos.toByteArray();
        }
    }

    public static <A, R, S extends Closeable> R[] read(Class<R> type, A[] args, IOFunction<A, S> reader, IOFunction<S, R> mapper) throws IOException {
        int length = args.length;
        R[] result = CollectionUtils.array(type, length);

        for (int i = 0; i < length; i++) {
            try (S stream = reader.apply(args[i])) {
                result[i] = mapper.apply(stream);
            }
        }
        return result;
    }


    public static byte[] bytes(JarFile jarFile, ZipEntry entry) {
        try {
            return toByteArray(jarFile.getInputStream(entry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
