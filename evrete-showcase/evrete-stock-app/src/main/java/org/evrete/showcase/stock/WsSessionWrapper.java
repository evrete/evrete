package org.evrete.showcase.stock;

import org.evrete.api.ActivationManager;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.showcase.stock.json.Message;
import org.evrete.showcase.stock.json.RunMessage;
import org.evrete.showcase.stock.rule.TimeSlot;

import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

class WsSessionWrapper {
    public static final int MAX_DATA_SIZE = 128;
    final AtomicInteger counter = new AtomicInteger(0);
    private final WsMessenger messenger;
    private StatefulSession knowledgeSession;

    public WsSessionWrapper(Session session) {
        this.messenger = new WsMessenger(session);
    }

    synchronized boolean insert(OHLC ohlc) throws IOException {
        Objects.requireNonNull(ohlc);
        int id = counter.getAndIncrement();
        if (id >= MAX_DATA_SIZE) {
            return false;
        } else {
            TimeSlot slot = new TimeSlot(id, ohlc);
            if (knowledgeSession != null) {
                knowledgeSession.insertAndFire(slot);
                messenger.sendDelayed(new Message("OHLC_INSERTED", Utils.toJson(slot)));
            }
            return true;
        }
    }

    public WsMessenger getMessenger() {
        return messenger;
    }

    public synchronized void closeSession() {
        if (knowledgeSession != null) {
            knowledgeSession.close();
            knowledgeSession = null;
        }
        counter.set(0);
    }

    synchronized void initSession(RunMessage msg) throws Exception {
        // Set delay
        messenger.setDelay(msg.delay);
        // Close previous session if active
        closeSession();

        // Compile knowledge session
        Knowledge knowledge = LiteralRule.parse(AppContext.knowledgeService(), msg.rules, messenger);
        this.knowledgeSession = knowledge.createSession();
        // We'll be using activation manager for execution callbacks
        ActivationManager activationManager = this.knowledgeSession.getActivationManager();
        this.knowledgeSession.setActivationManager(new DelegateActivationManager(activationManager));

        messenger.send(new Message("READY_FOR_DATA"));
    }

    private class DelegateActivationManager implements ActivationManager {
        private final ActivationManager delegate;

        DelegateActivationManager(ActivationManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {
            if (sequenceId == 100) {
                throw new IllegalStateException("Your rule set has been fired 100 times. Either there's a logical loop in your rules, or the ruleset is too big for this demo app");
            }
            delegate.onAgenda(sequenceId, agenda);
        }

        @Override
        public boolean test(RuntimeRule rule) {
            return delegate.test(rule);
        }

        @Override
        public void onActivation(RuntimeRule rule) {
            delegate.onActivation(rule);
            try {
                messenger.sendDelayed(new Message("RULE_EXECUTED", rule.getName()));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
