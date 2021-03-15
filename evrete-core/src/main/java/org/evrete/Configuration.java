package org.evrete;

import org.evrete.api.Copyable;

import java.util.Properties;
import java.util.logging.Logger;

public class Configuration extends Properties implements Copyable<Configuration> {
    public static final String PARALLELISM = "evrete.core.parallelism";
    public static final String OBJECT_COMPARE_METHOD = "evrete.core.facts-comparison";
    public static final String WARN_UNKNOWN_TYPES = "evrete.core.warn-unknown-types";
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    public static final String IDENTITY_METHOD_EQUALS = "equals";
    public static final String IDENTITY_METHOD_IDENTITY = "identity";
    private static final long serialVersionUID = -9015471049604658637L;

    @SuppressWarnings("unused")
    public Configuration(Properties defaults) {
        this();
        for (String key : defaults.stringPropertyNames()) {
            setProperty(key, defaults.getProperty(key));
        }
    }

    public Configuration() {
        setProperty(WARN_UNKNOWN_TYPES, Boolean.TRUE.toString());
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
    public Configuration copyOf() {
        return new Configuration(this);
    }
}
