package org.evrete.util.compiler;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

abstract class AbstractCompiledClass implements JavaFileObject {
    private final String binaryName;
    private final String name;

    AbstractCompiledClass(String binaryName, String name) {
        this.binaryName = binaryName;
        this.name = name;
    }


    @Override
    public final OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final Reader openReader(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Writer openWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final long getLastModified() {
        return 0;
    }

    @Override
    public final boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Kind getKind() {
        return Kind.CLASS;
    }

    @Override
    public final boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind())
                && (baseName.equals(name)
                || name.endsWith('/' + baseName));
    }

    @Override
    public final NestingKind getNestingKind() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Modifier getAccessLevel() {
        throw new UnsupportedOperationException();
    }

    final String getBinaryName() {
        return binaryName;
    }
}
