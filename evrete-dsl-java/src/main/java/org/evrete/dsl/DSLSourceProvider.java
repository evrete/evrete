package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.SourceCompiler;
import org.evrete.util.CommonUtils;
import org.evrete.util.CompilationException;
import org.evrete.util.JavaSourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-SOURCE' DSL knowledge.
 */
public class DSLSourceProvider extends AbstractDSLProvider {
    private static final Logger LOGGER = Logger.getLogger(DSLSourceProvider.class.getName());
    private static final Class<?>[] SUPPORTED_TYPES = new Class<?>[] {
            TYPE_INPUT_STREAM,
            TYPE_URL,
            TYPE_READER,
            TYPE_CHAR_SEQUENCE,
            TYPE_FILE
    };
    /**
     * Default public constructor
     */
    public DSLSourceProvider() {
    }

    @Override
    public Set<Class<?>> sourceTypes() {
        return Set.of(SUPPORTED_TYPES);
    }


    @Override
    public String getName() {
        return PROVIDER_JAVA_SOURCE;
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromURLs(RuntimeContext<C> context, Collection<URL> resources) throws IOException {
        Charset charset = charset(context.getConfiguration());
        Collection<String> sources = new ArrayList<>(resources.size());
        for (URL resource : resources) {
            sources.add(toSourceString(charset, resource));
        }
        return createClassMetaFromSource(context, sources);
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromReaders(RuntimeContext<C> context, Collection<Reader> resources) throws IOException {
        Collection<String> sources = new ArrayList<>(resources.size());
        for (Reader reader : resources) {
            sources.add(toSourceString(reader));
        }
        return createClassMetaFromSource(context, sources);
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromStrings(RuntimeContext<C> context, Collection<CharSequence> resources) {
        Collection<String> sources = new ArrayList<>(resources.size());
        for(CharSequence resource : resources) {
            sources.add(resource.toString());
        }
        return createClassMetaFromSource(context, sources);
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromStreams(RuntimeContext<C> context, Collection<InputStream> resources) throws IOException {
        Charset charset = charset(context.getConfiguration());
        Collection<String> sources = new ArrayList<>(resources.size());
        for (InputStream stream : resources) {
            sources.add(toSourceString(charset, stream));
        }
        return this.createClassMetaFromSource(context, sources);
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromFiles(RuntimeContext<C> context, Collection<File> resources) throws IOException {
        return createFromURLs(context, toURLs(resources));
    }

    private <C extends RuntimeContext<C>> ResourceClasses createClassMetaFromSource(RuntimeContext<C> context, Collection<String> sources) {
        // Sources need to be compiled first
        SourceCompiler sourceCompiler = context.getService().getSourceCompilerProvider().instance(context.getClassLoader());
        List<SourceCompiler.ClassSource> compilationUnits = new ArrayList<>(sources.size());
        Map<SourceCompiler.ClassSource, Class<?>> map = new IdentityHashMap<>();
        for (String source : sources) {
            SourceCompiler.ClassSource classSource = JavaSourceUtils.parse(source);
            compilationUnits.add(classSource);
        }
        assert compilationUnits.size() == sources.size();

        try {
            Collection<SourceCompiler.Result<SourceCompiler.ClassSource>> compiled = sourceCompiler.compile(compilationUnits);
            for(SourceCompiler.Result<SourceCompiler.ClassSource> result : compiled) {
                // We're saving the result in the map to maintain the initial order (see the code below)
                map.put(result.getSource(), result.getCompiledClass());
            }

            List<Class<?>> compiledClasses = new ArrayList<>(compiled.size());
            for(SourceCompiler.ClassSource classSource : compilationUnits) {
                Class<?> clazz = map.get(classSource);
                LOGGER.fine(()->"New class has been compiled and selected '" + clazz.getName() + "'");
                compiledClasses.add(clazz);
            }

            if(compiledClasses.isEmpty()) {
                LOGGER.warning("No classes were compiled");
                return null;
            } else {
                ClassLoader classLoader = compiledClasses.iterator().next().getClassLoader();
                return new ResourceClasses(classLoader, compiledClasses);
            }
        } catch (CompilationException e) {
            e.log(LOGGER, Level.SEVERE);
            throw new MalformedResourceException("Unable to compile source(s)", e);
        }
    }

    private static String toSourceString(Reader reader) throws IOException {
        return CommonUtils.toString(reader);
    }

    private static String toSourceString(Charset charset, InputStream stream) throws IOException {
        return new String(CommonUtils.toByteArrayChecked(stream), charset);
    }

    private static String toSourceString(Charset charset, URL url) throws IOException {
        try(InputStream is = url.openStream()) {
            return toSourceString(charset, is);
        }
    }
}
