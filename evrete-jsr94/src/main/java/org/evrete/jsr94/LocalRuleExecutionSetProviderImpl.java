package org.evrete.jsr94;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;

import javax.rules.admin.LocalRuleExecutionSetProvider;
import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetCreateException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

class LocalRuleExecutionSetProviderImpl implements LocalRuleExecutionSetProvider {
    private final KnowledgeService knowledgeService;

    LocalRuleExecutionSetProviderImpl(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(InputStream inputStream, Map map) throws RuleExecutionSetCreateException, IOException {
        String dsl = Utils.getStringProperty(map, Const.DSL_NAME);
        if (dsl == null) {
            throw new RuleExecutionSetCreateException("Missing DSL name property '" + Const.DSL_NAME + "'");
        }
        try {
            Knowledge knowledge = knowledgeService.newKnowledge();
            Utils.copyConfiguration(knowledge, map);
            knowledge.appendDslRules(dsl, inputStream);
            return new RuleExecutionSetImpl(knowledge, map);
        } catch (RuntimeException e) {
            throw new RuleExecutionSetCreateException("Unable to create RuleExecutionSet", e);
        }
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(Reader reader, Map map) throws RuleExecutionSetCreateException, IOException {
        String dsl = Utils.getStringProperty(map, Const.DSL_NAME);
        if (dsl == null) {
            throw new RuleExecutionSetCreateException("Missing DSL name property '" + Const.DSL_NAME + "'");
        }
        try {
            Knowledge knowledge = knowledgeService.newKnowledge();
            Utils.copyConfiguration(knowledge, map);
            knowledge.appendDslRules(dsl, reader);
            return new RuleExecutionSetImpl(knowledge, map);
        } catch (RuntimeException e) {
            throw new RuleExecutionSetCreateException("Unable to create RuleExecutionSet", e);
        }
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(Object o, Map map) throws RuleExecutionSetCreateException {
        throw new RuleExecutionSetCreateException("Unsupported by " + getClass().getName());
    }
}
