package org.evrete;

import java.util.Properties;

public class Configuration extends Properties {
    public static final String OBJECT_COMPARE_METHOD = "evrete.core.facts-comparison";
    public static final String WARN_UNKNOWN_TYPES = "evrete.core.warn-unknown-types";
    public static final String IDENTITY_METHOD_EQUALS = "equals";
    public static final String IDENTITY_METHOD_IDENTITY = "identity";
    private static final long serialVersionUID = -9015471049604658637L;

    @SuppressWarnings("unused")
    public Configuration(Properties defaults) {
        super(defaults);
    }

    public Configuration() {
        super();
        setProperty(WARN_UNKNOWN_TYPES, Boolean.TRUE.toString());
    }

    public boolean getAsBoolean(String property) {
        return Boolean.parseBoolean(getProperty(property));
    }

}
