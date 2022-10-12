package org.evrete.util.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

class CompiledClass extends AbstractCompiledClass {
    private final String packageName;
    private final byte[] bytes;

    CompiledClass(Class<?> cl, byte[] bytes) {
        super(cl.getName(), cl.getName());
        this.packageName = resolvePackageName(cl);
        this.bytes = bytes;
    }


    String getPackageName() {
        return packageName;
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    private static String resolvePackageName(Class<?> clazz) {
        Package p = clazz.getPackage();
        if(p == null) {
            String name = clazz.getName();
            int lastDot = name.lastIndexOf('.');
            if(lastDot < 0) {
                return "unnamed";
            } else {
                return name.substring(0, lastDot);
            }
        } else {
            return p.getName();
        }
    }
}
