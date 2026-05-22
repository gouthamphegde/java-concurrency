# Learning-Repo (Java)

This repository turns your guide (`Java21_Concurrency_Complete_Guide.md`) into a runnable Java practice workspace.

## Prerequisites

- JDK 21+
- Maven 3.9+

## Quick start

```bash
mvn clean test
```

Run the basic thread demo:

```bash
mvn -q exec:java -Dexec.mainClass=io.github.gouthamphegde.concurrency.PracticeApp -Dexec.args="threads"
```

Run the virtual thread demo:

```bash
mvn -q exec:java -Dexec.mainClass=io.github.gouthamphegde.concurrency.PracticeApp -Dexec.args="virtual"
```

## Project layout

- `Java21_Concurrency_Complete_Guide.md` - your full concurrency study guide
- `Java21_Concurrency_Complete_Guide_v2.md` - expanded v2 with deeper theory
- `Java_Functional_Programming_Guide.md` - functional programming in Java (lambdas, streams, Optional, records, pattern matching)
- `src/main/java/io/github/gouthamphegde/concurrency` - runnable demos and starter code
- `src/test/java/io/github/gouthamphegde/concurrency` - repeatable concurrency tests
- `exercises/` - place for your own implementations of sections 19 and 20

## Starter content included

- `PracticeApp` command-style runner for quick demos
- `ThreadBasicsExercise` starter for section 2 basics
- `VirtualThreadPing` starter for section 14 virtual threads
- `BrokenCounter` and `SafeCounter` for race condition practice
- `SafeCounterTest` showing latch-based concurrent testing (section 18 style)

## How to practice with the guide

1. Pick a section from the concurrency or functional programming guide.
2. Add one small class in `src/main/java/...` or `exercises/`.
3. Add one test in `src/test/java/...` whenever possible.
4. Run `mvn test` after each change.

## Optional: preview features

For structured concurrency and scoped values examples from sections 15 and 16:

```bash
mvn -q test -DargLine="--enable-preview"
```

For single-file experiments with preview APIs:

```bash
java --enable-preview --source 21 YourExperiment.java
```

