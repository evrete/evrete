package org.evrete.jsr94;


import org.evrete.KnowledgeService;

import javax.rules.ConfigurationException;
import javax.rules.RuleRuntime;
import javax.rules.RuleServiceProvider;
import javax.rules.admin.RuleAdministrator;

public class RuleServiceProviderImpl extends RuleServiceProvider {
    static final String RULE_SERVICE_PROVIDER = "org.evrete.jsr94";
    private static final KnowledgeService service = new KnowledgeService();
    private final RuleSetRegistrations registrations = new RuleSetRegistrations();


    @Override
    public RuleRuntime getRuleRuntime() {
        return new RuleRuntimeImpl(registrations);
    }

    @Override
    public RuleAdministrator getRuleAdministrator() throws ConfigurationException {
        return new RuleAdministratorImpl(service, registrations);
    }
}
