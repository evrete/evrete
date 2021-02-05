package org.evrete.util.compiler;

import javax.tools.ForwardingJavaFileObject;
import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

class ClassBytesFileObject<F extends JavaFileObject> extends ForwardingJavaFileObject<F> {
    private final ByteArrayOutputStream bos;

    ClassBytesFileObject(F fileObject, ByteArrayOutputStream bos) {
        super(fileObject);
        this.bos = bos;
    }

    @Override
    public OutputStream openOutputStream() {
        return bos;
    }
}
