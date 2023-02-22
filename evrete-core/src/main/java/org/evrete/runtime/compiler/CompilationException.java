package org.evrete.runtime.compiler;

import java.util.Collection;
import java.util.StringJoiner;

public class CompilationException extends Exception {

    private static final long serialVersionUID = -8017644675581374126L;

    public CompilationException(String message, String source) {
        super(toMessage(message, source));
    }

    public CompilationException(String message, Collection<JavaSource> sources) {
        super(toMessage(message, toMessage(sources)));
    }

    public CompilationException(Throwable cause, String source) {
        super(toMessage(cause, source), cause);
    }

    private static String toMessage(String message, String source) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("Compilation error");
        if (message != null) {
            sb.append(":").append(message);
        }

        if (source != null) {
            sb.append("\n\n")
                    .append("Source(s):")
                    .append(source)
                    .append("\n");
        }
        return sb.toString();
    }

    private static String toMessage(Throwable t, String source) {
        String m = t == null ? null : t.getMessage();
        return toMessage(m, source);
    }

    private static String toMessage(Collection<JavaSource> sources) {
        StringJoiner sj = new StringJoiner("\n");
        for(JavaSource s : sources) {
            sj.add(s.toString());
        }
        return  sj.toString();
    }


}
