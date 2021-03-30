package org.evrete.jsr94;

import org.evrete.KnowledgeService;

import javax.rules.admin.*;
import java.util.Map;

public class RuleAdministratorImpl implements RuleAdministrator {
    private final LocalRuleExecutionSetProviderImpl localProvider;
    private final RuleExecutionSetProviderImpl provider;
    private final RuleSetRegistrations registrations;

    RuleAdministratorImpl(KnowledgeService service, RuleSetRegistrations registrations) {
        this.localProvider = new LocalRuleExecutionSetProviderImpl(service);
        this.provider = new RuleExecutionSetProviderImpl(service);
        this.registrations = registrations;
    }

    @Override
    public RuleExecutionSetProvider getRuleExecutionSetProvider(Map map) {
        return provider;
    }

    @Override
    public LocalRuleExecutionSetProvider getLocalRuleExecutionSetProvider(Map map) {
        return localProvider;
    }

    @Override
    public void registerRuleExecutionSet(String s, RuleExecutionSet ruleExecutionSet, Map map) throws RuleExecutionSetRegisterException {
        if (ruleExecutionSet instanceof RuleExecutionSetImpl) {
            registrations.registerRuleExecutionSet(s, (RuleExecutionSetImpl) ruleExecutionSet);
        } else {
            throw new RuleExecutionSetRegisterException("Can not register a third-party RuleExecutionSet");
        }
    }

    @Override
    public void deregisterRuleExecutionSet(String s, Map map) {
        registrations.deregisterRuleExecutionSet(s);
    }
}
