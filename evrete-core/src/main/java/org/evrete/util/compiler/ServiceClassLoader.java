package org.evrete.util.compiler;

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureClassLoader;
import java.util.*;

public class ServiceClassLoader extends SecureClassLoader {
    private final Map<String, byte[]> resources = new HashMap<>();
    private final List<CompiledClass> localCompiledClasses = new ArrayList<>();

    public ServiceClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> buildClass(byte[] bytes) {
        Class<?> cl = defineClass(null, bytes, 0, bytes.length);
        this.localCompiledClasses.add(new CompiledClass(cl, bytes));
        return cl;
    }

    public void addResource(String name, byte[] bytes) {
        this.resources.put(name, bytes);
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
    public InputStream getResourceAsStream(String name) {
        byte[] bytes = resources.get(name);
        if (bytes == null) {
            return super.getResourceAsStream(name);
        } else {
            return new ByteArrayInputStream(bytes);
        }
    }
}
