package org.evrete;

import org.evrete.api.Copyable;
import org.evrete.api.FluentImports;
import org.evrete.api.Imports;

import java.util.Properties;
import java.util.logging.Logger;

public class Configuration extends Properties implements Copyable<Configuration>, FluentImports<Configuration> {
    public static final String OBJECT_COMPARE_METHOD = "evrete.core.fact-identity-strategy";
    public static final String INSERT_BUFFER_SIZE = "evrete.core.insert-buffer-size";
    public static final String WARN_UNKNOWN_TYPES = "evrete.core.warn-unknown-types";
    public static final int INSERT_BUFFER_SIZE_DEFAULT = 4096;
    public static final String IDENTITY_METHOD_EQUALS = "equals";
    public static final String IDENTITY_METHOD_IDENTITY = "identity";
    static final String SPI_MEMORY_FACTORY = "evrete.spi.memory-factory";
    static final String SPI_EXPRESSION_RESOLVER = "evrete.spi.expression-resolver";
    static final String SPI_TYPE_RESOLVER = "evrete.spi.type-resolver";
    static final String SPI_RHS_COMPILER = "evrete.spi.rhs-compiler";
    static final String SPI_SOURCE_COMPILER = "evrete.spi.source-compiler";
    static final String PARALLELISM = "evrete.core.parallelism";
    public static final String CONDITION_BASE_CLASS = "evrete.impl.condition-base-class";

    //TODO document
    public static final String RULE_BASE_CLASS = "evrete.impl.rule-base-class";

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
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
        super(System.getProperties());

        setIfAbsent(WARN_UNKNOWN_TYPES, Boolean.TRUE.toString());
        setIfAbsent(OBJECT_COMPARE_METHOD, IDENTITY_METHOD_IDENTITY);
        setIfAbsent(INSERT_BUFFER_SIZE, String.valueOf(INSERT_BUFFER_SIZE_DEFAULT));
        this.imports = new Imports();
    }

    private void setIfAbsent(String key, String value) {
        if (!contains(key)) {
            setProperty(key, value);
        }
    }

    public boolean getAsBoolean(String property) {
        return Boolean.parseBoolean(getProperty(property));
    }

    public boolean getAsBoolean(String property, boolean defaultValue) {
        String prop = getProperty(property, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(prop);
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

    public final Configuration addImport(String imp) {
        this.imports.add(imp);
        return this;
    }

    @Override
    public Configuration copyOf() {
        return new Configuration(this, this.imports);
    }
}
