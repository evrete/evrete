package org.evrete.spi.minimal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class Const {
    private static final Set<String> JAVA13_RESERVED_WORDS = new HashSet<>(
            Arrays.asList(
                    "abstract", "continue", "for", "new", "switch",
                    "assert", "default", "if", "package", "synchronized",
                    "boolean", "do", "goto", "private", "this",
                    "break", "double", "implements", "protected", "throw",
                    "byte", "else", "import", "public", "throws",
                    "case", "enum", "instanceof", "return", "transient",
                    "catch", "extends", "int", "short", "try",
                    "char", "final", "interface", "static", "void",
                    "class", "finally", "long", "strictfp", "volatile",
                    "const", "float", "native", "super", "while"
            )
    );

    private static final Set<String> LOCAL_RESERVED_WORDS = new HashSet<>(
            Arrays.asList(
                    "true", "false", "null", "var"
            )
    );

    static void assertName(String var) {
        if (LOCAL_RESERVED_WORDS.contains(var) || JAVA13_RESERVED_WORDS.contains(var)) {
            throw new IllegalArgumentException("Reserved word: '" + var + "'");
        }
    }

}
