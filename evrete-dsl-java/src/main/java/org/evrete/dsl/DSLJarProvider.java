package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

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

    private static final Class<?>[] SUPPORTED_TYPES = new Class<?>[]{
            TYPE_URL,
            TYPE_INPUT_STREAM
    };

    /**
     * Default public constructor
     */
    public DSLJarProvider() {
    }

    @Override
    public Optional<Class<?>[]> sourceTypes() {
        return Optional.of(SUPPORTED_TYPES);
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, InputStream[] inputStreams) throws IOException {
        try {
            return Arrays.stream(inputStreams)
                    .flatMap(is -> classes(context, is));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, URL[] urls) throws IOException {
        try {
            return Arrays.stream(urls)
                    .map(url -> {
                        try {
                            return url.openStream();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .flatMap(is -> classes(context, is));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Stream<Class<?>> classes(RuntimeContext<?> context, InputStream is) {
        try {
            JarInputStream jarInputStream = new JarInputStream(is);
            Set<String> ruleClasses = ruleClasses(context.getConfiguration());

            return Utils.jarStream(jarInputStream)
                    .filter(entry -> {
                        String name = entry.name;
                        return name.endsWith(CLASS_ENTRY_SUFFIX) && validateClass(name);
                    })
                    .map((Function<JarBytesEntry, Class<?>>) jarEntry -> {
                        String className = jarEntry.name
                                .substring(0, jarEntry.name.length() - CLASS_ENTRY_SUFFIX.length())
                                .replaceAll("/", ".");
                        return context.addClass(className, jarEntry.bytes);
                    })
                    .filter(aClass -> {
                        // TODO remove this predicate when the old loaders are deprecated
                        String className = aClass.getName();
                        if (ruleClasses.isEmpty()) {
                            return true;
                        } else {
                            return ruleClasses.contains(className);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private static boolean validateClass(String className) {
        for (String pkg : DISALLOWED_PACKAGES) {
            if (className.startsWith(pkg)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Knowledge create(KnowledgeService service, URL... urls) throws IOException {
        return service
                .newKnowledge()
                .builder()
                .importAllRules(this, urls)
                .build();
    }

    @Override
    public Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        return service
                .newKnowledge()
                .builder()
                .importAllRules(this, streams)
                .build();
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_JAR;
    }

}
