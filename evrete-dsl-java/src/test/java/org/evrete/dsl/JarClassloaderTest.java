package org.evrete.dsl;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.evrete.dsl.TestUtils.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JarClassloaderTest {

    @Test
    void scan() throws Exception {
        File dir = TestUtils.testResourceAsFile("jars/jar2");
        TestUtils.createTempJarFile(dir, file -> {
            try {
                Collection<URL> urls = listOf(file.toURI().toURL());
                doTest(urls);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void doTest(Collection<URL> urls) throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try(JarClassloader classloader = new JarClassloader(urls, parent)) {
            classloader.loadClass("pkg1.evrete.tests.classes.IntValue");
            classloader.loadClass("pkg1.evrete.tests.rule.RuleSet2");

            Enumeration<URL> s = classloader.findResources("pkg1/evrete/tests/rule");
            AtomicInteger found = new AtomicInteger();
            while (s.hasMoreElements()) {
                s.nextElement();
                found.incrementAndGet();
            }

            assertEquals(1, found.get());
        }
    }
}