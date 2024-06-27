package org.evrete.dsl;

import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.events.ContextEvent;
import org.evrete.api.events.EnvironmentChangeEvent;
import org.evrete.api.events.SessionCreatedEvent;
import org.evrete.runtime.compiler.RuntimeClassloader;
import org.evrete.runtime.compiler.SourceCompiler;
import org.evrete.util.JavaSourceUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class TestUtils {

    public static File testResourceAsFile(String path) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        if(url == null) {
            throw new IOException("Resource not found: " + path);
        } else {
            try {
                return new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
    }

    static synchronized void createTempJarFile(File root, Consumer<File> jarConsumer) throws Exception {
        Path compileRoot = root.toPath();

        SourceCompiler sourceCompiler = new SourceCompiler(new RuntimeClassloader(ClassLoader.getSystemClassLoader()));

        Stream<Path> javaFiles = Files.find(compileRoot, Integer.MAX_VALUE, (path, attrs) -> path.toString().endsWith(".java"));

        Set<String> sources = javaFiles.map(path -> {
            try {
                //noinspection ReadWriteStringCanBeUsed
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).collect(Collectors.toSet());

        Collection<Class<?>> classes = compile(sources);

        File out = File.createTempFile("speakace-test", ".jar");
        FileOutputStream fos = new FileOutputStream(out);
        JarOutputStream  jar = new JarOutputStream(fos);
        for(Class<?> c : classes) {
            String binaryName = c.getName().replaceAll("\\.", "/");
            String name = binaryName + ".class";
            ZipEntry zipEntry = new JarEntry(name);
            jar.putNextEntry(zipEntry);

            assert c.getClassLoader() instanceof RuntimeClassloader;
            InputStream stream = Objects.requireNonNull(c.getClassLoader().getResourceAsStream(name));
            copy(stream, jar);
            stream.close();
            jar.closeEntry();
        }
        jar.close();

        try {
            jarConsumer.accept(out);
        } finally {
            Files.deleteIfExists(out.toPath());
        }
    }


    static Collection<Class<?>> compile(Collection<String> sources) throws Exception {
        SourceCompiler sourceCompiler = new SourceCompiler(new RuntimeClassloader(ClassLoader.getSystemClassLoader()));
        Collection<JavaSourceCompiler.ClassSource> resolved = sources.stream().map(JavaSourceUtils::parse).collect(Collectors.toList());

        Collection<JavaSourceCompiler.Result<JavaSourceCompiler.ClassSource>> compiled =  sourceCompiler.compile(resolved);
        return compiled.stream().map((Function<JavaSourceCompiler.Result<JavaSourceCompiler.ClassSource>, Class<?>>) JavaSourceCompiler.Result::getCompiledClass).collect(Collectors.toList());
    }


    private static void copy(InputStream source, OutputStream sink) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
        }
    }

    public static class EnvHelperData {
        private static final Map<String, List<Object>> data = new HashMap<>();
        private static int count = 0;

        public static void reset() {
            data.clear();
            count = 0;
        }

        public static void add(String property, Object val) {
            data.computeIfAbsent(property, k -> new ArrayList<>()).add(val);
            count++;
        }

        public static void add(EnvironmentChangeEvent e) {
            data.computeIfAbsent(e.getProperty(), k -> new ArrayList<>()).add(e.getValue());
            count++;
        }

        static int total() {
            return count;
        }

        static int total(String property) {
            List<Object> l = data.get(property);
            return l == null ? 0 : l.size();
        }

    }

    public static class PhaseHelperData {
        static final Map<Class<?>, AtomicInteger> EVENTS = new HashMap<>();

        static {
            reset();
        }

        public static void reset() {
            EVENTS.clear();
        }

        public static int count(Class<?> event) {
            int result = 0;
            for(Map.Entry<Class<?>, AtomicInteger> entry : EVENTS.entrySet()) {
                if(event.isAssignableFrom(entry.getKey())) {
                    result += entry.getValue().intValue();
                }
            }
            return result;
        }

        public static void event(ContextEvent... events) {
            for (ContextEvent evt : events) {
                EVENTS.computeIfAbsent(evt.getClass(), k->new AtomicInteger()).incrementAndGet();
            }
        }

        static int total() {
            int total = 0;
            for (AtomicInteger i : EVENTS.values()) {
                total += i.get();
            }
            return total;
        }
    }
}
