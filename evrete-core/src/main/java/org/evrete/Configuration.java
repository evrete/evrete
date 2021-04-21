package org.evrete;

import org.evrete.api.Copyable;
import org.evrete.api.FluentImports;
import org.evrete.api.Imports;
import org.evrete.api.RuleScope;

import java.util.Properties;
import java.util.logging.Logger;

public class Configuration extends Properties implements Copyable<Configuration>, FluentImports<Configuration> {
    public static final String OBJECT_COMPARE_METHOD = "evrete.core.fact-identity-strategy";
    public static final String INSERT_BUFFER_SIZE = "evrete.core.insert-buffer-size";
    public static final String WARN_UNKNOWN_TYPES = "evrete.core.warn-unknown-types";
    public static final int INSERT_BUFFER_SIZE_DEFAULT = 4096;
    static final String PARALLELISM = "evrete.core.parallelism";
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    public static final String IDENTITY_METHOD_EQUALS = "equals";
    public static final String IDENTITY_METHOD_IDENTITY = "identity";
    private static final long serialVersionUID = -9015471049604658637L;
    private final Imports imports;

    @SuppressWarnings("unused")
    public Configuration(Properties defaults) {
        for (String key : defaults.stringPropertyNames()) {
            setProperty(key, defaults.getProperty(key));
        }
        this.imports = new Imports();
    }

    private Configuration(Properties defaults, Imports imports) {
        for (String key : defaults.stringPropertyNames()) {
            setProperty(key, defaults.getProperty(key));
        }
        this.imports = imports.copyOf();
    }

    public Configuration() {
        setProperty(WARN_UNKNOWN_TYPES, Boolean.TRUE.toString());
        setProperty(OBJECT_COMPARE_METHOD, IDENTITY_METHOD_IDENTITY);
        setProperty(INSERT_BUFFER_SIZE, String.valueOf(INSERT_BUFFER_SIZE_DEFAULT));
        this.imports = new Imports();
    }

    public boolean getAsBoolean(String property) {
        return Boolean.parseBoolean(getProperty(property));
    }

    public int getAsInteger(String property, int defaultValue) {
        String val = getProperty(property);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            LOGGER.warning("Property '" + property + "' is not an integer, returning default value of " + defaultValue);
            return defaultValue;
        }
    }

    @Override
    public Imports getImports() {
        return imports;
    }

    public final Configuration addImport(RuleScope scope, String imp) {
        this.imports.add(scope, imp);
        return this;
    }

    @Override
    public Configuration copyOf() {
        return new Configuration(this, this.imports);
    }
}
