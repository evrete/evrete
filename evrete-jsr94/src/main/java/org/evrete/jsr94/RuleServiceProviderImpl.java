package org.evrete.jsr94;


import org.evrete.KnowledgeService;

import javax.rules.ConfigurationException;
import javax.rules.RuleRuntime;
import javax.rules.RuleServiceProvider;
import javax.rules.admin.RuleAdministrator;

public class RuleServiceProviderImpl extends RuleServiceProvider {
    static final String RULE_SERVICE_PROVIDER = "org.evrete.jsr94";
    static final KnowledgeService service = new KnowledgeService();
    private final RuleSetRegistrations registrations = new RuleSetRegistrations();


    @Override
    public RuleRuntime getRuleRuntime() throws ConfigurationException {
        //TODO !!! class instance ???
        return new RuleRuntimeImpl(registrations);
    }

    @Override
    public RuleAdministrator getRuleAdministrator() throws ConfigurationException {
        //TODO !!! class instance ???
        return new RuleAdministratorImpl(service, registrations);
    }
}
