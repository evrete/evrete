package org.evrete.util.compiler;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

class JavaSource extends SimpleJavaFileObject {
    private final String code;

    private JavaSource(String fileName, String code) {
        super(URI.create("string:///" + fileName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    static Collection<JavaSource> task(String fileName, String code) {
        return Collections.singletonList(new JavaSource(fileName, code));
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
