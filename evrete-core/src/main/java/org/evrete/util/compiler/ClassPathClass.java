package org.evrete.util.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

class ClassPathClass extends AbstractCompiledClass {
    private final URI uri;

    ClassPathClass(String binaryName, URI uri) {
        super(binaryName, uri.getPath() == null ? uri.getSchemeSpecificPart() : uri.getPath()); // for FS based URI the path is not null, for JAR URI the scheme specific part is not null
        this.uri = uri;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return uri.toURL().openStream(); // easy way to handle any URI!
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "uri=" + uri +
                '}';
    }
}
