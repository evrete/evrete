package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.api.RuleSession;
import org.evrete.api.RuntimeContext;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.api.events.Events;
import org.evrete.api.events.SessionCreatedEvent;
import org.evrete.api.spi.DSLKnowledgeProvider;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;

abstract class AbstractDSLProvider implements DSLKnowledgeProvider, Constants {
    static final Logger LOGGER = Logger.getLogger(AbstractDSLProvider.class.getName());
    static final Class<?> TYPE_URL = URL.class;
    static final Class<?> TYPE_CHAR_SEQUENCE = CharSequence.class;
    static final Class<?> TYPE_READER = Reader.class;
    static final Class<?> TYPE_INPUT_STREAM = InputStream.class;
    static final Class<?> TYPE_CLASS = Class.class;
    static final Class<?> TYPE_FILE = File.class;

    static final String CHARSET_PROPERTY = "org.evrete.source-charset";
    static final String CHARSET_DEFAULT = "UTF-8";

    final MethodHandles.Lookup publicLookup;

    AbstractDSLProvider() {
        this.publicLookup = MethodHandles.publicLookup();
    }

    <C extends RuntimeContext<C>> ResourceClasses createFromURLs(RuntimeContext<C> target, Collection<URL> resources) throws IOException {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> ResourceClasses createFromReaders(RuntimeContext<C> target, Collection<Reader> resources) throws IOException {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> ResourceClasses createFromStrings(RuntimeContext<C> target, Collection<CharSequence> resources) {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> ResourceClasses createFromStreams(RuntimeContext<C> target, Collection<InputStream> resources) throws IOException {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> ResourceClasses createFromClasses(RuntimeContext<C> target, Collection<Class<?>> resources) {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> ResourceClasses createFromFiles(RuntimeContext<C> target, Collection<File> resources) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected abstract Set<Class<?>> sourceTypes();


    @Override
    public final <C extends RuntimeContext<C>> void appendTo(@NonNull RuntimeContext<C> context, Object source) throws IOException {
        if (source != null) {
            try {
                this.appendToInner(context, source);
            } catch (UncheckedIOException e) {
                // Unwrap IO exceptions
                throw e.getCause();
            }
        } else  {
            LOGGER.warning(()->"No sources specified");
        }
    }

    private <C extends RuntimeContext<C>> void appendToInner(RuntimeContext<C> context, @NonNull Object source) throws IOException {
        Set<Class<?>> supportedTypes = sourceTypes();
        // Each provider supports arrays and iterables of the supported types
        ResourceCollection resources = ResourceCollection.factory(source);
        if(resources == null) {
            return;
        }

        Class<?> componentType = resources.getComponentType();
        if(matches(componentType, supportedTypes)) {
            if (TYPE_URL.isAssignableFrom(componentType)) {
                appendToInner(context, createFromURLs(context, resources.cast()));
            } else if (TYPE_READER.isAssignableFrom(componentType)) {
                appendToInner(context, createFromReaders(context, resources.cast()));
            } else if (TYPE_CHAR_SEQUENCE.isAssignableFrom(componentType)) {
                appendToInner(context, createFromStrings(context, resources.cast()));
            } else if (TYPE_INPUT_STREAM.isAssignableFrom(componentType)) {
                appendToInner(context, createFromStreams(context, resources.cast()));
            } else if (TYPE_CLASS.isAssignableFrom(componentType)) {
                appendToInner(context, createFromClasses(context, resources.cast()));
            } else if (TYPE_FILE.isAssignableFrom(componentType)) {
                appendToInner(context, createFromFiles(context, resources.cast()));
            } else {
                throw new IllegalArgumentException("Unsupported source type " + componentType);
            }
        } else {
            throw new IllegalArgumentException("Unsupported source type " + componentType.getName() + " provided to " + this.getClass().getName());
        }
    }

    private <C extends RuntimeContext<C>> void appendToInner(RuntimeContext<C> context, ResourceClasses resourceClasses) throws IOException {
        if(resourceClasses != null) {
            try {
                Collection<RulesClass> rulesClasses = new ArrayList<>(resourceClasses.classes.size());
                for (Class<?> cl : resourceClasses.classes) {
                    WrappedClass wrappedClass = new WrappedClass(cl, publicLookup);
                    RulesClass rulesClass = new RulesClass(wrappedClass);
                    rulesClasses.add(rulesClass);
                }
                buildAndAppendRules(context, resourceClasses.classLoader, rulesClasses);
            } finally {
                resourceClasses.closeResources();
            }
        } else {
            LOGGER.warning("No resources were selected, no rules will be applied");
        }
    }

    private <C extends RuntimeContext<C>> void buildAndAppendRules(RuntimeContext<C> context, ClassLoader classLoader, Collection<RulesClass> rulesClasses) {
        // 1. Collect required data (first pass)
        MetadataCollector initMeta = new MetadataCollector();
        for (RulesClass rulesClass : rulesClasses) {
            rulesClass.collectMetaData(context, initMeta);
        }

        RuleSetBuilder<C> target = context.builder(classLoader);

        // 2. Build the rules (second pass)
        for(RulesClass rulesClass : rulesClasses) {
            rulesClass.applyTo(target, initMeta);
        }
        target.build();


        // 3. Session binding
        if(!initMeta.classesToInstantiate.isEmpty()) {
            if(context instanceof RuleSession) {
                // This context is a session. Create class instances and rebind method handles.
                initMeta.applyToSession((RuleSession<?>) context);
            } else {
                // This context is a knowledge instance. We need to create class instances each
                // time a new session is created.
                Events.Subscription subscription = context.subscribe(
                        SessionCreatedEvent.class,
                        event -> initMeta.applyToSession(event.getSession())
                );
                context.getService().getServiceSubscriptions().add(subscription);
            }
        }
    }

    static Charset charset(Configuration configuration) {
        String charSet = configuration.getProperty(CHARSET_PROPERTY, CHARSET_DEFAULT);
        return Charset.forName(charSet);
    }

    static boolean matches(Class<?> clazz, Set<Class<?>> set) {
        for(Class<?> c : set) {
            if(c.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    static Collection<URL> toURLs(Collection<File> files) throws IOException {
        Collection<URL> urls = new ArrayList<>();
        for (File file : files) {
            urls.add(file.toURI().toURL());
        }
        return urls;
    }

}


