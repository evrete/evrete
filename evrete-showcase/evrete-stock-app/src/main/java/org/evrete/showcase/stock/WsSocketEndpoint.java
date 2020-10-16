package org.evrete.showcase.stock;

import org.evrete.showcase.stock.json.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/socket")
public class WsSocketEndpoint {
    private final Map<Session, WsSessionWrapper> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        WsSessionWrapper wrapper = new WsSessionWrapper(session);
        sessionMap.put(session, wrapper);

        // On new session we provide web client with default rules and stock price history
        WsMessenger messenger = wrapper.getMessenger();
        try {
            messenger.send(new ConfigMessage(
                    AppContext.DEFAULT_SOURCE,
                    AppContext.DEFAULT_STOCK_HISTORY
            ));

            messenger.send(new Message(
                    "LOG",
                    "Maximum entries: " + WsSessionWrapper.MAX_DATA_SIZE
            ));
        } catch (Exception e) {
            closeSession(session);
        }
    }

    @OnClose
    public void onClose(Session session) {
        closeSession(session);
    }

    @OnMessage
    public void processMessage(String message, Session session) {
        WsSessionWrapper sessionWrapper = sessionMap.get(session);
        try {
            process(message, sessionWrapper);
        } catch (RuntimeException e) {
            try {
                e.printStackTrace();
                sessionWrapper.getMessenger().send(e);
            } catch (IOException io) {
                io.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void process(String message, WsSessionWrapper sessionWrapper) throws Exception {
        WsMessenger messenger = sessionWrapper.getMessenger();
        JsonMessage m = Utils.fromJson(message, JsonMessage.class);
        switch (m.getType()) {
            case "PING":
                messenger.send(new Message("PONG"));
                break;
            case "RUN_COMMAND":
                RunMessage msg = Utils.fromJson(message, RunMessage.class);
                sessionWrapper.initSession(msg);
                break;
            case "STOP":
                sessionWrapper.closeSession();
                messenger.send(new Message("STOPPED"));
                break;
            case "OHLC":
                if (!sessionWrapper.insert(Utils.fromJson(message, OHLCMessage.class).ohlc)) {
                    sessionWrapper.closeSession();
                    messenger.send(new Message("STOPPED"));
                }
                break;
            default:
                messenger.send(Message.error("Unknown command " + m.getType()));
        }
    }

    @OnError
    @SuppressWarnings("unused")
    public void onError(Session session, Throwable t) {
        closeSession(session);
    }

    private void closeSession(Session session) {
        WsSessionWrapper wrapper = sessionMap.remove(session);
        if (wrapper != null) {
            wrapper.closeSession();
        }
    }
}
