package org.evrete.util.compiler;

public class CompiledClassLoader extends ClassLoader {

    public CompiledClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> buildClass(byte[] bytes) {
        try {
            Class<?> cl = defineClass(null, bytes, 0, bytes.length);
            return loadClass(cl.getName());
        } catch (Throwable t) {
            throw new CompilationException(t);
        }
    }
}
