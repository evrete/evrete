## Running Benchmarks

Launch the following Maven commands from your command line to execute specific benchmark tests:

### Compiled Literal Expressions

This benchmark assesses the performance difference between Java's native code (written as if you manually coded each
condition) and the code produced by the library after compiling literal expressions.

```
./gradlew :evrete-benchmarks:expressions --console=plain
```

The results should show that the compiled code is just 4-5% slower than the native Java implementation.:

```
Benchmark               Mode  Cnt  Score   Error  Units
Expressions.compiled    avgt    8  5.027 ± 0.066  ms/op
Expressions.javaNative  avgt    8  4.822 ± 0.085  ms/op
```

### **Evrete** vs **Drools** Benchmarks

The commands below will produce CSV files suitable for building data charts similar
to the ones shown on https://evrete.org/docs#performance

```
./gradlew :evrete-benchmarks:ruleEngines --console=plain
```
