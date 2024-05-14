package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeContext;
import org.evrete.util.CompilationException;
import org.evrete.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
            TYPE_CHAR_SEQUENCE
    };
    /**
     * Default public constructor
     */
    public DSLSourceProvider() {
    }

    @Override
    public Optional<Class<?>[]> sourceTypes() {
        return Optional.of(SUPPORTED_TYPES);
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, URL[] urls) throws IOException {
        // Despite the fact that we need to return a stream,
        // the resources may contain interdependent sources,
        // so we read them all at once.
        Charset charset = charset(context.getConfiguration());
        return sourceClasses(context, toSourceStrings(charset, urls));
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, Reader[] readers) throws IOException {
        // Despite the fact that we need to return a stream,
        // the resources may contain interdependent sources,
        // so we read them all at once.
        return sourceClasses(context, toSourceStrings(readers));
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, CharSequence[] strings) {
        // Despite the fact that we need to return a stream,
        // the resources may contain interdependent sources,
        // so we read them all at once.
        try {
            JavaSourceCompiler compiler = context.getSourceCompiler();
            List<JavaSourceCompiler.ClassSource> sources = new ArrayList<>(strings.length);
            for (CharSequence string : strings) {
                sources.add(compiler.resolve(string.toString()));
            }
            return compiler
                    .compile(sources)
                    .stream()
                    .map(JavaSourceCompiler.Result::getCompiledClass);
        } catch (CompilationException e) {
            e.log(LOGGER, Level.SEVERE);
            throw new IllegalArgumentException("Failed to compile Java source code", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, InputStream[] inputStreams) throws IOException {
        // Despite the fact that we need to return a stream,
        // the resources may contain interdependent sources,
        // so we read them all at once.
        Charset charset = charset(context.getConfiguration());
        return sourceClasses(context, toSourceStrings(charset, inputStreams));
    }


    @Override
    public String getName() {
        return PROVIDER_JAVA_SOURCE;
    }

    @Override
    public Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        String[] sources = toSourceStrings(charset(service.getConfiguration()), streams);
        Knowledge knowledge = service.newKnowledge();
        return knowledge.builder()
                .importAllRules(this, sources)
                .build();
    }

    @Override
    public Knowledge create(KnowledgeService service, URL... resources) throws IOException {
        Knowledge knowledge = service.newKnowledge();
        return knowledge
                .builder()
                .importAllRules(this, resources)
                .build();
    }

    @Override
    public Knowledge create(KnowledgeService service, Reader... readers) throws IOException {
        Knowledge knowledge = service.newKnowledge();
        String[] sources = toSourceStrings(readers);
        return knowledge
                .builder()
                .importAllRules(this, sources)
                .build();
    }


    private static String[] toSourceStrings(Reader[] readers) throws IOException {
        return IOUtils.read(String.class, readers, r -> r, IOUtils::toString);
    }

    private static String[] toSourceStrings(Charset charset, InputStream... streams) throws IOException {
        return IOUtils.read(String.class, streams, i -> i, i -> new String(IOUtils.toByteArrayChecked(i), charset));
    }

    private static String[] toSourceStrings(Charset charset, URL... urls) throws IOException {
        return IOUtils.read(String.class, urls, URL::openStream, i -> new String(IOUtils.toByteArrayChecked(i), charset));
    }

}
