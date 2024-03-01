## Running Benchmarks

Launch the following Maven commands from your command line to execute specific benchmark tests:

### Compiled Literal Expressions

This benchmark assesses the performance difference between Java's native code (written as if you manually coded each
condition) and the code produced by the library after compiling literal expressions.

```
cd ./evrete-benchmarks
mvn -Dtest=org.evrete.benchmarks.ExpressionsBenchmarks clean test
```

The results should show that the compiled code is approximately 5% slower:

```
Benchmark               Mode  Cnt  Score   Error  Units
Expressions.compiled    avgt    8  5.027 ± 0.066  ms/op
Expressions.javaNative  avgt    8  4.822 ± 0.085  ms/op
```

### Hash Collections Performance

This benchmark measures the library's inner `LinearHashSet` implementation vs Java's `HashSet` for `add`, `contains`,
and scan operations.

```
cd ./evrete-benchmarks
mvn -Dtest=org.evrete.benchmarks.HashCollectionsBenchmarks clean test
```

Sample output of average time per op (the higher, the better)

```
Benchmark                      (set)   Mode  Cnt  Score   Error   Units
HashCollections.contains  LinearHash  thrpt   10  0.356 ± 0.024  ops/ms
HashCollections.contains     HashSet  thrpt   10  0.441 ± 0.013  ops/ms
HashCollections.insert    LinearHash  thrpt   10  0.061 ± 0.013  ops/ms
HashCollections.insert       HashSet  thrpt   10  0.108 ± 0.024  ops/ms
HashCollections.iterator  LinearHash  thrpt   10  0.390 ± 0.011  ops/ms
HashCollections.iterator     HashSet  thrpt   10  0.164 ± 0.047  ops/ms
HashCollections.scan      LinearHash  thrpt   10  0.710 ± 0.053  ops/ms
HashCollections.scan         HashSet  thrpt   10  0.144 ± 0.012  ops/ms
```

### Linked Data Structures

This benchmark measures the library's inner linked list implementation against Java's `LinkedList`.

```
cd ./evrete-benchmarks
mvn -Dtest=org.evrete.benchmarks.LinkedCollections clean test
```

Last output (the higher, the better):

```
Benchmark                  (collection)   Mode  Cnt  Score   Error   Units
ListCollections.insert    LinkedDataRWD  thrpt   10  1.407 ± 0.028  ops/ms
ListCollections.insert     LinkedDataRW  thrpt   10  1.708 ± 0.048  ops/ms
ListCollections.insert       LinkedList  thrpt   10  1.360 ± 0.016  ops/ms
ListCollections.iterator  LinkedDataRWD  thrpt   10  2.732 ± 0.137  ops/ms
ListCollections.iterator   LinkedDataRW  thrpt   10  3.053 ± 0.095  ops/ms
ListCollections.iterator     LinkedList  thrpt   10  2.940 ± 0.231  ops/ms
ListCollections.nested    LinkedDataRWD  thrpt   10  3.403 ± 0.212  ops/ms
ListCollections.nested     LinkedDataRW  thrpt   10  3.395 ± 0.227  ops/ms
ListCollections.nested       LinkedList  thrpt   10  2.056 ± 0.189  ops/ms
```

### **Evrete** vs **Drools** benchmarks

```
cd ./evrete-benchmarks
mvn -Dtest=org.evrete.benchmarks.RuleEngines clean test
```
