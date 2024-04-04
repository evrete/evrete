package org.evrete.jsr94;


import org.evrete.KnowledgeService;

import javax.rules.RuleRuntime;
import javax.rules.RuleServiceProvider;
import javax.rules.admin.RuleAdministrator;

/**
 * Implementation of the RuleServiceProvider interface
 */
public class RuleServiceProviderImpl extends RuleServiceProvider {
    private static final KnowledgeService service = new KnowledgeService();
    private final RuleSetRegistrations registrations = new RuleSetRegistrations();

    /**
     * Public no-arg constructor for initialization via {@link Class#forName(String)}
     */
    public RuleServiceProviderImpl() {
    }

    @Override
    public RuleRuntime getRuleRuntime() {
        return new RuleRuntimeImpl(registrations);
    }

    @Override
    public RuleAdministrator getRuleAdministrator() {
        return new RuleAdministratorImpl(service, registrations);
    }
}
