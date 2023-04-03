package org.evrete.runtime.compiler;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

class ClassPathSource extends AbstractJavaObject {
    private final URI uri;
    private final String binaryName;

    ClassPathSource(String binaryName, URI uri) {
        this.uri = uri;
        this.binaryName = binaryName;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return kind.equals(getKind());
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @Override
    public Writer openWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return uri.toURL().openStream();
    }

    @Override
    String getBinaryName() {
        return binaryName;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "uri=" + uri +
                '}';
    }
}
