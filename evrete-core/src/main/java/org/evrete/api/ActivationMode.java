package org.evrete.api;

public enum ActivationMode {
    CONTINUOUS,
    DEFAULT,
    /**
     * @deprecated  Activation mode that was 'DEFAULT' for versions prior to 2.2.00.
     */
    @Deprecated
    DEFAULT_OLD
}
