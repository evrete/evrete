package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;
import org.evrete.dsl.annotation.RuleSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.evrete.dsl.Utils.LOGGER;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-JAR' DSL knowledge.
 */

public class DSLJarProvider extends AbstractDSLProvider {
    static final String CLASSES_PROPERTY = "org.evrete.dsl.rule-classes";
    private static final String EMPTY_CLASSES = "";
    private static final String CLASS_ENTRY_SUFFIX = ".class";
    private static final String[] DISALLOWED_PACKAGES = new String[]{
            "java.",
            "javax.",
            "sun.",
            "org.evrete.",
    };

    /**
     * Default public constructor
     */
    public DSLJarProvider() {
    }

    private static Knowledge apply(Knowledge knowledge, MethodHandles.Lookup lookup, Set<String> ruleClasses, InputStream... streams) throws IOException {
        List<Class<?>> jarClasses = fillClassLoader(knowledge, streams);
        Knowledge current = knowledge;
        if (ruleClasses.isEmpty()) {
            // Implicit declaration via @RuleSet
            if (jarClasses.isEmpty()) {
                LOGGER.warning("Classes annotated with @" + RuleSet.class.getSimpleName() + " not found");
                return knowledge;
            } else {
                for (Class<?> cl : jarClasses) {
                    current = processRuleSet(current, lookup, cl);
                }
            }
        } else {
            // Classes specified explicitly
            for (String ruleClass : ruleClasses) {
                try {
                    Class<?> cl = current.getClassLoader().loadClass(ruleClass);
                    current = processRuleSet(current, lookup, cl);
                } catch (ClassNotFoundException e) {
                    // No such rule class
                    LOGGER.warning("Ruleset class '" + ruleClass + "' not found");
                }
            }
        }

        return current;
    }

    private static Set<String> ruleClasses(Configuration configuration) {
        Set<String> ruleClasses = new HashSet<>();
        String ruleSets = (String) configuration.getOrDefault(CLASSES_PROPERTY, EMPTY_CLASSES);
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

    private static List<Class<?>> fillClassLoader(RuntimeContext<?> ctx, InputStream... resources) throws IOException {
        JarInputStream[] streams = new JarInputStream[resources.length];
        for (int i = 0; i < resources.length; i++) {
            streams[i] = new JarInputStream(resources[i]);
        }

        List<Class<?>> ruleSets = new LinkedList<>();

        for (JarInputStream resource : streams) {
            try (JarInputStream is = resource) {
                List<Class<?>> jarRuleSets = applyJar(ctx, is);
                ruleSets.addAll(jarRuleSets);
            }
        }
        return ruleSets;
    }

    private static List<Class<?>> applyJar(RuntimeContext<?> ctx, JarInputStream is) throws IOException {
        JavaSourceCompiler compiler = ctx.getSourceCompiler();
        JarEntry entry;
        byte[] buffer = new byte[1024];
        //Map<String, byte[]> resources = new HashMap<>();
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
                }
            }
        }

        // Building classes and resources
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
            String className = e.getKey();
            byte[] bytes = e.getValue();
            compiler.defineClass(className, bytes);
        }

        List<Class<?>> allClasses = new LinkedList<>();
        for(String classNme : classes.keySet()) {
            try {
                allClasses.add(ctx.getClassLoader().loadClass(classNme));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Class '" + classNme + "' couldn't be loaded", e);
            }
        }
        return allClasses;
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
    public Knowledge create(KnowledgeService service, TypeResolver typeResolver, InputStream... streams) throws IOException {
        if (streams == null || streams.length == 0) throw new IOException("Empty streams");
        Set<String> ruleClasses = ruleClasses(service.getConfiguration());
        Knowledge knowledge = service.newKnowledge(typeResolver);
        MethodHandles.Lookup lookup = defaultLookup();
        return apply(knowledge, lookup, ruleClasses, streams);
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_JAR;
    }

}
