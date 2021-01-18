# Evrete

![GitHub repo size](https://img.shields.io/github/repo-size/andbi/evrete)
![GitHub contributors](https://img.shields.io/github/contributors/andbi/evrete)
![GitHub stars](https://img.shields.io/github/stars/andbi/evrete?style=social)

Evrete is a light-weight and intuitive Java rule engine with a set of 
features that make it stand out among other known implementations.


## Project home

The official project description, documentation, and usage examples can be found at https://www.evrete.org

## Prerequisites
Evrete is Java 8+ compatible and ships with zero dependencies.

## Installation

Maven Central Repository

```xml

<dependency>
    <groupId>org.evrete</groupId>
    <artifactId>evrete-core</artifactId>
    <version>1.2.05</version>
</dependency>
```

## Quick start

Below is a simple rule that removes from session memory every integer except prime numbers.

```java
class PrimeNumbers {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .newRule()
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1 * $i2 == $i3")
                .execute(
                        ctx -> {
                            int $i3 = ctx.get("$i3");
                            ctx.delete($i3);
                        }
                )
                .createSession();

        // Inject candidates
        for (int i = 2; i <= 100; i++) {
            session.insert(i);
        }

        // Execute rules
        session.fire();

        // Print current memory state
        session.forEachMemoryObject(System.out::println);

        // Closing resources
        session.close();
        service.shutdown();
    }
}
```

## Contributing to Evrete

To contribute to Evrete, follow the GitHub documentation on
[creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request)
.

## Contact

If you want to contact me you can reach me
at [feedback@evrete.org](mailto:feedback@evrete.org?subject=[GitHub]%20Feedback)

## License
<!--- If you're not sure which open license to use see https://choosealicense.com/--->

This project uses the following license: [MIT](https://opensource.org/licenses/MIT)

