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
        try {
            Knowledge knowledge = knowledgeService.newKnowledge().builder().importRules(Utils.dslName(map), inputStream).build();
            Utils.copyConfiguration(knowledge, map);
            return new RuleExecutionSetImpl(knowledge, map);
        } catch (RuntimeException e) {
            throw new RuleExecutionSetCreateException("Unable to create RuleExecutionSet", e);
        }
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(Reader reader, Map map) throws RuleExecutionSetCreateException, IOException {
        try {
            Knowledge knowledge = knowledgeService.newKnowledge().builder().importRules(Utils.dslName(map), reader).build();
            Utils.copyConfiguration(knowledge, map);
            return new RuleExecutionSetImpl(knowledge, map);
        } catch (Exception e) {
            throw new RuleExecutionSetCreateException("Unable to create RuleExecutionSet", e);
        }
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(Object o, Map map) throws RuleExecutionSetCreateException {
        try {
            Knowledge knowledge = knowledgeService.newKnowledge().builder().importRules(Utils.dslName(map), o).build();
            Utils.copyConfiguration(knowledge, map);
            return new RuleExecutionSetImpl(knowledge, map);
        } catch (Exception e) {
            throw new RuleExecutionSetCreateException("Unable to create RuleExecutionSet", e);
        }
    }
}
