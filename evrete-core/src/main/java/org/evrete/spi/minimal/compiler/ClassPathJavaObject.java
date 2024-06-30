package org.evrete.spi.minimal.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

class ClassPathJavaObject extends AbstractJavaObject {
    private final byte[] bytes;

    private final Class<?> clazz;
    private final URI uri;

    ClassPathJavaObject(Class<?> cl, byte[] bytes) {
        this.bytes = bytes;
        this.clazz = cl;
        this.uri = URI.create("class:///" + cl.getName().replaceAll("\\.", "/") + "." + Kind.CLASS.extension);
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return kind.equals(getKind()) && simpleName.equals(this.clazz.getSimpleName());
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public Writer openWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    String getBinaryName() {
        return clazz.getName();
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }


    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(bytes);
    }
}
