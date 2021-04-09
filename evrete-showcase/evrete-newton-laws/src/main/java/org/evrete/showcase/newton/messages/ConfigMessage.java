package org.evrete.showcase.newton.messages;

import org.evrete.showcase.shared.JsonMessage;

public class ConfigMessage extends JsonMessage {
    public String rules;
    public String presets;

    public ConfigMessage(String rules, String presets) {
        super("CONFIG");
        this.rules = rules;
        this.presets = presets;
    }
}
