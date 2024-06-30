package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.api.events.Events;
import org.evrete.api.spi.SourceCompiler;
import org.evrete.api.RuleSession;
import org.evrete.api.RuntimeContext;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.api.events.SessionCreatedEvent;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.util.CommonUtils;
import org.evrete.util.CompilationException;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
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

    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, URL url) throws IOException {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, Reader reader) throws IOException {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, CharSequence literal) {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, Class<?> clazz) {
        throw new UnsupportedOperationException();
    }

    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected abstract Set<Class<?>> sourceTypes();


    @Override
    public final <C extends RuntimeContext<C>> void appendTo(@NonNull RuleSetBuilder<C> target, Object source) throws IOException {
        if (source != null) {
            try {
                this.appendToInner(target, source);
            } catch (UncheckedIOException e) {
                // Unwrap IO exceptions
                throw e.getCause();
            }
        } else  {
            LOGGER.warning(()->"No sources specified");
        }
    }

    private <C extends RuntimeContext<C>> void appendToInner(RuleSetBuilder<C> target, @NonNull Object source) throws IOException {
        Set<Class<?>> sourceTypes = sourceTypes();
        Collection<?> resolvedSources = CommonUtils.toCollection(source);
        RuntimeContext<C> context = target.getContext();

        // This is a three-step approach:
        // 1. First, we collect metadata from each source
        List<DSLMeta<C>> metaData = new ArrayList<>(resolvedSources.size());
        for (Object singleSource : resolvedSources) {
            if(singleSource == null) {
                LOGGER.warning(()->"Null source detected in " + source);
            } else {
                Class<?> sourceType = singleSource.getClass();
                if(matches(sourceType, sourceTypes)) {
                    if (TYPE_URL.isAssignableFrom(sourceType)) {
                        metaData.addAll(createClassMeta(target, (URL) singleSource));
                    } else if (TYPE_READER.isAssignableFrom(sourceType)) {
                        metaData.addAll(createClassMeta(target, (Reader) singleSource));
                    } else if (TYPE_CHAR_SEQUENCE.isAssignableFrom(sourceType)) {
                        metaData.addAll(createClassMeta(target, (CharSequence) singleSource));
                    } else if (TYPE_INPUT_STREAM.isAssignableFrom(sourceType)) {
                        metaData.addAll(createClassMeta(target, (InputStream) singleSource));
                    } else if (TYPE_CLASS.isAssignableFrom(sourceType)) {
                        metaData.addAll(createClassMeta(target, (Class<?>) singleSource));
                    } else if (TYPE_FILE.isAssignableFrom(sourceType)) {
                        metaData.addAll(createClassMeta(target, (File) singleSource));
                    } else {
                        throw new IllegalArgumentException("Unsupported source type " + sourceType);
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported source type " + sourceType + " provided to " + this.getClass().getName());
                }
            }
        }

        // 2. If the collected data implies compiling Java sources, then compile and apply to the metadata.
        Map<SourceCompiler.ClassSource, DSLMeta<C>> sourceMap = new IdentityHashMap<>();

        for (DSLMeta<C> meta : metaData) {
            SourceCompiler.ClassSource sourceToCompile = meta.sourceToCompile();
            if (sourceToCompile != null) {
                sourceMap.put(sourceToCompile, meta);
            }
        }
        if(!sourceMap.isEmpty()) {

            SourceCompiler sourceCompiler = context.getService().getSourceCompilerProvider().instance(context.getClassLoader());
            try {
                Collection<SourceCompiler.Result<SourceCompiler.ClassSource>> compiledSources = sourceCompiler.compile(sourceMap.keySet());
                for(SourceCompiler.Result<SourceCompiler.ClassSource> result : compiledSources) {
                    DSLMeta<C> meta = sourceMap.get(result.getSource());
                    meta.applyCompiledSource(result.getCompiledClass());
                }
            } catch (CompilationException e) {
                e.log(LOGGER, Level.SEVERE);
                throw new IllegalStateException(e);
            }
        }

        // 3. Collect required data (first pass)
        MetadataCollector initMeta = new MetadataCollector();
        for(DSLMeta<C> meta : metaData) {
            meta.collectMetaData(context, initMeta);
        }

        // 4. Build the rules (second pass)
        for(DSLMeta<C> meta : metaData) {
            meta.applyTo(target, initMeta);
        }

        // 5. Session binding
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

}


