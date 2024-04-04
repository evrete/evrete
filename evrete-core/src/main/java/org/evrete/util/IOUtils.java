package org.evrete.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Utility class for input/output operations.
 */
public final class IOUtils {
    public static byte[] toByteArray(InputStream is) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[4];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                bos.write(data, 0, nRead);
            }

            bos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public static byte[] bytes(JarFile jarFile, ZipEntry entry) {
        try {
            return toByteArray(jarFile.getInputStream(entry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
