package org.evrete.showcase.shared;

public class JsonMessage {
    static final String TYPE_ERROR = "ERROR";
    final String type;

    public JsonMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
