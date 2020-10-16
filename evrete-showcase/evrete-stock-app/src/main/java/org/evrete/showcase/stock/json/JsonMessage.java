package org.evrete.showcase.stock.json;

public class JsonMessage {
    static final String TYPE_ERROR = "ERROR";
    static final String TYPE_OHLC = "OHLC";
    static final String TYPE_RUN = "RUN_COMMAND";
    final String type;

    public JsonMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
