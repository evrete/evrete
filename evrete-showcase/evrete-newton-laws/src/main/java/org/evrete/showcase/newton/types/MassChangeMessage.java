package org.evrete.showcase.newton.types;

import org.evrete.showcase.shared.JsonMessage;

public class MassChangeMessage extends JsonMessage {
    public String object;
    public double mass;

    public MassChangeMessage() {
        super("MASS_CHANGE");
    }
}
