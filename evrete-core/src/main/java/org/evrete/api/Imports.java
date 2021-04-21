package org.evrete.api;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Imports implements Copyable<Imports>, Serializable {
    private static final long serialVersionUID = -6504055142637422799L;
    private final EnumMap<RuleScope, Set<String>> imports = new EnumMap<>(RuleScope.class);

    public Imports() {
        for (RuleScope scope : RuleScope.values()) {
            this.imports.put(scope, new HashSet<>());
        }
    }

    private Imports(Imports parent) {
        this();
        append(parent);
    }

    public void append(Imports parent) {
        for (Map.Entry<RuleScope, Set<String>> entry : parent.imports.entrySet()) {
            this.imports.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    public void add(RuleScope scope, String imp) {
        String s;
        if (imp == null || (s = imp.trim()).isEmpty()) {
            return;
        }

        s = s.replaceAll(";", "");
        s = s.replaceAll("\\s{2,}]", " ");

        this.imports.get(scope).add(s);
    }

    public Set<String> get(RuleScope... scopes) {
        Set<String> set = new HashSet<>();
        if (scopes != null) {
            for (RuleScope scope : scopes) {
                set.addAll(this.imports.get(scope));
            }
        }
        return set;
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
