package org.evrete.showcase.abs.town;

import org.evrete.api.ActivationManager;
import org.evrete.api.ActivationMode;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.showcase.abs.town.json.Viewport;
import org.evrete.showcase.abs.town.types.Entity;
import org.evrete.showcase.abs.town.types.World;
import org.evrete.showcase.abs.town.types.WorldTime;
import org.evrete.showcase.shared.AbstractSocketSession;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.Utils;

import javax.websocket.Session;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class TownSessionWrapper extends AbstractSocketSession {
    private final AtomicBoolean sessionGate = new AtomicBoolean(true);
    private Viewport viewport;
    private SessionThread thread;
    private UiMap uiWriter;

    TownSessionWrapper(Session session) {
        super(session);
    }


    void setViewport(Viewport viewport) {
        this.viewport = viewport;
        if (uiWriter != null) {
            // Session started
            uiWriter.setViewport(viewport);
        }
    }

    void start(String configXml, int interval) {
        // Reading config
        this.sessionGate.set(true);

        // Building knowledge
        StatefulSession session = AppContext.knowledge().createSession().setActivationMode(ActivationMode.CONTINUOUS);
        session.setActivationManager(new ActivationManager() {
            @Override
            public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {
                if (sequenceId > 100) throw new IllegalStateException("Rules result in infinite loop");
            }
        });

        setKnowledgeSession(session);
        WorldTime worldTime = new WorldTime();

        World saved = World.readFromFile();
        World world;
        if (saved != null) {
            world = saved;
            System.out.println("Using a saved world.....");
        } else {
            world = World.factory(AppContext.MAP_DATA, 1.0f, 0.75f, 0.75);
        }
        this.uiWriter = new UiMap(world, worldTime, viewport);

        // Put the initial allocation on the map
        for (Entity person : world.population) {
            person.set("current_location", person.getProperty("home"));
        }

        getMessenger().sendUnchecked(new Message("LOG", String.format("Data initialized. Residents: %d, Homes: %d, Businesses: %d", world.population.size(), world.homes.size(), world.businesses.size())));

        session.insert(world.population);
        session.insert(worldTime);
        session.insert(world);
        this.thread = new SessionThread(session, interval, getMessenger(), world, worldTime, uiWriter, sessionGate::get);
        this.thread.start();
    }

    @Override
    public boolean closeSession() {
        this.sessionGate.set(false);
        while (thread != null && thread.isAlive()) {
            Utils.delay(100);
        }
        thread = null;
        return super.closeSession();
    }

    void stop() {
        closeSession();
    }
}
