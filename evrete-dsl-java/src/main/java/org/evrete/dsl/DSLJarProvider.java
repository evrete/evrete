package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.builders.RuleSetBuilder;
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
    static final String CLASSES_PROPERTY = "org.evrete.dsl.rule-classes";
    static final String RULESETS_PROPERTY = "org.evrete.dsl.ruleset-names";
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
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, File file) throws IOException {
        return this.createClassMeta(target, file.toURI().toURL());
    }

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, URL url) throws IOException {
        RuntimeContext<C> context = target.getContext();
        try(JarClassloader jarClassloader = new JarClassloader(url, context.getClassLoader())) {
            String[] classes = CommonUtils.splitConfigString(target.get(CLASSES_PROPERTY, EMPTY_STRING));
            final String[] criteria;
            final  Collection<Class<?>> selectedRuleClasses;
            if(classes.length == 0) {
                String[] ruleSets = CommonUtils.splitCSV(target.get(RULESETS_PROPERTY, EMPTY_STRING));
                if(ruleSets.length == 0) {
                    throw new IllegalArgumentException("Neither ruleset names nor class names are specified");
                } else {
                    criteria = ruleSets;
                    selectedRuleClasses = readRulesets(jarClassloader, ruleSets);
                }
            } else {
                criteria = classes;
                selectedRuleClasses = readClasses(jarClassloader, classes);
            }

            Collection<DSLMeta<C>> result = new ArrayList<>(selectedRuleClasses.size());
            for(Class<?> ruleClass : selectedRuleClasses) {
                if(Utils.isDslRuleClass(ruleClass)) {
                    result.add(new DSLMetaClassSource<>(publicLookup, ruleClass));
                }
            }

            if(result.isEmpty()) {
                LOGGER.warning("No rule classes selected given the provided criteria: " + Arrays.toString(criteria));
            }
            return result;
        }
    }

    private Collection<Class<?>> readClasses(JarClassloader jarClassloader, String[] classNames) {
        Collection<Class<?>> result = new ArrayList<>(classNames.length);
        for(String className : classNames) {
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
            if(rs != null) {
                String ruleSetName = rs.value();
                if(ruleSetName != null && !ruleSetName.isEmpty() && filter.contains(ruleSetName)) {
                    stringClassMap.put(ruleSetName, c);
                }
            }
        });

        // We need to return results in the same order
        Collection<Class<?>> result = new ArrayList<>(stringClassMap.size());
        for(String ruleSetName : rulesetNames) {
            Class<?> ruleClass = stringClassMap.get(ruleSetName);
            if(ruleClass != null) {
                result.add(ruleClass);
            }
        }
        return result;
    }


    @Override
    public Set<Class<?>> sourceTypes() {
        return Set.of(SUPPORTED_TYPES);
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_JAR;
    }

}
