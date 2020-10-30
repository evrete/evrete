package org.evrete.showcase.chess;

import org.evrete.showcase.chess.json.BoardMessage;
import org.evrete.showcase.chess.json.QueenToggleMessage;
import org.evrete.showcase.shared.JsonMessage;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.shared.Utils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/socket")
public class ChessSocketEndpoint {
    private final Map<Session, ChessSessionWrapper> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        ChessSessionWrapper wrapper = new ChessSessionWrapper(session);
        sessionMap.put(session, wrapper);
        wrapper.getMessenger().sendUnchecked(new BoardMessage(wrapper.getChessBoard()));
    }

    @OnClose
    public void onClose(Session session) {
        ChessSessionWrapper wrapper = sessionMap.remove(session);
        if (wrapper != null) {
            wrapper.closeSession();
        }
    }

    @OnMessage
    public void processMessage(String message, Session session) {
        ChessSessionWrapper sessionWrapper = sessionMap.get(session);
        try {
            process(message, sessionWrapper);
        } catch (Exception e) {
            e.printStackTrace();
            sessionWrapper.getMessenger().send(e);
            stopSession(session);
        }
    }

    private void process(String message, ChessSessionWrapper sessionWrapper) throws Exception {
        SocketMessenger
                messenger = sessionWrapper.getMessenger();
        JsonMessage m = Utils.fromJson(message, JsonMessage.class);
        switch (m.getType()) {
            case "PING":
                messenger.send(new Message("PONG"));
                break;
            case "RESET":
                Message resetMessage = Utils.fromJson(message, Message.class);
                int size = Integer.parseInt(resetMessage.text);
                messenger.sendUnchecked(new BoardMessage(sessionWrapper.reset(size)));
                break;
            case "TOGGLE":
                QueenToggleMessage toggleMessage = Utils.fromJson(message, QueenToggleMessage.class);
                messenger.sendUnchecked(new BoardMessage(sessionWrapper.toggle(toggleMessage)));
                break;
            case "RUN":
                sessionWrapper.run();
                break;
            case "STOP":
                sessionWrapper.closeSession();
                break;
            default:
                messenger.send(Message.error("Unknown command " + m.getType()));
        }
    }

    @OnError
    @SuppressWarnings("unused")
    public void onError(Session session, Throwable t) {
        onClose(session);
    }

    private void stopSession(Session session) {
        ChessSessionWrapper wrapper = sessionMap.get(session);
        if (wrapper != null) {
            wrapper.closeSession();
        }
    }
}
