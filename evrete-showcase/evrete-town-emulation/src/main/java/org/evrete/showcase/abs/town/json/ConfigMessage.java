package org.evrete.showcase.abs.town.json;

import org.evrete.showcase.shared.JsonMessage;

public class ConfigMessage extends JsonMessage {
    String xml;

    public ConfigMessage(String xml) {
        super("CONFIG");
        this.xml = xml;
    }
}
