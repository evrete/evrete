package org.evrete.runtime.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Objects;
import java.util.Scanner;

public class JavaSource extends AbstractJavaObject {
    private final String className;
    private final String packageName;
    private final String source;

    private final URI uri;

    private JavaSource(long prefix, String packageName, String className, String source) {
        this.packageName = packageName;
        this.className = className;
        this.source = source;
        this.uri = URI.create("string:///" + prefix + "/" + packageName.replaceAll("\\.", "/") + "/" + className + Kind.SOURCE.extension);
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return kind == Kind.SOURCE && className.equals(simpleName);
    }

    static JavaSource parse(long prefix, String source) {
        if(prefix < 0) {
            throw new IllegalStateException();
        }

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
                // The next word should be the class's name
                return new JavaSource(prefix, packageName, words[i+1].trim(), source);
            }
        }

        throw new IllegalArgumentException("Couldn't find any of the class|interface|enum|record keywords");
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

    @Override
    public Writer openWriter() {
        throw new UnsupportedOperationException();
    }

    String getClassName() {
        return className;
    }

    String getPackageName() {
        return packageName;
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(source.getBytes());
    }

    @Override
    public Kind getKind() {
        return Kind.SOURCE;
    }

    @Override
    public URI toUri() {
        return this.uri;
    }

    @Override
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return this.source;
    }

    @Override
    String getBinaryName() {
        return packageName + "." + className;
    }

    @Override
    public String toString() {
        String sep =  System.lineSeparator();
        StringBuilder sb = new StringBuilder(this.source.length() * 2);
        sb.append(sep)
                .append('\'')
                .append(getBinaryName()).append("':")
                .append(sep)
        ;

        Scanner scanner = new Scanner(source);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            sb.append("\t").append(line).append(sep);
        }


        return sb.toString();
    }
}
