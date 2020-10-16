package org.evrete.showcase.stock.json;

public class RunMessage extends JsonMessage {
    public String rules;
    public int delay;

    public RunMessage() {
        super(TYPE_RUN);
    }
}
