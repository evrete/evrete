# Evrete

![GitHub repo size](https://img.shields.io/github/repo-size/andbi/evrete)
![GitHub stars](https://img.shields.io/github/stars/andbi/evrete?style=social)

Evrete is a forward-chaining Java rule engine that implements the RETE algorithm and is fully compliant with the Java
Rule Engine specification (JSR 94).

Historically designed as a fast and lightweight alternative to full-scale rule management systems, the engine also
brings its own mix of features:

1. Rule authoring

- Rules can be authored both externally and inline as a plain Java 8 code.
- The engine allows rules to be authored as **annotated Java** sources, classes, or archives.
- The library itself is a flexible tool for creating custom domain-specific rule languages (DSL).

2. Intuitive and developer-friendly

- Library's type system allows it to seamlessly process any kinds of objects, including JSON and XML.
- Fluent builders, Java functional interfaces, and other best practices keep developers' code concise and clear.
- Key components are exposed as Service Provider Interfaces and can be customized.

3. Performance and security

- The engine's algorithm and memory are optimized for large-scale and labeled data.
- Built-in support of Java Security Manager protects against unwanted or potentially malicious code in the rules.

## Project home

The official project description, documentation, and usage examples can be found at https://www.evrete.org

## Prerequisites

Evrete is Java 8+ compatible and ships with zero dependencies.

## Installation

Maven Central Repository, core module:

```xml

<dependency>
    <groupId>org.evrete</groupId>
    <artifactId>evrete-core</artifactId>
    <version>2.2.01</version>
</dependency>
```

Support for annotated rules (optional):

```xml

<dependency>
    <groupId>org.evrete</groupId>
    <artifactId>evrete-dsl-java</artifactId>
    <version>2.2.01</version>
</dependency>
```

## Quick start

Below is a simple example of rule that removes from session memory every integer except prime numbers.

As inline Java code:

```java
public class PrimeNumbersInline {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1 * $i2 == $i3")
                .execute(ctx -> ctx.deleteFact("$i3"));

        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }

            // Execute rules
            session.fire();

            // Print current memory state
            session.forEachFact((handle, o) -> System.out.println(o));
        }
        service.shutdown();
    }
}
```

As annotated Java source file:
```java
public class PrimeNumbersDSLUrl {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge(
                        "JAVA-SOURCE",
                        new URL("https://www.evrete.org/examples/PrimeNumbersSource.java")
                );

        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }
            // Execute rules
            session.fire();
            // Printout current memory state
            session.forEachFact((handle, o) -> System.out.println(o));
        }
    }
}
```

where the rule itself is stored externally
as [PrimeNumbersSource.java](https://www.evrete.org/examples/PrimeNumbersSource.java)

For further details see the official [documentation](https://www.evrete.org/docs/)


## License
<!--- If you're not sure which open license to use see https://choosealicense.com/--->

This project uses the following license: [MIT](https://opensource.org/licenses/MIT)

