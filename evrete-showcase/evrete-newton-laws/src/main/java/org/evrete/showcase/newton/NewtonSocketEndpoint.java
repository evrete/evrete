package org.evrete.showcase.newton;

import org.evrete.showcase.newton.messages.ConfigMessage;
import org.evrete.showcase.shared.JsonMessage;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.shared.Utils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/socket")
public class NewtonSocketEndpoint {
    private final Map<Session, NewtonSessionWrapper> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        NewtonSessionWrapper wrapper = new NewtonSessionWrapper(session);
        sessionMap.put(session, wrapper);

        // On new session we provide web client with default rules and stock price history
        SocketMessenger messenger = wrapper.getMessenger();
        try {
            messenger.send(new ConfigMessage(
                    AppContext.DEFAULT_SOURCE,
                    AppContext.DEFAULT_PRESETS
            ));
        } catch (Exception e) {
            stopSession(session);
        }
    }

    @OnClose
    public void onClose(Session session) {
        NewtonSessionWrapper wrapper = sessionMap.remove(session);
        if (wrapper != null) {
            wrapper.closeSession();
        }
    }

    private static void process(String message, NewtonSessionWrapper sessionWrapper) throws Exception {
        SocketMessenger
                messenger = sessionWrapper.getMessenger();
        JsonMessage m = Utils.fromJson(message, JsonMessage.class);
        switch (m.getType()) {
            case "PING":
                messenger.send(new Message("PONG"));
                break;
            case "START":
                //StartMessage startMessage = Utils.fromJson(message, StartMessage.class);
                sessionWrapper.initSession();
                break;
            case "STOP":
                System.out.println("Stopping.......");
                sessionWrapper.closeSession();
                break;
            case "GRAVITY_CONSTANT":
                Message gravityChange = Utils.fromJson(message, Message.class);
                double gravity = Double.parseDouble(gravityChange.text);
                sessionWrapper.updateGravity(gravity);
                break;
            default:
                messenger.send(Message.error("Unknown command " + m.getType()));
        }
    }

    @OnMessage
    public void processMessage(String message, Session session) {
        NewtonSessionWrapper sessionWrapper = sessionMap.get(session);
        try {
            process(message, sessionWrapper);
        } catch (Exception e) {
            e.printStackTrace();
            sessionWrapper.getMessenger().send(e);
            stopSession(session);
        }
    }

    @OnError
    @SuppressWarnings("unused")
    public void onError(Session session, Throwable t) {
        onClose(session);
    }

    private void stopSession(Session session) {
        NewtonSessionWrapper wrapper = sessionMap.get(session);
        if (wrapper != null) {
            wrapper.closeSession();
        }
    }
}
