package org.evrete.spi.minimal;

import javax.tools.ForwardingJavaFileObject;
import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

class JcBytesJavaFileObject<F extends JavaFileObject> extends ForwardingJavaFileObject<F> {
    private final ByteArrayOutputStream bos;

    JcBytesJavaFileObject(F fileObject, ByteArrayOutputStream bos) {
        super(fileObject);
        this.bos = bos;
    }


    @Override
    public OutputStream openOutputStream() {
        return bos;
    }
}
