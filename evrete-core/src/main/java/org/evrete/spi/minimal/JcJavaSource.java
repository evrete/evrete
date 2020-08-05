package org.evrete.spi.minimal;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

class JcJavaSource extends SimpleJavaFileObject {
    private final String code;

    private JcJavaSource(String fileName, String code) {
        super(URI.create("string:///" + fileName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    static Collection<JcJavaSource> task(String fileName, String code) {
        return Collections.singletonList(new JcJavaSource(fileName, code));
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
