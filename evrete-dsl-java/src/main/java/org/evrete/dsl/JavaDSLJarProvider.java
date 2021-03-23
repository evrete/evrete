package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.api.RuntimeContext;
import org.evrete.util.compiler.BytesClassLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;

public class JavaDSLJarProvider extends AbstractJavaDSLProvider {
    private static final Logger LOGGER = Logger.getLogger(JavaDSLJarProvider.class.getName());
    static final String CLASSES_PROPERTY = "org.evrete.dsl.rule-classes";
    private static final String EMPTY_CLASSES = "";
    private static final String CLASS_ENTRY_SUFFIX = ".class";
    private static final String[] DISALLOWED_PACKAGES = new String[]{
            "java.",
            "javax.",
            "sun.",
            "org.evrete.",
    };

    private static void apply(RuntimeContext<?> targetContext, Set<String> ruleClasses, InputStream... streams) throws IOException {
        ClassLoader ctxClassLoader = targetContext.getClassLoader();
        ProtectionDomain domain = targetContext.getService().getSecurity().getProtectionDomain(RuleScope.BOTH);
        BytesClassLoader classLoader = new BytesClassLoader(ctxClassLoader, domain);
        fillClassLoader(classLoader, streams);

        for (String ruleClass : ruleClasses) {
            try {
                Class<?> cl = classLoader.loadClass(ruleClass);
                processRuleSet(targetContext, new JavaClassRuleSet(cl));
            } catch (ClassNotFoundException e) {
                // No such rule class
                LOGGER.warning("Ruleset class '" + ruleClass + "' not found");
            }
        }
    }

    @Override
    public Knowledge create(KnowledgeService service, URL... resources) throws IOException {
        Knowledge knowledge = service.newKnowledge();
        apply(knowledge, resources);
        return knowledge;
    }

    private static void fillClassLoader(BytesClassLoader classLoader, InputStream... resources) throws IOException {
        JarInputStream[] streams = new JarInputStream[resources.length];
        for (int i = 0; i < resources.length; i++) {
            streams[i] = new JarInputStream(resources[i]);
        }
        for (JarInputStream resource : streams) {
            try (JarInputStream is = resource) {
                applyJar(classLoader, is);
            }
        }
    }

    private static void applyJar(BytesClassLoader secureClassLoader, JarInputStream is) throws IOException {
        JarEntry entry;
        byte[] buffer = new byte[1024];
        Map<String, byte[]> resources = new HashMap<>();
        Map<String, byte[]> classes = new HashMap<>();
        while ((entry = is.getNextJarEntry()) != null) {
            if (!entry.isDirectory()) {
                byte[] bytes = toBytes(is, buffer);
                String name = entry.getName();
                if (name.endsWith(CLASS_ENTRY_SUFFIX)) {
                    String className = name
                            .substring(0, name.length() - CLASS_ENTRY_SUFFIX.length())
                            .replaceAll("/", ".");
                    validateClassName(className);
                    classes.put(className, bytes);
                } else {
                    resources.put(name, bytes);
                }
            }
        }

        // Building classes and resources
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
            String className = e.getKey();
            byte[] bytes = e.getValue();

            try {
                secureClassLoader.loadClass(className);
            } catch (ClassNotFoundException cnf) {
                // Class not found, building new one
                Class<?> clazz = secureClassLoader.buildClass(bytes);
                if (!clazz.getName().equals(className)) {
                    throw new IllegalStateException();
                }
            }
        }

        for (Map.Entry<String, byte[]> e : resources.entrySet()) {
            secureClassLoader.addResource(e.getKey(), e.getValue());
        }
    }

    private static Set<String> ruleClasses(RuntimeContext<?> targetContext) {
        Set<String> ruleClasses = new HashSet<>();
        String ruleSets = (String) targetContext.getConfiguration().getOrDefault(CLASSES_PROPERTY, EMPTY_CLASSES);
        if (!ruleSets.isEmpty()) {
            String[] classNames = ruleSets.split("[\\s,;]");
            for (String className : classNames) {
                if (className != null && !className.isEmpty()) {
                    ruleClasses.add(className);
                }
            }
        }
        return ruleClasses;
    }

    private static void validateClassName(String className) {
        for (String pkg : DISALLOWED_PACKAGES) {
            if (className.startsWith(pkg)) {
                throw new IllegalArgumentException("Package name not allowed '" + className + "'");
            }
        }
    }

    private static byte[] toBytes(JarInputStream is, byte[] buffer) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read;
        while ((read = is.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        bos.flush();
        bos.close();
        return bos.toByteArray();
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_J;
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, InputStream... streams) throws IOException {
        if (streams == null || streams.length == 0) return;
        Set<String> ruleClasses = ruleClasses(targetContext);
        if (ruleClasses.isEmpty()) {
            LOGGER.warning("No ruleset classes were specified in the '" + CLASSES_PROPERTY + "', resources skipped.");
        } else {
            apply(targetContext, ruleClasses, streams);
        }
    }

}
