package org.evrete.spi.minimal;

import java.util.HashMap;
import java.util.Map;

class JcClassLoader extends ClassLoader {
    private final Map<String, byte[]> classBytes = new HashMap<>();

    JcClassLoader(ClassLoader parent) {
        super(parent);
    }

    Class<?> buildClass(String className, byte[] bytes) {
        try {
            classBytes.put(className, bytes);
            return loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] b = classBytes.get(name);
        if (b == null || b.length == 0) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(null, b, 0, b.length);
    }
}
