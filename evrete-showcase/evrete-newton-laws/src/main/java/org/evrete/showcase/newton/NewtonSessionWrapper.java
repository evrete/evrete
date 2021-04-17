package org.evrete.showcase.newton;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.showcase.newton.model.Particle;
import org.evrete.showcase.newton.model.SpaceTime;
import org.evrete.showcase.newton.rules.MainRuleset;
import org.evrete.showcase.shared.AbstractSocketSession;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.shared.Utils;

import javax.websocket.Session;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class NewtonSessionWrapper extends AbstractSocketSession {
    private static final double TIME_STEP = 0.000002;
    //private final AtomicBoolean gate = new AtomicBoolean(true);
    //private CountDownLatch latch = new CountDownLatch(0);
    private Future<?> thread;

    NewtonSessionWrapper(Session session) {
        super(session);
    }

/*
    private synchronized void pause(boolean flag) {
        if (flag) {
            this.latch = new CountDownLatch(1);
        } else {
            this.latch.countDown();
        }
    }
*/

/*
    private boolean isPaused() {

        return this.latch.getCount() > 0;
    }
*/

/*
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
*/

    void updateGravity(double d) {
        StatefulSession session = getKnowledgeSession();
        if (session != null) {
            session.set("G", d);
        }
    }

/*
    private CountDownLatch getLatch() {
        return latch;
    }
*/

    synchronized void initSession() throws Exception {
        if (thread != null && !thread.isDone()) {
            throw new Exception("Session already started");
        }
        //this.gate.set(true);

        // Close previous session if active
        closeSession();

        // Compile knowledge session
        Knowledge knowledge = buildKnowledge();
        StatefulSession session = knowledge
                .createSession()
                //.setFireCriteria(gate::get)
                .set("time-step", TIME_STEP)
                .setActivationMode(ActivationMode.CONTINUOUS);

        super.setKnowledgeSession(session);
        session.setActivationManager(new ReportingActivationManager(getMessenger()));

        for (int i = 0; i < 4; i++) {
            Particle p = new Particle();
            p.id = i;
            p.position.x = 10 * i;
            p.position.y = 10 * i;
            session.insert(p);
        }

        SpaceTime space = new SpaceTime();
        session.insert(space);
        System.out.println("!!!! firing");
        //session.fire();
        this.thread = session.fireAsync();

        while (!thread.isDone()) {
            System.out.println("Running ....");
            Utils.delay(500);
        }

        try {
            Object o = thread.get();
            System.out.println("Done !!!!! " + o);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        getMessenger().send(new Message("STARTED"));
    }

    private Knowledge buildKnowledge() throws Exception {
        KnowledgeService service = AppContext.knowledgeService();
        Knowledge knowledge = service.newKnowledge("JAVA-CLASS", MainRuleset.class);
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
        //this.gate.set(false);
        //this.pause(false); // Unlock latches if any
        if (thread != null) {
            this.thread.cancel(true);
        }
        return super.closeSession();
    }

    private class ReportingActivationManager implements ActivationManager {
        private final SocketMessenger messenger;

        ReportingActivationManager(SocketMessenger messenger) {
            this.messenger = messenger;
        }

        @Override
        public boolean test(RuntimeRule rule) {
            return true;
        }

        @Override
        public void onActivation(RuntimeRule rule, long count) {
            System.out.println("RULE: " + rule + " : " + count);
        }

        @Override
        public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {
            System.out.println("AGENDA: " + agenda);
            try {
                //getLatch().await();

                Utils.delay(500);

                // Get current particles
                Map<String, Particle> particles = new HashMap<>();

                getKnowledgeSession().forEachFact((handle, o) -> {
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
                    //gate.set(false);
                } else {
                    messenger.send(new Message(
                            "REPORT",
                            Utils.toJson(particles)
                    ));
                }

            } catch (Exception e) {
                e.printStackTrace();
                closeSession();
            }
        }
    }
}
