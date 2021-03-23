package org.evrete.util.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class BytesClassLoader extends SecureClassLoader {
    private static final Logger LOGGER = Logger.getLogger(BytesClassLoader.class.getName());
    public final Map<String, byte[]> resources = new HashMap<>();
    private final ProtectionDomain protectionDomain;

    public BytesClassLoader(ClassLoader parent, ProtectionDomain protectionDomain) {
        super(parent);
        Objects.requireNonNull(protectionDomain);
        this.protectionDomain = protectionDomain;
    }

    public Class<?> buildClass(byte[] bytes) {
        return defineClass(null, bytes, 0, bytes.length, protectionDomain);
    }

    public void addResource(String name, byte[] bytes) {
        this.resources.put(name, bytes);
    }

    @Override
    protected URL findResource(String name) {
        if (!resources.isEmpty()) {
            //TODO fix
            LOGGER.warning("Redirecting the findResource(String name) call to parent classloader. To access this classloader's resources, please use the getResourceAsStream() method instead");
        }
        return super.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        if (!resources.isEmpty()) {
            //TODO fix
            LOGGER.warning("Redirecting the findResources(String name) call to parent classloader. To access this classloader's resources, please use the getResourceAsStream() method instead");
        }
        return super.findResources(name);
    }

    @Override
    public URL getResource(String name) {
        //TODO fix
        LOGGER.warning("Redirecting the getResource(String name) call to parent classloader. To access this classloader's resources, please use the getResourceAsStream() method instead");
        return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] bytes = resources.get(name);
        if (bytes == null) {
            return super.getResourceAsStream(name);
        } else {
            return new ByteArrayInputStream(bytes);
        }
    }
}
