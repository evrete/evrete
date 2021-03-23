package org.evrete.showcase.shared;

import javax.websocket.Session;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketMessenger {
    private static final Logger LOGGER = Logger.getLogger(SocketMessenger.class.getName());
    private final Session session;
    private int delay;

    public SocketMessenger(Session session) {
        this.session = session;
    }

    private void send(String text) throws IOException {
        if (session.isOpen()) {
            synchronized (session) {
                session.getBasicRemote().sendText(text);
            }
        }
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public <T extends JsonMessage> void send(T message) throws IOException {
        send(Utils.toJson(message));
    }

    public <T extends JsonMessage> void sendUnchecked(T message) {
        try {
            send(message);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    public <T extends JsonMessage> void sendDelayed(T message) throws IOException {
        Utils.delay(delay);
        send(message);
    }

    public void send(Throwable e) {
        try {
            send(Message.error(e.getMessage()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
