package org.evrete.showcase.stock;

import org.evrete.api.ActivationManager;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.showcase.shared.AbstractSocketSession;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.shared.Utils;
import org.evrete.showcase.stock.json.RunMessage;
import org.evrete.showcase.stock.rule.TimeSlot;

import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

class StockSessionWrapper extends AbstractSocketSession {
    public static final int MAX_DATA_SIZE = 128;
    final AtomicInteger counter = new AtomicInteger(0);

    public StockSessionWrapper(Session session) {
        super(session);
    }

    synchronized boolean insert(OHLC ohlc) throws IOException {
        Objects.requireNonNull(ohlc);
        int id = counter.getAndIncrement();
        if (id >= MAX_DATA_SIZE) {
            return false;
        } else {
            TimeSlot slot = new TimeSlot(id, ohlc);
            StatefulSession knowledgeSession = getKnowledgeSession();
            if (knowledgeSession != null) {
                knowledgeSession.insertAndFire(slot);
                getMessenger().sendDelayed(new Message("OHLC_INSERTED", Utils.toJson(slot)));
            }
            return true;
        }
    }

    public synchronized void closeSession() {
        super.closeSession();
        counter.set(0);
    }

    synchronized void initSession(RunMessage msg) throws Exception {
        // Set delay
        SocketMessenger messenger = getMessenger();
        messenger.setDelay(msg.delay);
        // Close previous session if active
        closeSession();

        // Compile knowledge session
        Knowledge knowledge = LiteralRule.parse(AppContext.knowledgeService(), msg.rules, messenger);
        StatefulSession knowledgeSession = knowledge.createSession();
        setKnowledgeSession(knowledgeSession);
        // We'll be using activation manager for execution callbacks
        ActivationManager activationManager = knowledgeSession.getActivationManager();
        knowledgeSession.setActivationManager(new DelegateActivationManager(activationManager));

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
                getMessenger().sendDelayed(new Message("RULE_EXECUTED", rule.getName()));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
