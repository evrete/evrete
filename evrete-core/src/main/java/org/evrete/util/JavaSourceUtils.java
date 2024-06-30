package org.evrete.util;

import org.evrete.api.spi.JavaSourceCompiler;

import java.util.Objects;

public final class JavaSourceUtils {
    public static JavaSourceCompiler.ClassSource parse(String source) {
        // 1. Remove block comments first
        String src = removeBlockComments(Objects.requireNonNull(source));

        // 2. Convert to Unix format
        String[] lines = src
                .replaceAll("\n\r", "\n")
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .split("\n");

        // 3. Remove line comments
        StringBuilder sb = new StringBuilder(source.length());
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                sb.append(trimmed).append('\n');
            }
        }

        src = sb.toString().trim();

        // 4. With all the comments removed, we can trim the source till the first curly brace
        int p = source.indexOf('{');
        if (p < 0) {
            throw new IllegalArgumentException("Not a Java source");
        }
        src = src.substring(0, p - 1).trim();

        // 5. Getting package info
        final String packageName;
        p = src.indexOf("package");
        if (p < 0) {
            throw new IllegalArgumentException("Unnamed (default) packages are not supported");
        } else {
            int semi = source.indexOf(';');
            if (semi < 0) {
                throw new IllegalArgumentException("Not a Java source");
            } else {
                packageName = source.substring(p + "package".length() + 1, semi).trim();
            }
        }

        String[] parts = src.replaceAll("\\n", "").split(";");
        String classDef = parts[parts.length - 1];
        String[] words = classDef.split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            String w = words[i];
            if(isClassDef(w)) {
                // The next word should be the class's simple name
                String simpleName = words[i+1].trim();
                String binaryName = packageName + "." + simpleName;
                return new SourceImpl(source, binaryName);
            }
        }

        throw new IllegalArgumentException("Couldn't find any of the class|interface|enum|record keywords");

    }

    private static class SourceImpl implements JavaSourceCompiler.ClassSource {
        private final String source;
        private final String binaryName;

        public SourceImpl(String source, String binaryName) {
            this.source = source;
            this.binaryName = binaryName;
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public String binaryName() {
            return binaryName;
        }
    }

    private static boolean isClassDef(String s) {
        return "class".equals(s)
                ||
                "enum".equals(s)
                ||
                "record".equals(s)
                ||
                "interface".equals(s);
    }


    static String removeBlockComments(String arg) {
        int open = arg.indexOf("/*");
        if (open < 0) {
            return arg;
        } else {
            int close = arg.indexOf("*/", open + 2);
            if (close < 0) {
                throw new IllegalArgumentException("Malformed block comment in '" + arg + "'");
            }

            String blockRemoved = arg.substring(0, open) + arg.substring(close + 2);
            // Continue recursively
            return removeBlockComments(blockRemoved);
        }
    }

}
