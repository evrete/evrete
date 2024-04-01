package org.evrete.dsl;

import org.evrete.runtime.compiler.RuntimeClassloader;
import org.evrete.runtime.compiler.SourceCompiler;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TestUtils {

    public static void testFile(Object f) {
        new File(f.toString()).exists();
    }

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

        Collection<Class<?>> classes = sourceCompiler.compile(sources).values();

        File out = File.createTempFile("speakace-test", ".jar");
        FileOutputStream fos = new FileOutputStream(out);
        JarOutputStream jar = new JarOutputStream(fos);
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

        static int total() {
            return count;
        }

        static int total(String property) {
            List<Object> l = data.get(property);
            return l == null ? 0 : l.size();
        }

    }

    public static class PhaseHelperData {
        static final EnumMap<Phase, AtomicInteger> EVENTS = new EnumMap<>(Phase.class);

        static {
            reset();
        }

        public static void reset() {
            for (Phase phase : Phase.values()) {
                EVENTS.put(phase, new AtomicInteger());
            }
        }

        public static int count(Phase phase) {
            return EVENTS.get(phase).get();
        }

        public static void event(Phase... phases) {
            for (Phase phase : phases) {
                EVENTS.get(phase).incrementAndGet();
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
