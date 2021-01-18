package org.evrete.showcase.abs.town;

import org.evrete.showcase.abs.town.json.StartMessage;
import org.evrete.showcase.abs.town.json.Viewport;
import org.evrete.showcase.shared.JsonMessage;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.shared.Utils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/socket")
public class TownEmulationEndpoint {
    private final Map<Session, TownSessionWrapper> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        TownSessionWrapper wrapper = new TownSessionWrapper(session);
        sessionMap.put(session, wrapper);

/*
        wrapper.getMessenger().sendUnchecked(new ConfigMessage(AppContext.DEFAULT_XML));




        // DELETE BELOW
        List<XYPoint> buffer = new LinkedList<>();

        for (XYPoint home : AppContext.MAP_DATA.homes) {
            buffer.add(home);
            if (buffer.size() == 64) {
                wrapper.getMessenger().sendUnchecked(new MapMessage("residential", buffer));
                buffer.clear();
            }
        }
        if (buffer.size() > 0) {
            wrapper.getMessenger().sendUnchecked(new MapMessage("residential", buffer));
            buffer.clear();
        }

        for (XYPoint home : AppContext.MAP_DATA.businesses) {
            buffer.add(home);
            if (buffer.size() == 64) {
                wrapper.getMessenger().sendUnchecked(new MapMessage("businesses", buffer));
                buffer.clear();
            }
        }
        if (buffer.size() > 0) {
            wrapper.getMessenger().sendUnchecked(new MapMessage("businesses", buffer));
            buffer.clear();
        }
*/

    }

    @OnClose
    public void onClose(Session session) {
        TownSessionWrapper wrapper = sessionMap.remove(session);
        if (wrapper != null) {
            wrapper.closeSession();
        }
    }

    @OnMessage
    public void processMessage(String message, Session session) {
        TownSessionWrapper sessionWrapper = sessionMap.get(session);
        try {
            process(message, sessionWrapper);
        } catch (Exception e) {
            e.printStackTrace();
            sessionWrapper.getMessenger().send(e);
            stopSession(session);
        }
    }

    private void process(String message, TownSessionWrapper sessionWrapper) throws Exception {
        SocketMessenger
                messenger = sessionWrapper.getMessenger();
        JsonMessage m = Utils.fromJson(message, JsonMessage.class);
        switch (m.getType()) {
            case "PING":
                messenger.send(new Message("PONG"));
                break;
            case "VIEWPORT":
                sessionWrapper.setViewport(Utils.fromJson(message, Viewport.class));
                break;
            case "START":
                StartMessage startMessage = Utils.fromJson(message, StartMessage.class);
                sessionWrapper.start(startMessage.config, Math.max(startMessage.interval, 1));
                break;
            case "STOP":
                sessionWrapper.stop();
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
        TownSessionWrapper wrapper = sessionMap.get(session);
        if (wrapper != null) {
            wrapper.closeSession();
        }
    }
}
