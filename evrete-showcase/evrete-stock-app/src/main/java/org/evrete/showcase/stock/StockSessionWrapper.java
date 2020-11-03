package org.evrete.showcase.stock;

import org.evrete.api.*;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.showcase.shared.*;
import org.evrete.showcase.stock.json.RunMessage;
import org.evrete.showcase.stock.rule.TimeSlot;

import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

class StockSessionWrapper extends AbstractSocketSession {
    public static final int MAX_DATA_SIZE = 128;
    public static final int MAX_FACTS = 4;

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

    private static Knowledge parse(String rs, SocketMessenger messenger) throws Exception {
        Knowledge knowledge = AppContext.knowledgeService().newKnowledge();

        Type<TimeSlot> subjectType = knowledge.getTypeResolver().declare(TimeSlot.class);
        knowledge.getTypeResolver().wrapType(new SlotType(subjectType));


        List<LiteralRule> parsedRules = LiteralRule.parse(rs);

        messenger.sendDelayed(new Message("LOG", "Compiling " + parsedRules.size() + " rules..."));
        for (LiteralRule r : parsedRules) {
            // Sanity checks
            if (r.factTypeVars().isEmpty()) {
                throw new Exception("Invalid rule header format: " + r.getName());
            } else if (r.factTypeVars().size() > MAX_FACTS) {
                throw new Exception("Too many fact declarations in rule '" + r.getName() + "'");
            }


            RuleBuilder<Knowledge> builder = knowledge.newRule(r.getName());
            FactBuilder[] factTypes = r.parsedFactTypes(TimeSlot.class);
            String[] conditions = r.parsedConditions();
            builder
                    .forEach(factTypes)
                    .where(conditions)
                    .setRhs(r.getBody());

            RuleDescriptor descriptor = knowledge.compileRule(builder);
            messenger.sendDelayed(new Message("RULE_COMPILED", descriptor.getName()));
        }

        //TODO move to DEFAULT
        knowledge.setAgendaMode(AgendaMode.CONTINUOUS);
        return knowledge;

    }

    public synchronized boolean closeSession() {
        counter.set(0);
        return super.closeSession();
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

    synchronized void initSession(RunMessage msg) throws Exception {
        // Set delay
        SocketMessenger messenger = getMessenger();
        messenger.setDelay(msg.delay);
        // Close previous session if active
        closeSession();

        // Compile knowledge session


        Knowledge knowledge = parse(msg.rules, messenger);
        StatefulSession knowledgeSession = knowledge.createSession();
        setKnowledgeSession(knowledgeSession);
        // We'll be using activation manager for execution callbacks
        ActivationManager activationManager = knowledgeSession.getActivationManager();
        knowledgeSession.setActivationManager(new DelegateActivationManager(activationManager));

        messenger.send(new Message("READY_FOR_DATA"));
    }

    public static class SlotType extends TypeWrapper<TimeSlot> {
        public SlotType(Type<TimeSlot> delegate) {
            super(delegate);
        }

        @Override
        public TypeField getField(String name) {
            TypeField found = getDelegate().getField(name);
            if (found == null) {
                //Declaring field right in the get method
                found = declareField(name, (ToDoubleFunction<TimeSlot>) subject -> subject.get(name, ConditionSuperClass.UNDEFINED));
            }
            return found;
        }
    }

}
