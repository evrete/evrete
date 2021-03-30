package org.evrete.jsr94;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.w3c.dom.Element;

import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetCreateException;
import javax.rules.admin.RuleExecutionSetProvider;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

class RuleExecutionSetProviderImpl implements RuleExecutionSetProvider {
    private final KnowledgeService knowledgeService;

    RuleExecutionSetProviderImpl(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(Element element, Map map) throws RuleExecutionSetCreateException, RemoteException {
        throw new UnsupportedOperationException("DOM elements are not directly supported, available DSL providers should parse documents as streams or URLs.");
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(Serializable serializable, Map map) throws RuleExecutionSetCreateException, RemoteException {
        if (serializable instanceof String) {
            return createRuleExecutionSet(serializable, map);
        }

        if (serializable instanceof URL) {
            URL url = (URL) serializable;
            try {
                return createRuleExecutionSet(map, url);
            } catch (IOException e) {
                throw new RuleExecutionSetCreateException("Unable to create ruleset from URL", e);
            }
        }

        if (serializable instanceof URL[]) {
            URL[] urls = (URL[]) serializable;
            try {
                return createRuleExecutionSet(map, urls);
            } catch (IOException e) {
                throw new RuleExecutionSetCreateException("Unable to create ruleset from URL[]", e);
            }
        }

        if (serializable instanceof URI) {
            URI uri = (URI) serializable;
            try {
                URL url = uri.toURL();
                return createRuleExecutionSet(map, url);
            } catch (IOException e) {
                throw new RuleExecutionSetCreateException("Unable to create ruleset from URI", e);
            }
        }

        if (serializable instanceof URI[]) {
            URI[] uris = (URI[]) serializable;
            try {
                URL[] urls = new URL[uris.length];
                for (int i = 0; i < uris.length; i++) {
                    urls[i] = uris[i].toURL();
                }
                return createRuleExecutionSet(map, urls);
            } catch (IOException e) {
                throw new RuleExecutionSetCreateException("Unable to create ruleset from URI[]", e);
            }
        }

        if (serializable instanceof File) {
            File file = (File) serializable;
            try {
                URL url = file.toURI().toURL();
                return createRuleExecutionSet(map, url);
            } catch (IOException e) {
                throw new RuleExecutionSetCreateException("Unable to create ruleset from File", e);
            }
        }

        if (serializable instanceof File[]) {
            File[] files = (File[]) serializable;
            try {
                URL[] urls = new URL[files.length];
                for (int i = 0; i < files.length; i++) {
                    urls[i] = files[i].toURI().toURL();
                }
                return createRuleExecutionSet(map, urls);
            } catch (IOException e) {
                throw new RuleExecutionSetCreateException("Unable to create ruleset from File[]", e);
            }
        }


        throw new RuleExecutionSetCreateException("Unable to create ruleset, unsupported serializable");
    }

    @Override
    public RuleExecutionSet createRuleExecutionSet(String s, Map map) throws RuleExecutionSetCreateException, IOException {
        String dsl = Utils.getStringProperty(map, Const.DSL_NAME);
        if (dsl == null) {
            throw new RuleExecutionSetCreateException("Missing DSL name property '" + Const.DSL_NAME + "'");
        }

        try {
            Knowledge knowledge = knowledgeService.newKnowledge();
            knowledge.appendDslRules(dsl, new StringReader(s));
            return new RuleExecutionSetImpl(knowledge, map);
        } catch (RuntimeException e) {
            throw new RuleExecutionSetCreateException("Unable to create RuleExecutionSet", e);
        }

    }

    private RuleExecutionSet createRuleExecutionSet(Map<?, ?> map, URL... urls) throws RuleExecutionSetCreateException, IOException {
        String dsl = Utils.getStringProperty(map, Const.DSL_NAME);
        if (dsl == null) {
            throw new RuleExecutionSetCreateException("Missing DSL name property '" + Const.DSL_NAME + "'");
        }

        try {
            Knowledge knowledge = knowledgeService.newKnowledge();
            knowledge.appendDslRules(dsl, urls);
            return new RuleExecutionSetImpl(knowledge, map);
        } catch (RuntimeException e) {
            throw new RuleExecutionSetCreateException("Unable to create RuleExecutionSet", e);
        }
    }
}
