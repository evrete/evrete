package org.evrete.spi.minimal.compiler;

import org.evrete.api.spi.SourceCompiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

class JavaSourceObject extends AbstractJavaObject {
    private static final AtomicLong SOURCE_COUNTER = new AtomicLong();

    private final String simpleName;
    private final SourceCompiler.ClassSource source;

    private final URI uri;

    JavaSourceObject(SourceCompiler.ClassSource source) {
        ClassMeta meta = new ClassMeta(source.binaryName());
        this.simpleName = meta.getSimpleName();
        this.source = source;
        this.uri = URI.create("string:///source-" + SOURCE_COUNTER.incrementAndGet() + "."  + Kind.SOURCE.extension);
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return kind == Kind.SOURCE && this.simpleName.equals(simpleName);
    }

    SourceCompiler.ClassSource getSource() {
        return source;
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
