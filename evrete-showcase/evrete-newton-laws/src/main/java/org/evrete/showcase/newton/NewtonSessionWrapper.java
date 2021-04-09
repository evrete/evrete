package org.evrete.showcase.newton;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationManager;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.showcase.newton.messages.StartMessage;
import org.evrete.showcase.newton.model.Particle;
import org.evrete.showcase.newton.model.SpaceTime;
import org.evrete.showcase.newton.rules.MainRuleset;
import org.evrete.showcase.shared.AbstractSocketSession;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.shared.Utils;

import javax.websocket.Session;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class NewtonSessionWrapper extends AbstractSocketSession {
    private final AtomicBoolean gate = new AtomicBoolean(true);
    private final Map<String, Particle> initialData = new HashMap<>();
    private CountDownLatch latch = new CountDownLatch(0);
    private SessionThread thread;

    NewtonSessionWrapper(Session session) {
        super(session);
    }

    private synchronized void pause(boolean flag) {
        if (flag) {
            this.latch = new CountDownLatch(1);
        } else {
            this.latch.countDown();
        }
    }

    private boolean isPaused() {
        return this.latch.getCount() > 0;
    }

    void togglePause() {
        pause(!isPaused());
        Message msg;
        if (isPaused()) {
            msg = new Message("PAUSED");
        } else {
            msg = new Message("STARTED");
        }
        getMessenger().sendUnchecked(msg);
    }

    void updateGravity(double d) {
        StatefulSession session = getKnowledgeSession();
        if (session != null) {
            session.set("G", d);
        }
    }

    void updateMass(String objectId, double mass) {
        StatefulSession session = getKnowledgeSession();
        if (session != null) {
            Particle p = initialData.get(objectId);
            if (p != null) {
                p.mass = mass;
            }
        }
    }

    private CountDownLatch getLatch() {
        return latch;
    }

    synchronized void initSession(StartMessage msg) throws Exception {
        if (msg.particles.size() == 0) {
            throw new Exception("No objects provided");
        }
        if (thread != null && thread.isAlive()) {
            throw new Exception("Session already started");
        }
        // Close previous session if active
        closeSession();

        // Compile knowledge session
        Knowledge knowledge = buildKnowledge(msg);
        StatefulSession session = knowledge
                .createSession()
                .setFireCriteria(gate::get)
                .set("G", msg.gravity)
                .set("time-step", 0.000002);

        super.setKnowledgeSession(session);

        this.initialData.clear();
        this.initialData.putAll(msg.particles);

        this.gate.set(true);

        session.setActivationManager(new ReportingActivationManager(session, getMessenger()));

        this.thread = new SessionThread(session, getMessenger(), initialData.values());
        thread.start();
    }

    private Knowledge buildKnowledge(StartMessage msg) throws Exception {
        //List<LiteralRule> rules = LiteralRule.parse(msg.rules);
        KnowledgeService service = AppContext.knowledgeService();
        Knowledge knowledge = service.newKnowledge().appendDslRules("JAVA-CLASS", MainRuleset.class);
        for (RuleDescriptor rule : knowledge.getRules()) {
            getMessenger().send(new Message(
                    "LOG",
                    "Rule compiled: " + rule.getName()
            ));
        }

        return knowledge;
    }

    @Override
    public boolean closeSession() {
        this.gate.set(false);
        this.pause(false); // Unlock latches if any
        this.initialData.clear();
        return super.closeSession();
    }

    public static class SessionThread extends Thread {
        final SocketMessenger messenger;
        private final Collection<Particle> initialData;
        private final StatefulSession session;


        SessionThread(StatefulSession session, SocketMessenger messenger, Collection<Particle> initialData) {
            this.initialData = initialData;
            this.session = session;
            this.messenger = messenger;
        }

        @Override
        public void run() {
            try {
                run1();
            } catch (Exception e) {
                messenger.send(e);
            } finally {
                messenger.sendUnchecked(new Message("STOPPED"));
            }
        }

        private void run1() throws Exception {
            session.insert(initialData);
            SpaceTime space = new SpaceTime();
            session.insert(space);
            messenger.send(new Message("STARTED"));
            session.fire();
        }
    }

    private class ReportingActivationManager implements ActivationManager {
        private final StatefulSession session;
        private final SocketMessenger messenger;
        private static final int fps = 20;
        private final AtomicLong lastReported = new AtomicLong(System.currentTimeMillis());

        ReportingActivationManager(StatefulSession session, SocketMessenger messenger) {
            this.session = session;
            this.messenger = messenger;
        }

        @Override
        public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {

            try {
                getLatch().await();

                long now = System.currentTimeMillis();
                if (now - lastReported.get() > (1000 / fps)) {
                    Utils.delay(5);

                    // Get current particles
                    Map<String, Particle> particles = new HashMap<>();

                    session.forEachFact((handle, o) -> {
                        if (o instanceof Particle) {
                            Particle p = (Particle) o;
                            particles.put(String.valueOf(p.id), p);
                        }
                    });

                    if (particles.size() == 1) {
                        messenger.send(new Message(
                                "LOG",
                                "One particle left, simulation ends"
                        ));
                        gate.set(false);
                    }

                    getMessenger().send(new Message(
                            "REPORT",
                            Utils.toJson(particles)
                    ));
                    lastReported.set(System.currentTimeMillis());
                }

            } catch (Exception e) {
                closeSession();
            }
        }
    }
}
