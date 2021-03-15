package org.evrete.util.compiler;

public class BytesClassLoader extends ClassLoader {

    public BytesClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> buildClass(byte[] bytes) {
        try {
            Class<?> cl = defineClass(null, bytes, 0, bytes.length);
            return loadClass(cl.getName());
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }
}
