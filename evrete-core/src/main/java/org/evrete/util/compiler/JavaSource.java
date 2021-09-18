package org.evrete.util.compiler;

import org.evrete.util.StringLiteralRemover;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

final class JavaSource extends SimpleJavaFileObject {
    private final String code;
    private final static String KEYWORD_CLASS = "class";
    private final static String KEYWORD_INTERFACE = "interface";
    private final static String KEYWORD_RECORD = "record";
    private final static String KEYWORD_ANNOTATION = "@interface";

    private static final String[] JAVA_TYPES = {
            KEYWORD_CLASS,
            KEYWORD_INTERFACE,
            KEYWORD_RECORD,
            KEYWORD_ANNOTATION
    };

    private static final AtomicInteger folderId = new AtomicInteger();

    private JavaSource(URI uri, String code) {
        super(uri, Kind.SOURCE);
        this.code = code;
    }

    static Collection<JavaSource> task(String sourceCode) throws CompilationException {
        String derivedClassName = guessJavaClassName(sourceCode);
        String virtualFolder = "folder" + folderId.incrementAndGet();
        URI uri = URI.create("string:///" + virtualFolder + "/" + derivedClassName + Kind.SOURCE.extension);
        return Collections.singletonList(new JavaSource(uri, sourceCode));
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }


    /**
     * <p>
     * This method strips the source down to a simple String which can be used
     * to derive Java type name, whether it's a class, interface, enum or an annotation.
     * </p>
     *
     * @param source source code
     * @return top-level type name
     */
    private static String guessJavaClassName(String source) throws CompilationException {
        Objects.requireNonNull(source, "Source can not be null");

        // 1. Converting to Unix format
        String stripped = source.replaceAll("\n\r", "\n");
        stripped = stripped.replaceAll("\r\n", "\n");
        stripped = stripped.replaceAll("\r", "\n");

        // 2. Split by newline and remove line comments, imports and package definition
        String[] lines = stripped.split("\n");
        StringBuilder sb = new StringBuilder(source.length());
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                line = line.trim();
                if (!line.startsWith("//") && !line.startsWith("import") && !line.startsWith("package")) {
                    sb.append(line).append("\n");
                }
            }
        }
        stripped = sb.toString();

        // 3. Strip String constants (they will be replaced with ${const...} references
        StringLiteralRemover r = StringLiteralRemover.of(stripped, false);
        stripped = r.getConverted();
        for (String c : r.getConstantMap().keySet()) {
            int pos = stripped.indexOf(c);
            stripped = stripped.substring(0, pos) + "\"\"" + stripped.substring(pos + c.length());
        }

        // 4. Stripping block comments
        int commentStart;
        while ((commentStart = stripped.indexOf("/*")) >= 0) {
            int commentEnd = stripped.indexOf("*/", commentStart);
            if (commentEnd < 0) {
                throw new CompilationException("Unable to derive type name: unbalanced block comments.", source);
            } else {
                stripped = stripped.substring(0, commentStart) + stripped.substring(commentEnd + 2);
            }
        }

        // 5. Now that comments and string literals are removed, every bracket pair
        // is balanced and safe for removal
        stripped = clearBrackets(stripped, '(', ')');
        stripped = clearBrackets(stripped, '{', '}');

        // 6. Make the rest a one-liner
        while (stripped.contains("\n")) {
            stripped = stripped.replaceAll("\n", " ");
        }

        // 7. Remove duplicate whitespaces and split
        String[] typeDeclarationWords = stripped
                .replaceAll("\\s+", " ")
                .split("\\s");

        // 8. Now the words array contains optional empty string, type annotation
        // followed by type declaration elements like [, @Annotation1, @Annotation2, class, MyClass, extends, OtherClass]

        for (String type : JAVA_TYPES) {
            int pos = findIndexOf(type, typeDeclarationWords);
            if (pos >= 0) {
                // Type name follows immediately after the 'class'/'interface' keyword
                int nameIndex = pos + 1;
                if (nameIndex >= typeDeclarationWords.length) {
                    throw new IllegalStateException();
                }
                return typeDeclarationWords[nameIndex];
            }
        }

        Logger.getAnonymousLogger().warning("Source code:\n" + source);
        throw new CompilationException("Unable to locate class definition, see the error logs for the corresponding source code", source);
    }

    private static String clearBrackets(String source, char openingBracket, char closingBracket) {
        int start;
        String current = source;
        while ((start = current.indexOf(openingBracket)) >= 0) {

            int nested = 0;
            for (int pos = start + 1; pos < current.length(); pos++) {
                char c = current.charAt(pos);
                if (c == openingBracket) {
                    nested++;
                }

                if (c == closingBracket) {
                    if (nested == 0) {
                        current = current.substring(0, start) + current.substring(pos + 1);
                        break;
                    } else {
                        nested--;
                    }
                }
            }
        }
        return current;
    }

    private static int findIndexOf(String arg, String[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arg.equals(arr[i])) {
                return i;
            }
        }
        return -1;
    }
}
