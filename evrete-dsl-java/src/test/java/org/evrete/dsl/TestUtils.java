package org.evrete.dsl;

import java.io.File;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TestUtils {

    public static void testFile(Object f) {
        new File(f.toString()).exists();
    }
}
