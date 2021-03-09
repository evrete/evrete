package org.evrete.util.compiler;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

class JavaSource extends SimpleJavaFileObject {
    private final String code;
    private final static String CLASS_KEYWORD = "class";
    private final static char CURLY_BRACKET_LEFT = '{';
    private static final AtomicInteger folderId = new AtomicInteger();

    private JavaSource(URI uri, String code) {
        super(uri, Kind.SOURCE);
        this.code = code;
    }

    static Collection<JavaSource> task(String sourceCode) {
        String derivedClassName = guessJavaClassName(sourceCode);
        String virtualFolder = "folder" + folderId.incrementAndGet();
        URI uri = URI.create("string:///" + virtualFolder + "/" + derivedClassName + Kind.SOURCE.extension);
        return Collections.singletonList(new JavaSource(uri, sourceCode));
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }

    private static String guessJavaClassName(String source) {
        Objects.requireNonNull(source, "Source can not be null");
        // First occurrences of the 'class' keyword and left curly brackets are the source of class definition
        int classStart = source.indexOf(CLASS_KEYWORD);
        int classEnd = source.indexOf(CURLY_BRACKET_LEFT);
        if (classStart < 0 || classEnd < 0 || classEnd < classStart) {
            Logger.getAnonymousLogger().warning("Source code:\n" + source);
            throw new CompilationException("Unable to locate class definition, see the error logs for the corresponding source code");
        }

        String scope = source.substring(classStart + CLASS_KEYWORD.length(), classEnd);
        // If we split the scope by whitespace, the first non-empty entry should be the name of the class
        String[] parts = scope.split("\\s");
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                return part;
            }
        }
        Logger.getAnonymousLogger().warning("Source code:\n" + source);
        throw new CompilationException("Unable to locate class definition, see the error logs for the corresponding source code");
    }
}
