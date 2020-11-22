package org.evrete.showcase.newton;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.showcase.newton.types.Particle;
import org.evrete.showcase.newton.types.SpaceTime;
import org.evrete.showcase.newton.types.StartMessage;
import org.evrete.showcase.newton.types.Vector;
import org.evrete.showcase.shared.*;

import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class NewtonSessionWrapper extends AbstractSocketSession {
    private final AtomicBoolean gate = new AtomicBoolean(true);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, Particle> initialData = new HashMap<>();
    private CountDownLatch latch = new CountDownLatch(0);


    public NewtonSessionWrapper(Session session) {
        super(session);
    }

    synchronized void pause(boolean flag) {
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
        if (running.get()) {
            throw new Exception("Session already started");
        }
        // Close previous session if active
        closeSession();

        // Compile knowledge session
        Knowledge knowledge = buildKnowledge(msg);
        System.out.println("Gravity " + msg.gravity);
        StatefulSession knowledgeSession = knowledge
                .createSession()
                .setActivationManager(new ReportingActivationManager())
                .setFireCriteria(gate::get)
                .set("G", msg.gravity)
                .set("time-step", 0.000002);

        super.setKnowledgeSession(knowledgeSession);

        this.initialData.clear();
        this.initialData.putAll(msg.particles);

        this.gate.set(true);
        SessionThread thread = new SessionThread();
        thread.start();
    }

    private Knowledge buildKnowledge(StartMessage msg) throws Exception {
        List<LiteralRule> rules = LiteralRule.parse(msg.rules);
        KnowledgeService service = AppContext.knowledgeService();
        Knowledge knowledge = service.newKnowledge();
        knowledge.addImport(Vector.class);
        knowledge.addImport(Particle.class);
        for (LiteralRule rule : rules) {
            Collection<FactBuilder> facts = new ArrayList<>();
            for (String var : rule.factTypeVars()) {
                if ("$subject".equals(var)) {
                    facts.add(FactBuilder.fact(var, Particle.class));
                }
                if ("$other".equals(var)) {
                    facts.add(FactBuilder.fact(var, Particle.class));
                }
                if ("$time".equals(var)) {
                    facts.add(FactBuilder.fact(var, SpaceTime.class));
                }
            }

            if (facts.isEmpty()) {
                throw new Exception("No facts are selected in rule '" + rule.getName() + "'");
            }

            RuleBuilder<Knowledge> builder = knowledge.newRule(rule.getName())
                    .forEach(facts)
                    .where(rule.parsedConditions())
                    .setRhs(rule.getBody());

            RuleDescriptor descriptor = knowledge.compileRule(builder);
            getMessenger().send(new Message(
                    "LOG",
                    "Rule compiled: " + descriptor.getName()
            ));
        }

        //TODO move to DEFAULT
        knowledge.setActivationMode(ActivationMode.CONTINUOUS);

        return knowledge;
    }

    @Override
    public boolean closeSession() {
        this.gate.set(false);
        this.pause(false); // Unlock latches if any
        this.initialData.clear();
        return super.closeSession();
    }

    private class ReportingActivationManager implements ActivationManager {
        private static final int fps = 20;
        private final AtomicLong lastReported = new AtomicLong(System.currentTimeMillis());

        @Override
        public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {

            try {
                getLatch().await();

                long now = System.currentTimeMillis();
                if (now - lastReported.get() > (1000 / fps)) {
                    Utils.delay(5);

                    // Get current particles
                    Map<String, Particle> particles = new HashMap<>();
                    getKnowledgeSession().forEachMemoryObject(o -> {
                        if (o instanceof Particle) {
                            Particle p = (Particle) o;
                            particles.put(String.valueOf(p.id), p);
                        }
                    });

                    if (particles.size() == 1) {
                        getMessenger().send(new Message(
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


    public class SessionThread extends Thread {

        public SessionThread() {
        }

        @Override
        public void run() {
            final StatefulSession session = getKnowledgeSession();
            final SocketMessenger messenger = getMessenger();
            session.insert(initialData.values());
            session.insert(new SpaceTime());
            try {
                messenger.send(new Message("STARTED"));
                running.set(true);
                session.fire();
            } catch (Exception e) {
                messenger.send(e);
            } finally {
                running.set(false);
                messenger.sendUnchecked(new Message("STOPPED"));

            }
        }
    }

}
