package org.evrete.runtime.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

class ClassPathJavaObject extends AbstractJavaObject {
    private final String packageName;
    private final byte[] bytes;

    private final Class<?> clazz;
    private final URI uri;

    ClassPathJavaObject(Class<?> cl, byte[] bytes) {
        this.packageName = resolvePackageName(cl);
        this.bytes = bytes;
        this.clazz = cl;
        this.uri = URI.create("class:///" + cl.getName().replaceAll("\\.", "/") + "." + Kind.CLASS.extension);
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public Writer openWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }


    private static String resolvePackageName(Class<?> clazz) {
        Package p = clazz.getPackage();
        if (p == null) {
            String name = clazz.getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot < 0) {
                return "unnamed";
            } else {
                return name.substring(0, lastDot);
            }
        } else {
            return p.getName();
        }
    }

    @Override
    String getBinaryName() {
        return clazz.getName();
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }


    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(bytes);
    }
}
