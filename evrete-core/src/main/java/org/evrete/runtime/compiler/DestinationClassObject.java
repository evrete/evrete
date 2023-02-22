package org.evrete.runtime.compiler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

public class DestinationClassObject extends AbstractJavaObject {
    private final String binaryName;

    private final ByteArrayOutputStream outputStream;
    private final URI uri;

    public DestinationClassObject(String binaryName) {
        this.binaryName = binaryName;
        this.outputStream = new ByteArrayOutputStream();
        this.uri = URI.create("out:///" + binaryName.replaceAll("\\.", "/") + Kind.CLASS.extension);
    }

    @Override
    public OutputStream openOutputStream()  {
        return outputStream;
    }

    @Override
    public Writer openWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public InputStream openInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    byte[] getBytes() {
        return outputStream.toByteArray();
    }

    @Override
    String getBinaryName() {
        return binaryName;
    }

    @Override
    public Kind getKind() {
        throw new UnsupportedOperationException("TODO implement");
    }
}
