package org.evrete;

import java.util.Properties;

public class Configuration extends Properties{
    private static final boolean DEFAULT_WARN_UNKNOWN_TYPES = true;
    private boolean warnUnknownTypes = DEFAULT_WARN_UNKNOWN_TYPES;

    @SuppressWarnings("unused")
    public Configuration(Properties defaults) {
        super(defaults);
    }

    public Configuration() {
        super();
    }

    public boolean isWarnUnknownTypes() {
        return warnUnknownTypes;
    }

    public Configuration setWarnUnknownTypes(boolean warnUnknownTypes) {
        this.warnUnknownTypes = warnUnknownTypes;
        return this;
    }
}
