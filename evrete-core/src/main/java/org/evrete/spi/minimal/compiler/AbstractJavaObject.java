package org.evrete.spi.minimal.compiler;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * <p>
 *     A virtual Java file, that can be either a source, or a compiled .class resource.
 * </p>
 */
abstract class AbstractJavaObject implements JavaFileObject {
    //private final String packageName;

    AbstractJavaObject() {
    }

    abstract String getBinaryName();

    @Override
    public NestingKind getNestingKind() {
        return null;
    }

    @Override
    public Modifier getAccessLevel() {
        return null;
    }

    @Override
    public final String getName() {
        return this.getClass().getSimpleName() + ":" + getBinaryName();
    }

    @Override
    public final Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return new StringReader(getCharContent(ignoreEncodingErrors).toString());
    }

    @Override
    public final long getLastModified() {
        return 0;
    }

    @Override
    public boolean delete() {
        return true;
    }

}
