package org.evrete.showcase.stock.json;

import org.evrete.showcase.shared.JsonMessage;

public class RunMessage extends JsonMessage {
    static final String TYPE_RUN = "RUN_COMMAND";

    public String rules;
    public int delay;

    public RunMessage() {
        super(TYPE_RUN);
    }
}
