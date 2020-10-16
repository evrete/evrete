package org.evrete.showcase.stock.json;

public class Message extends JsonMessage {
    public String text;

    public Message(String type, String text) {
        super(type);
        this.text = text;
    }

    public Message(String type) {
        this(type, null);
    }

    public static Message error(String txt) {
        return new Message(TYPE_ERROR, txt);
    }
}
