package org.evrete.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a class for managing Java imports.
 */
public class Imports implements Copyable<Imports>, Serializable {
    private static final long serialVersionUID = -6504055142637422799L;
    private final Set<String> imports = new HashSet<>();

    public Imports() {
    }

    private Imports(Imports parent) {
        append(parent);
    }

    public void append(Imports parent) {
        this.imports.addAll(parent.imports);
    }

    public void add(String imp) {
        String s;
        if (imp == null || (s = imp.trim()).isEmpty()) {
            return;
        }

        s = s.replaceAll(";", "");
        s = s.replaceAll("\\s{2,}]", " ");

        this.imports.add(s);
    }

    public Set<String> get() {
        return Collections.unmodifiableSet(imports);
    }


    public void asJavaImportStatements(StringBuilder destination) {
        String sep = System.lineSeparator();
        if (!imports.isEmpty()) {
            for (String imp : imports) {
                destination.append("import ").append(imp).append(";").append(sep);
            }
            destination.append(sep);
        }
    }

    @Override
    public Imports copyOf() {
        return new Imports(this);
    }

    @Override
    public String toString() {
        return "Imports{" +
                "imports=" + imports +
                '}';
    }
}
