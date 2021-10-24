package org.evrete.util.compiler;

import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URL;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.*;
import java.util.logging.Logger;

public class ServiceClassLoader extends SecureClassLoader {
    private static final Logger LOGGER = Logger.getLogger(ServiceClassLoader.class.getName());
    private final Map<String, byte[]> resources = new HashMap<>();
    private final ProtectionDomain protectionDomain;
    private final List<CompiledClass> localCompiledClasses = new ArrayList<>();

    public ServiceClassLoader(ClassLoader parent, ProtectionDomain protectionDomain) {
        super(parent);
        Objects.requireNonNull(protectionDomain);
        this.protectionDomain = protectionDomain;
    }

    public Class<?> buildClass(byte[] bytes) {
        Class<?> cl = defineClass(null, bytes, 0, bytes.length, protectionDomain);
        this.localCompiledClasses.add(new CompiledClass(cl, bytes));
        return cl;
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

    Collection<JavaFileObject> getCompiledClasses(String packageName) {
        ClassLoader current = this;
        Collection<JavaFileObject> col = new ArrayList<>();
        while (current !=  null) {
            if(current instanceof  ServiceClassLoader) {
                ServiceClassLoader scl = (ServiceClassLoader) current;
                for(CompiledClass cc : scl.localCompiledClasses) {
                    if(cc.getPackageName().equals(packageName)) {
                        col.add(cc);
                    }
                }
            }
            current = current.getParent();
        }
        return col;
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
