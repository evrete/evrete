package org.evrete.showcase.newton.messages;

import org.evrete.showcase.newton.model.Particle;
import org.evrete.showcase.shared.JsonMessage;

import java.util.Map;

public class StartMessage extends JsonMessage {
    public Map<String, Particle> particles;
    public String rules;
    public double gravity;

    public StartMessage() {
        super("START");
    }

    @Override
    public String toString() {
        return "{type=" + getType() +
                ", particles=" + particles +
                ", rules=" + rules +
                '}';
    }
}
