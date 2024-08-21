package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.dsl.annotation.RuleSet;
import org.evrete.util.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-JAR' DSL knowledge.
 */

public class DSLJarProvider extends AbstractDSLProvider {
    private static final Logger LOGGER = Logger.getLogger(DSLJarProvider.class.getName());
    private static final String EMPTY_STRING = "";

    private static final Class<?>[] SUPPORTED_TYPES = new Class<?>[]{
            TYPE_URL,
            TYPE_FILE
    };

    /**
     * Default public constructor
     */
    public DSLJarProvider() {
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromFiles(RuntimeContext<C> context, Collection<File> resources) throws IOException {
        return this.createFromURLs(context, toURLs(resources));
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromURLs(RuntimeContext<C> context, Collection<URL> resources) throws IOException {
        JarClassloader jarClassloader = new JarClassloader(resources, context.getClassLoader());
        String[] configClasses = CommonUtils.splitConfigString(context.get(PROP_RULE_CLASSES, EMPTY_STRING));
        final String[] criteria;
        final Collection<Class<?>> selectedRuleClasses;
        if (configClasses.length == 0) {
            String[] configRuleSets = CommonUtils.splitCSV(context.get(PROP_RULESETS, EMPTY_STRING));
            if (configRuleSets.length == 0) {
                throw new IllegalArgumentException("Neither ruleset names nor class names are specified");
            } else {
                criteria = configRuleSets;
                selectedRuleClasses = readRulesets(jarClassloader, configRuleSets);
            }
        } else {
            criteria = configClasses;
            selectedRuleClasses = readClasses(jarClassloader, configClasses);
        }

        Collection<Class<?>> dslClasses = new ArrayList<>(selectedRuleClasses.size());
        for (Class<?> ruleClass : selectedRuleClasses) {
            if (Utils.isDslRuleClass(ruleClass)) {
                dslClasses.add(ruleClass);
            }
        }

        if (dslClasses.isEmpty()) {
            LOGGER.fine(()->"No rule classes selected given the provided criteria: " + Arrays.toString(criteria));
            return null;
        } else {
            return new ResourceClasses(jarClassloader, dslClasses, jarClassloader);
        }
    }

    private Collection<Class<?>> readClasses(JarClassloader jarClassloader, String[] classNames) {
        Collection<Class<?>> result = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            try {
                result.add(jarClassloader.loadClass(className));
            } catch (ClassNotFoundException e) {
                throw new MalformedResourceException("Unable to load class " + className, e);
            }
        }
        return result;
    }

    private Collection<Class<?>> readRulesets(JarClassloader jarClassloader, String[] rulesetNames) throws IOException {
        Map<String, Class<?>> stringClassMap = new HashMap<>();
        Set<String> filter = new HashSet<>(Arrays.asList(rulesetNames));
        jarClassloader.scan(c -> {
            RuleSet rs = c.getAnnotation(RuleSet.class);
            if (rs != null) {
                String ruleSetName = rs.value();
                if (ruleSetName != null && !ruleSetName.isEmpty() && filter.contains(ruleSetName)) {
                    stringClassMap.put(ruleSetName, c);
                }
            }
        });

        // We need to return results in the same order
        Collection<Class<?>> result = new ArrayList<>(stringClassMap.size());
        for (String ruleSetName : rulesetNames) {
            Class<?> ruleClass = stringClassMap.get(ruleSetName);
            if (ruleClass != null) {
                result.add(ruleClass);
            }
        }
        return result;
    }


    @Override
    public Set<Class<?>> sourceTypes() {
        return new HashSet<>(Arrays.asList(SUPPORTED_TYPES));
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_JAR;
    }

}
