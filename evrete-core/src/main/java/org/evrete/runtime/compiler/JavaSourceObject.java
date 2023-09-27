package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

class JavaSourceObject extends AbstractJavaObject {
    private static final AtomicLong SOURCE_COUNTER = new AtomicLong();

    private final String simpleName;
    private final JavaSourceCompiler.ClassSource source;

    private final URI uri;

    JavaSourceObject(JavaSourceCompiler.ClassSource source) {
        ClassMeta meta = new ClassMeta(source.binaryName());
        this.simpleName = meta.getSimpleName();
        this.source = source;
        this.uri = URI.create("string:///source-" + SOURCE_COUNTER.incrementAndGet() + "."  + Kind.SOURCE.extension);
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return kind == Kind.SOURCE && this.simpleName.equals(simpleName);
    }

    JavaSourceCompiler.ClassSource getSource() {
        return source;
    }

    static JavaSourceCompiler.ClassSource parse(String source) {

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
                ClassMeta meta = new ClassMeta(packageName, words[i+1].trim());
                return new JavaSourceCompiler.ClassSource() {
                    @Override
                    public String binaryName() {
                        return meta.getBinaryName();
                    }

                    @Override
                    public String getSource() {
                        return source;
                    }
                };
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


    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(source.getSource().getBytes());
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
        return this.source.getSource();
    }

    @Override
    String getBinaryName() {
        return source.binaryName();
    }

    @Override
    public String toString() {
        String sep =  System.lineSeparator();
        StringBuilder sb = new StringBuilder(this.source.getSource().length() * 2);
        sb.append(sep)
                .append('\'')
                .append(getBinaryName()).append("':")
                .append(sep)
        ;

        Scanner scanner = new Scanner(source.getSource());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            sb.append("\t").append(line).append(sep);
        }


        return sb.toString();
    }
}
