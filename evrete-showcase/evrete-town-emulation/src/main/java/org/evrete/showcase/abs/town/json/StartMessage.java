package org.evrete.showcase.abs.town.json;

import org.evrete.showcase.shared.JsonMessage;

public class StartMessage extends JsonMessage {
    public String config;

    public int interval = 10;

    public StartMessage() {
        super("START");
    }
}
