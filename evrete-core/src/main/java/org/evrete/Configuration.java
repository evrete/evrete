package org.evrete;

import org.evrete.api.Copyable;
import org.evrete.api.FluentImports;
import org.evrete.api.Imports;

import java.util.*;
import java.util.logging.Logger;

/**
 * <p>
 * Configuration class represents a configuration object that extends the Properties class and
 * implements the {@link Copyable} and {@link FluentImports} interfaces.
 * </p>
 * <p>
 * Initial configuration is supplied to the {@link KnowledgeService} and then copied down to instances of {@link org.evrete.api.Knowledge} and {@link org.evrete.api.RuleSession}.
 * </p>
 * <p>
 * This way, changes in the configuration of a {@link org.evrete.api.Knowledge} instance will be passed down to every {@link org.evrete.api.RuleSession} spawned from that instance after the change. However, changes in the configuration of a {@link org.evrete.api.RuleSession} will only be available to that session.
 * </p>
 */
public class Configuration extends Properties implements Copyable<Configuration>, FluentImports<Configuration> {
    public static final String OBJECT_COMPARE_METHOD = "evrete.core.fact-identity-strategy";
    public static final String INSERT_BUFFER_SIZE = "evrete.core.insert-buffer-size";
    public static final String WARN_UNKNOWN_TYPES = "evrete.core.warn-unknown-types";
    public static final boolean WARN_UNKNOWN_TYPES_DEFAULT = true;
    public static final String DAEMON_INNER_THREADS = "evrete.core.daemon-threads";
    public static final boolean DAEMON_INNER_THREADS_DEFAULT = true;
    public static final String IDENTITY_METHOD_EQUALS = "equals";
    public static final String IDENTITY_METHOD_IDENTITY = "identity";
    static final String SPI_MEMORY_FACTORY = "evrete.spi.memory-factory";
    public static final String SPI_EXPRESSION_RESOLVER = "evrete.spi.expression-resolver";
    static final String SPI_TYPE_RESOLVER = "evrete.spi.type-resolver";
    static final String SPI_SOURCE_COMPILER = "evrete.spi.source-compiler";
    static final String PARALLELISM = "evrete.core.parallelism";
    public static final String RULE_BASE_CLASS = "evrete.impl.rule-base-class";
    public static final String SPI_LHS_STRIP_WHITESPACES = "evrete.spi.compiler.lhs-strip-whitespaces";


    private static final Set<String> OBSOLETE_PROPERTIES = new HashSet<>(Arrays.asList(
            IDENTITY_METHOD_EQUALS,
            SPI_EXPRESSION_RESOLVER,
            IDENTITY_METHOD_IDENTITY,
            OBJECT_COMPARE_METHOD,
            INSERT_BUFFER_SIZE
    ));


    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private static final long serialVersionUID = -9015471049604658637L;
    private final Imports imports;

    @SuppressWarnings("unused")
    public Configuration(Properties defaults) {
        this(defaults, new Imports());
    }

    private Configuration(Properties defaults, Imports imports) {
        super(defaults);
        this.imports = imports;
    }

    public Configuration() {
        this(System.getProperties());
    }

    public Configuration set(String key, String value) {
        this.setProperty(key, value);
        return this;
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        if (OBSOLETE_PROPERTIES.contains(key)) {
            LOGGER.warning(()->"Property '" + key + "' is obsolete and will be ignored");
        }
        return super.setProperty(key, value);
    }

    /**
     * Retrieves the value of a property as a boolean.
     *
     * @param property the name of the property
     * @return the value of the property as a boolean
     */
    public boolean getAsBoolean(String property) {
        return Boolean.parseBoolean(getProperty(property));
    }

    /**
     * Retrieves the value of a property as a boolean. If the property is not found,
     * it returns the default value provided.
     *
     * @param property     the name of the property
     * @param defaultValue the default value to be returned if the property is not found
     * @return the value of the property as a boolean, or the default value if the property is not found
     */
    @SuppressWarnings("unused")
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
            LOGGER.warning(()->"Property '" + property + "' is not an integer, returning default value of " + defaultValue);
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
