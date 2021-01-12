package org.evrete.showcase.abs.town;

import org.evrete.api.ActivationManager;
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
    private World world;
    private Viewport viewport;
    private WorldTime worldTime;
    //private TransitionManager transitionManager;
    private int intervalSeconds;
    private SessionThread thread;
    private UiWriter uiWriter;

    public TownSessionWrapper(Session session) {
        super(session);
    }


    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
        if (uiWriter != null) {
            // Session started
            uiWriter.setViewport(viewport);
        }
    }

    void start(String configXml, int interval) {
        // Reading config
        //this.transitionManager = new TransitionManager(configXml, interval);
        this.intervalSeconds = interval;
        this.sessionGate.set(true);

        // Building knowledge
        StatefulSession session = newKnowledgeSession();
        setKnowledgeSession(session);

        this.world = new World(AppContext.MAP_DATA, 0.001f);
        this.worldTime = new WorldTime();
        this.uiWriter = new UiWriter(getMessenger(), world, worldTime, viewport);

        getMessenger().sendUnchecked(new Message("LOG", String.format("Data initialized. Residents: %d, Homes: %d, Businesses: %d", this.world.population.size(), this.world.homes.size(), this.world.businesses.size())));


        //session.insert(this.world.population);

        Entity single = new Entity("person");
        Entity home = new Entity("home");
        Entity work = new Entity("work");

        single.set("work", work);
        single.set("home", home);
        single.set("location", home);
        single.set("wakeup", 20000);
        single.set("active", false);


        session.insert(single);
        session.insert(this.worldTime);
        session.insert(this.world);
        this.thread = new SessionThread(session);
        this.thread.start();
    }

    private StatefulSession newKnowledgeSession() {
        StatefulSession session = AppContext.knowledge().createSession();

        session.setActivationManager(new ActivationManager() {
            @Override
            public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {
                TownSessionWrapper.this.uiWriter.writeState();

                //System.out.println("SEQ: " + sequenceId + " : " + agenda);
                Utils.delay(10);
            }

            @Override
            public boolean test(RuntimeRule rule) {
                return worldTime.absoluteTimeSeconds() - worldTime.getInitialTimeSeconds() < 3600 * 24 * 7; //One week
                //return worldTime.absoluteTimeSeconds() - worldTime.getInitialTimeSeconds() < 360;
            }

        });

        return session;

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

    public void stop() {
        closeSession();
    }

    class SessionThread extends Thread {
        private final StatefulSession session;

        public SessionThread(StatefulSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            // Initial fire
            session.fire();

            int tmp = 0;
            while (sessionGate.get() && worldTime.absoluteTimeSeconds() - worldTime.getInitialTimeSeconds() < 3600 * 24 * 7) {
                session.update(worldTime.increment(intervalSeconds));
                session.fire();
                if (tmp++ > 40) {
                    break;
                }
                System.out.println("updating time.....");
            }


            getMessenger().sendUnchecked(new Message("LOG", "Session ended"));
            getMessenger().sendUnchecked(new Message("END"));
        }
    }


}
