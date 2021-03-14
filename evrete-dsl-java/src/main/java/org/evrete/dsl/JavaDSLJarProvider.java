package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.DSLKnowledgeProvider;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

public class JavaDSLJarProvider extends AbstractJavaDSLProvider implements DSLKnowledgeProvider {
    static final String NAME = "JAVA-JAR";
    private static final Logger LOGGER = Logger.getLogger(JavaDSLJarProvider.class.getName());
    private static final String CLASSES_PROPERTY = "org.evrete.dsl.classes";
    private static final String EMPTY_CLASSES = "";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, URL... resources) throws IOException {
        if (resources == null || resources.length == 0) return;
        URLClassLoader loader = new URLClassLoader(resources, targetContext.getClassLoader());
        String ruleSets = (String) targetContext.getConfiguration().getOrDefault(CLASSES_PROPERTY, EMPTY_CLASSES);
        if (ruleSets.isEmpty()) {
            LOGGER.warning("No ruleset classes were specified in the '" + CLASSES_PROPERTY + "', resources skipped.");
        } else {
            String[] classNames = ruleSets.split("[\\s,;]");
            for (String className : classNames) {
                if (className != null && !className.isEmpty()) {
                    try {
                        processRuleSet(targetContext, new JavaClassRuleSet(loader.loadClass(className)));
                    } catch (ClassNotFoundException e) {
                        LOGGER.warning("Ruleset class '" + className + "' not found");
                    }
                }
            }
        }
    }
}
