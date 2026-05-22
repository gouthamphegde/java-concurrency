# Functional Programming in Java 21 - Complete Learning Guide

> A practical, hands-on guide to functional programming in Java: lambdas, functional interfaces, method references, streams, collectors, Optional, immutability, records, sealed types, pattern matching, and functional design.

---

## Table of Contents

1. [Functional Programming Mindset](#1-functional-programming-mindset)
2. [Lambda Expressions](#2-lambda-expressions)
3. [Functional Interfaces](#3-functional-interfaces)
4. [Method References](#4-method-references)
5. [Streams](#5-streams)
6. [Collectors](#6-collectors)
7. [Optional](#7-optional)
8. [Immutability](#8-immutability)
9. [Records](#9-records)
10. [Sealed Types and Pattern Matching](#10-sealed-types-and-pattern-matching)
11. [Functional Error Handling](#11-functional-error-handling)
12. [Functional Composition](#12-functional-composition)
13. [Parallel Streams](#13-parallel-streams)
14. [Common Patterns](#14-common-patterns)
15. [Anti-Patterns](#15-anti-patterns)
16. [Exercises](#16-exercises)
17. [Projects](#17-projects)
18. [Quick Reference Cheat Sheet](#18-quick-reference-cheat-sheet)

---

<a id="1-functional-programming-mindset"></a>
## 1. Functional Programming Mindset

Functional programming is a style of writing programs by combining functions and values instead of mutating shared state step by step.

Java is not a purely functional language, but modern Java supports many functional techniques.

### 1.1 Core Ideas

| Idea | Meaning |
|------|---------|
| Function as value | Pass behavior into methods using lambdas or method references |
| Pure function | Same input always gives same output, with no side effects |
| Immutability | Prefer values that do not change after creation |
| Declarative code | Describe what should happen, not every control-flow step |
| Composition | Build larger behavior by combining smaller functions |
| Lazy evaluation | Delay work until the result is actually needed |

### 1.2 Imperative vs Functional Style

Imperative style:

```java
List<String> activeNames = new ArrayList<>();

for (User user : users) {
    if (user.active()) {
        activeNames.add(user.name().toUpperCase());
    }
}
```

Functional style:

```java
List<String> activeNames = users.stream()
        .filter(User::active)
        .map(User::name)
        .map(String::toUpperCase)
        .toList();
```

The functional version reads as a pipeline:

1. Start with users.
2. Keep active users.
3. Extract names.
4. Convert names to uppercase.
5. Collect the result.

### 1.3 When Functional Java Helps

- Transforming collections
- Filtering data
- Aggregating values
- Building validation pipelines
- Removing repetitive loops
- Expressing business rules as composable functions
- Writing thread-friendly code with immutable data

### 1.4 When Not to Force It

Functional style is not automatically better. A normal loop may be clearer when:

- The logic has many branches
- The operation requires early exit with complex state
- The pipeline has side effects
- Debugging each step is difficult
- Performance requires tight control over allocations

Good Java code can mix object-oriented, imperative, and functional styles.

---

<a id="2-lambda-expressions"></a>
## 2. Lambda Expressions

A lambda expression is an anonymous function. It lets you pass behavior as data.

### 2.1 Basic Syntax

```java
parameter -> expression
```

Examples:

```java
x -> x * 2

(a, b) -> a + b

name -> name.toUpperCase()

() -> System.out.println("Hello")
```

Block body:

```java
name -> {
    String trimmed = name.trim();
    return trimmed.toUpperCase();
}
```

### 2.2 Lambdas Need a Target Type

A lambda does not exist by itself. Java assigns it to a functional interface.

```java
Predicate<String> isBlank = value -> value.isBlank();
Function<String, Integer> length = value -> value.length();
Consumer<String> printer = value -> System.out.println(value);
Supplier<Instant> clock = () -> Instant.now();
```

### 2.3 Capturing Variables

Lambdas can read local variables only if they are final or effectively final.

```java
String prefix = "USER-";

Function<String, String> addPrefix = id -> prefix + id;
```

This is allowed because `prefix` is not changed after assignment.

This is not allowed:

```java
int count = 0;

users.forEach(user -> count++); // compile error
```

Use stream operations instead:

```java
long count = users.stream()
        .filter(User::active)
        .count();
```

### 2.4 Side Effects

A side effect is any change outside the function:

- Mutating a list
- Updating a field
- Printing to the console
- Writing to a file
- Calling a database

Side effects are sometimes necessary, but keep them at the edges of the program.

Prefer this:

```java
List<String> emails = users.stream()
        .filter(User::active)
        .map(User::email)
        .toList();

emailService.sendAll(emails);
```

Avoid this when possible:

```java
users.stream()
        .filter(User::active)
        .forEach(user -> emailService.send(user.email()));
```

---

<a id="3-functional-interfaces"></a>
## 3. Functional Interfaces

A functional interface has exactly one abstract method. Lambdas and method references are assigned to functional interfaces.

### 3.1 Common Built-In Interfaces

| Interface | Method | Use |
|-----------|--------|-----|
| `Predicate<T>` | `boolean test(T value)` | Check a condition |
| `Function<T, R>` | `R apply(T value)` | Transform a value |
| `Consumer<T>` | `void accept(T value)` | Perform an action |
| `Supplier<T>` | `T get()` | Provide a value |
| `UnaryOperator<T>` | `T apply(T value)` | Transform T to T |
| `BinaryOperator<T>` | `T apply(T a, T b)` | Combine two T values |
| `BiFunction<T, U, R>` | `R apply(T a, U b)` | Transform two inputs |
| `BiPredicate<T, U>` | `boolean test(T a, U b)` | Check two inputs |
| `BiConsumer<T, U>` | `void accept(T a, U b)` | Act on two inputs |

### 3.2 Predicate

```java
Predicate<User> isActive = User::active;
Predicate<User> hasCompanyEmail = user -> user.email().endsWith("@company.com");

List<User> internalActiveUsers = users.stream()
        .filter(isActive.and(hasCompanyEmail))
        .toList();
```

Useful methods:

```java
predicate.and(other)
predicate.or(other)
predicate.negate()
Predicate.not(User::active)
```

### 3.3 Function

```java
Function<User, String> toEmail = User::email;
Function<String, String> normalize = email -> email.trim().toLowerCase();

Function<User, String> normalizedEmail = toEmail.andThen(normalize);
```

Composition order:

```java
f.andThen(g)  // first f, then g
f.compose(g)  // first g, then f
```

### 3.4 Consumer

```java
Consumer<String> print = System.out::println;
Consumer<String> audit = message -> auditLog.add(message);

Consumer<String> printAndAudit = print.andThen(audit);
```

Use `Consumer` for side effects. Do not use it for transformations.

### 3.5 Supplier

```java
Supplier<UUID> ids = UUID::randomUUID;
Supplier<Instant> now = Instant::now;
```

Suppliers are useful for lazy values:

```java
String value = cache.getOrDefault(key, expensiveDefault.get()); // not lazy
```

Better:

```java
String value = cache.computeIfAbsent(key, ignored -> expensiveDefault.get());
```

### 3.6 Primitive Specializations

Use primitive interfaces to avoid boxing when performance matters.

| Interface | Use |
|-----------|-----|
| `IntPredicate` | `int -> boolean` |
| `IntFunction<R>` | `int -> R` |
| `IntConsumer` | `int -> void` |
| `IntSupplier` | `() -> int` |
| `IntUnaryOperator` | `int -> int` |
| `IntBinaryOperator` | `(int, int) -> int` |
| `ToIntFunction<T>` | `T -> int` |

Example:

```java
int totalLength = names.stream()
        .mapToInt(String::length)
        .sum();
```

### 3.7 Creating Your Own Functional Interface

Use `@FunctionalInterface` for clarity and compiler checking.

```java
@FunctionalInterface
interface DiscountPolicy {
    BigDecimal apply(BigDecimal subtotal);
}
```

Usage:

```java
DiscountPolicy tenPercentOff = subtotal -> subtotal.multiply(new BigDecimal("0.90"));
```

Prefer built-in interfaces unless a domain-specific name improves readability.

---

<a id="4-method-references"></a>
## 4. Method References

Method references are shorthand for lambdas that call an existing method.

### 4.1 Types of Method References

| Form | Example | Equivalent Lambda |
|------|---------|-------------------|
| Static method | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| Instance method on object | `printer::print` | `s -> printer.print(s)` |
| Instance method on parameter | `String::trim` | `s -> s.trim()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

### 4.2 Examples

```java
List<Integer> numbers = strings.stream()
        .map(Integer::parseInt)
        .toList();
```

```java
List<String> sorted = names.stream()
        .map(String::trim)
        .filter(Predicate.not(String::isBlank))
        .sorted(String::compareToIgnoreCase)
        .toList();
```

```java
Map<String, User> byEmail = users.stream()
        .collect(Collectors.toMap(User::email, Function.identity()));
```

### 4.3 Lambda vs Method Reference

Use a method reference when it is immediately clear.

Good:

```java
.map(User::email)
```

Better as lambda:

```java
.filter(user -> user.active() && user.email().endsWith("@company.com"))
```

---

<a id="5-streams"></a>
## 5. Streams

A stream is a pipeline for processing values. It does not store data. It describes work to perform over a data source.

### 5.1 Stream Pipeline

```java
List<String> result = users.stream()
        .filter(User::active)          // intermediate
        .map(User::email)              // intermediate
        .sorted()                      // intermediate
        .toList();                     // terminal
```

Stream operations are either:

- Intermediate: return another stream, lazy, not executed immediately
- Terminal: produce a result or side effect, triggers the pipeline

### 5.2 Common Intermediate Operations

| Operation | Purpose |
|-----------|---------|
| `filter` | Keep matching values |
| `map` | Transform each value |
| `flatMap` | Transform and flatten nested streams |
| `distinct` | Remove duplicates |
| `sorted` | Sort values |
| `limit` | Keep first N values |
| `skip` | Skip first N values |
| `peek` | Inspect values, usually for debugging |
| `takeWhile` | Keep values while condition is true |
| `dropWhile` | Drop values while condition is true |

### 5.3 Common Terminal Operations

| Operation | Purpose |
|-----------|---------|
| `toList` | Collect to unmodifiable list |
| `collect` | Collect using a collector |
| `forEach` | Perform side effects |
| `count` | Count values |
| `reduce` | Combine values |
| `min` / `max` | Find smallest or largest |
| `anyMatch` | True if any value matches |
| `allMatch` | True if all values match |
| `noneMatch` | True if no values match |
| `findFirst` | First value as Optional |
| `findAny` | Any value as Optional |

### 5.4 Map

Use `map` for one-to-one transformations.

```java
List<String> emails = users.stream()
        .map(User::email)
        .toList();
```

### 5.5 Filter

Use `filter` to keep values that match a condition.

```java
List<Order> largeOrders = orders.stream()
        .filter(order -> order.total().compareTo(new BigDecimal("1000")) > 0)
        .toList();
```

### 5.6 FlatMap

Use `flatMap` when each input produces multiple outputs.

```java
List<String> allTags = articles.stream()
        .flatMap(article -> article.tags().stream())
        .distinct()
        .sorted()
        .toList();
```

Without `flatMap`, the result would be a stream of lists.

### 5.7 Reduce

Use `reduce` to combine values into one value.

```java
int total = numbers.stream()
        .reduce(0, Integer::sum);
```

For common numeric operations, prefer primitive streams:

```java
int total = numbers.stream()
        .mapToInt(Integer::intValue)
        .sum();
```

### 5.8 Short-Circuiting

Some operations can stop early.

```java
boolean hasAdmin = users.stream()
        .anyMatch(user -> user.roles().contains("ADMIN"));
```

Short-circuiting operations include:

- `anyMatch`
- `allMatch`
- `noneMatch`
- `findFirst`
- `findAny`
- `limit`
- `takeWhile`

### 5.9 Stream Laziness

Intermediate operations do not run until a terminal operation is called.

```java
Stream<String> pipeline = names.stream()
        .filter(name -> {
            System.out.println("filtering " + name);
            return name.length() > 3;
        });

// Nothing printed yet.

List<String> result = pipeline.toList();
```

### 5.10 Streams Are Single-Use

```java
Stream<String> stream = names.stream();

long count = stream.count();
List<String> result = stream.toList(); // IllegalStateException
```

Create a new stream for each pipeline.

---

<a id="6-collectors"></a>
## 6. Collectors

Collectors turn stream values into collections, maps, strings, numbers, or grouped results.

### 6.1 List, Set, and Map

```java
List<String> emails = users.stream()
        .map(User::email)
        .toList();
```

```java
Set<String> domains = users.stream()
        .map(user -> user.email().substring(user.email().indexOf('@') + 1))
        .collect(Collectors.toSet());
```

```java
Map<Long, User> byId = users.stream()
        .collect(Collectors.toMap(User::id, Function.identity()));
```

Handle duplicate keys explicitly:

```java
Map<String, User> byEmail = users.stream()
        .collect(Collectors.toMap(
                User::email,
                Function.identity(),
                (first, second) -> first
        ));
```

### 6.2 Grouping

```java
Map<Department, List<Employee>> byDepartment = employees.stream()
        .collect(Collectors.groupingBy(Employee::department));
```

Counting by group:

```java
Map<Department, Long> countByDepartment = employees.stream()
        .collect(Collectors.groupingBy(
                Employee::department,
                Collectors.counting()
        ));
```

Mapping values inside a group:

```java
Map<Department, List<String>> namesByDepartment = employees.stream()
        .collect(Collectors.groupingBy(
                Employee::department,
                Collectors.mapping(Employee::name, Collectors.toList())
        ));
```

### 6.3 Partitioning

Use `partitioningBy` when there are exactly two groups: true and false.

```java
Map<Boolean, List<User>> activeStatus = users.stream()
        .collect(Collectors.partitioningBy(User::active));
```

### 6.4 Joining Strings

```java
String csv = users.stream()
        .map(User::email)
        .collect(Collectors.joining(", "));
```

### 6.5 Summarizing

```java
IntSummaryStatistics stats = orders.stream()
        .collect(Collectors.summarizingInt(Order::itemCount));

int min = stats.getMin();
int max = stats.getMax();
double average = stats.getAverage();
long count = stats.getCount();
```

### 6.6 Collecting and Then

```java
Set<String> immutableDomains = users.stream()
        .map(User::email)
        .map(email -> email.substring(email.indexOf('@') + 1))
        .collect(Collectors.collectingAndThen(
                Collectors.toSet(),
                Set::copyOf
        ));
```

### 6.7 `toList()` vs `Collectors.toList()`

| Code | Result |
|------|--------|
| `stream.toList()` | Unmodifiable list |
| `stream.collect(Collectors.toList())` | Mutable list currently, but not guaranteed by the contract |
| `stream.collect(Collectors.toCollection(ArrayList::new))` | Explicit mutable ArrayList |

Use `toList()` when you do not need to mutate the result.

---

<a id="7-optional"></a>
## 7. Optional

`Optional<T>` represents a value that may or may not be present.

It is most useful as a method return type.

### 7.1 Creating Optional Values

```java
Optional<String> present = Optional.of("value");
Optional<String> empty = Optional.empty();
Optional<String> maybe = Optional.ofNullable(possiblyNullValue);
```

Use `of` when null is a bug. Use `ofNullable` when null is expected input.

### 7.2 Reading Optional Values

Prefer transformation methods:

```java
String displayName = userRepository.findById(id)
        .map(User::displayName)
        .orElse("Unknown user");
```

Lazy default:

```java
User user = userRepository.findById(id)
        .orElseGet(() -> userRepository.createGuestUser(id));
```

Throwing:

```java
User user = userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
```

### 7.3 `map` vs `flatMap`

Use `map` when the function returns a normal value.

```java
Optional<String> email = optionalUser.map(User::email);
```

Use `flatMap` when the function already returns an Optional.

```java
Optional<Address> address = optionalUser.flatMap(User::primaryAddress);
```

### 7.4 Optional and Streams

```java
List<User> foundUsers = ids.stream()
        .map(userRepository::findById)
        .flatMap(Optional::stream)
        .toList();
```

### 7.5 Optional Anti-Patterns

Avoid this:

```java
if (optionalUser.isPresent()) {
    return optionalUser.get().email();
}
return "unknown";
```

Prefer this:

```java
return optionalUser
        .map(User::email)
        .orElse("unknown");
```

Avoid using Optional for:

- Fields in entities or DTOs
- Method parameters
- Collection elements
- Serialization contracts

Use it primarily for return values.

---

<a id="8-immutability"></a>
## 8. Immutability

Immutable objects do not change after creation. Functional code becomes easier to reason about when values are immutable.

### 8.1 Benefits

- Fewer hidden side effects
- Safer sharing across threads
- Easier testing
- Easier caching
- Easier debugging

### 8.2 Immutable Class Example

```java
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(amount.add(other.amount), currency);
    }
}
```

The `add` method returns a new `Money` value instead of changing the current object.

### 8.3 Defensive Copies

Collections are mutable unless protected.

```java
public record Cart(List<CartLine> lines) {
    public Cart {
        lines = List.copyOf(lines);
    }
}
```

### 8.4 Immutable Updates

```java
public record User(long id, String name, String email, boolean active) {
    public User deactivate() {
        return new User(id, name, email, false);
    }
}
```

Instead of mutating:

```java
user.setActive(false);
```

Return a changed copy:

```java
User inactive = user.deactivate();
```

---

<a id="9-records"></a>
## 9. Records

Records are compact immutable data carriers.

```java
public record User(long id, String name, String email, boolean active) {
}
```

Java automatically provides:

- Private final fields
- Constructor
- Accessor methods
- `equals`
- `hashCode`
- `toString`

### 9.1 Compact Constructor

```java
public record EmailAddress(String value) {
    public EmailAddress {
        value = value.trim().toLowerCase();

        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}
```

### 9.2 Records in Stream Pipelines

```java
record DepartmentReport(String department, long activeEmployees) {
}

List<DepartmentReport> reports = employees.stream()
        .collect(Collectors.groupingBy(
                Employee::department,
                Collectors.filtering(Employee::active, Collectors.counting())
        ))
        .entrySet()
        .stream()
        .map(entry -> new DepartmentReport(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(DepartmentReport::department))
        .toList();
```

### 9.3 Records Are Shallowly Immutable

This record is not fully immutable:

```java
public record Team(List<String> members) {
}
```

The list can still be mutated by outside code. Fix it with `List.copyOf`:

```java
public record Team(List<String> members) {
    public Team {
        members = List.copyOf(members);
    }
}
```

---

<a id="10-sealed-types-and-pattern-matching"></a>
## 10. Sealed Types and Pattern Matching

Sealed types let you define a closed set of implementations. Pattern matching lets you work with those types safely and clearly.

### 10.1 Sealed Interface

```java
sealed interface PaymentResult permits PaymentResult.Success, PaymentResult.Failed {
    record Success(String transactionId) implements PaymentResult {
    }

    record Failed(String reason) implements PaymentResult {
    }
}
```

### 10.2 Pattern Matching for `switch`

```java
String message = switch (result) {
    case PaymentResult.Success success -> "Paid: " + success.transactionId();
    case PaymentResult.Failed failed -> "Failed: " + failed.reason();
};
```

Because `PaymentResult` is sealed, Java can check whether the switch covers all possible cases.

### 10.3 Functional Domain Modeling

Sealed types are useful for modeling alternatives without nulls or string status codes.

Instead of this:

```java
record Response(boolean success, String data, String error) {
}
```

Use this:

```java
sealed interface Response permits Response.Success, Response.Error {
    record Success(String data) implements Response {
    }

    record Error(String message) implements Response {
    }
}
```

This makes invalid states impossible, such as `success=true` with an error message.

---

<a id="11-functional-error-handling"></a>
## 11. Functional Error Handling

Java functional interfaces do not allow checked exceptions by default.

### 11.1 Checked Exception Problem

This does not compile if `parseFile` throws `IOException`:

```java
List<Document> documents = paths.stream()
        .map(path -> parseFile(path))
        .toList();
```

### 11.2 Handle Exceptions Inside the Lambda

```java
List<Document> documents = paths.stream()
        .map(path -> {
            try {
                return parseFile(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        })
        .toList();
```

This is often the simplest practical Java solution.

### 11.3 Return a Result Type

For domain-level failures, model success and failure explicitly.

```java
sealed interface ParseResult permits ParseResult.Success, ParseResult.Failure {
    record Success(Document document) implements ParseResult {
    }

    record Failure(Path path, String message) implements ParseResult {
    }
}
```

```java
ParseResult parseSafely(Path path) {
    try {
        return new ParseResult.Success(parseFile(path));
    } catch (IOException e) {
        return new ParseResult.Failure(path, e.getMessage());
    }
}
```

```java
Map<Boolean, List<ParseResult>> results = paths.stream()
        .map(this::parseSafely)
        .collect(Collectors.partitioningBy(result -> result instanceof ParseResult.Success));
```

### 11.4 Do Not Hide Important Failures

Avoid turning failures into empty values unless absence is truly valid.

Risky:

```java
Optional<Document> parseMaybe(Path path) {
    try {
        return Optional.of(parseFile(path));
    } catch (IOException e) {
        return Optional.empty();
    }
}
```

Better when the caller needs failure details:

```java
ParseResult parseSafely(Path path) {
    // return Success or Failure with details
}
```

---

<a id="12-functional-composition"></a>
## 12. Functional Composition

Composition means building larger functions from smaller functions.

### 12.1 Compose Functions

```java
Function<String, String> trim = String::trim;
Function<String, String> lower = String::toLowerCase;
Function<String, EmailAddress> toEmail = EmailAddress::new;

Function<String, EmailAddress> normalizeEmail = trim
        .andThen(lower)
        .andThen(toEmail);
```

### 12.2 Compose Predicates

```java
Predicate<Order> isPaid = order -> order.status() == OrderStatus.PAID;
Predicate<Order> isLarge = order -> order.total().compareTo(new BigDecimal("500")) >= 0;
Predicate<Order> requiresReview = isPaid.and(isLarge);
```

### 12.3 Compose Comparators

```java
Comparator<Employee> byDepartmentThenName = Comparator
        .comparing(Employee::department)
        .thenComparing(Employee::lastName)
        .thenComparing(Employee::firstName);
```

### 12.4 Strategy as Function

Instead of creating many small strategy classes, simple strategies can be functions.

```java
record PricingRule(String name, UnaryOperator<BigDecimal> apply) {
}
```

```java
List<PricingRule> rules = List.of(
        new PricingRule("Black Friday", price -> price.multiply(new BigDecimal("0.70"))),
        new PricingRule("Member", price -> price.multiply(new BigDecimal("0.95")))
);

BigDecimal finalPrice = rules.stream()
        .map(PricingRule::apply)
        .reduce(UnaryOperator.identity(), UnaryOperator::andThen)
        .apply(originalPrice);
```

---

<a id="13-parallel-streams"></a>
## 13. Parallel Streams

Parallel streams split work across the common ForkJoinPool.

```java
List<Result> results = inputs.parallelStream()
        .map(this::expensiveCpuOperation)
        .toList();
```

### 13.1 Good Use Cases

- CPU-heavy operations
- Large input collections
- Independent elements
- No shared mutable state
- No blocking I/O

### 13.2 Bad Use Cases

- Small collections
- Database calls
- HTTP calls
- File I/O-heavy tasks
- Operations requiring order-sensitive mutation
- Code running inside application servers where common pool usage is risky

### 13.3 Avoid Shared Mutation

Bad:

```java
List<String> result = new ArrayList<>();

users.parallelStream()
        .map(User::email)
        .forEach(result::add); // unsafe
```

Good:

```java
List<String> result = users.parallelStream()
        .map(User::email)
        .toList();
```

### 13.4 Measure Before Keeping

Parallel streams can be slower because of splitting, scheduling, and merging overhead.

Use them only after measuring with realistic data.

---

<a id="14-common-patterns"></a>
## 14. Common Patterns

### 14.1 Filtering and Mapping

```java
List<String> activeEmails = users.stream()
        .filter(User::active)
        .map(User::email)
        .toList();
```

### 14.2 Index by ID

```java
Map<Long, User> usersById = users.stream()
        .collect(Collectors.toMap(User::id, Function.identity()));
```

### 14.3 Group by Field

```java
Map<String, List<User>> usersByCountry = users.stream()
        .collect(Collectors.groupingBy(User::country));
```

### 14.4 Find First Match

```java
Optional<User> admin = users.stream()
        .filter(user -> user.roles().contains("ADMIN"))
        .findFirst();
```

### 14.5 Validate All Rules

```java
record ValidationRule<T>(String message, Predicate<T> valid) {
}
```

```java
List<ValidationRule<User>> rules = List.of(
        new ValidationRule<>("Name is required", user -> !user.name().isBlank()),
        new ValidationRule<>("Email is invalid", user -> user.email().contains("@"))
);

List<String> errors = rules.stream()
        .filter(rule -> !rule.valid().test(user))
        .map(ValidationRule::message)
        .toList();
```

### 14.6 Transform Nested Data

```java
List<OrderLine> lines = orders.stream()
        .flatMap(order -> order.lines().stream())
        .toList();
```

### 14.7 Top N Values

```java
List<Product> topProducts = products.stream()
        .sorted(Comparator.comparing(Product::sales).reversed())
        .limit(10)
        .toList();
```

### 14.8 Null-Safe Stream from Collection

```java
Stream<User> stream = users == null ? Stream.empty() : users.stream();
```

Better: avoid null collections. Return empty collections instead.

---

<a id="15-anti-patterns"></a>
## 15. Anti-Patterns

### 15.1 Side Effects in `map`

Bad:

```java
users.stream()
        .map(user -> {
            audit(user);
            return user.email();
        })
        .toList();
```

Better:

```java
List<String> emails = users.stream()
        .map(User::email)
        .toList();

users.forEach(this::audit);
```

### 15.2 Using `forEach` to Build Collections

Bad:

```java
List<String> emails = new ArrayList<>();
users.stream().forEach(user -> emails.add(user.email()));
```

Good:

```java
List<String> emails = users.stream()
        .map(User::email)
        .toList();
```

### 15.3 Overly Long Pipelines

Bad:

```java
var result = orders.stream()
        .filter(...)
        .map(...)
        .flatMap(...)
        .collect(...)
        .entrySet()
        .stream()
        .filter(...)
        .map(...)
        .sorted(...)
        .toList();
```

Better: split into named intermediate values or methods.

### 15.4 Ignoring Duplicate Keys

This throws if two users share an email:

```java
Map<String, User> byEmail = users.stream()
        .collect(Collectors.toMap(User::email, Function.identity()));
```

Handle duplicates deliberately:

```java
Map<String, User> byEmail = users.stream()
        .collect(Collectors.toMap(
                User::email,
                Function.identity(),
                (first, second) -> first
        ));
```

### 15.5 Misusing Optional

Bad:

```java
void send(Optional<User> user) {
}
```

Better:

```java
void send(User user) {
}
```

Callers can decide whether to call the method:

```java
optionalUser.ifPresent(this::send);
```

### 15.6 Complex Logic Hidden in Lambdas

Bad:

```java
.filter(user -> user.active() && !user.locked() && user.roles().stream().anyMatch(role -> role.startsWith("ADMIN")))
```

Better:

```java
.filter(this::canAccessAdminArea)
```

---

<a id="16-exercises"></a>
## 16. Exercises

### Exercise 1: Basic Lambdas

Given a list of names:

1. Remove blank names.
2. Trim whitespace.
3. Convert names to title case or uppercase.
4. Sort alphabetically.
5. Return the result as a list.

### Exercise 2: User Filtering

Create a `User` record with:

- `id`
- `name`
- `email`
- `active`
- `roles`

Write stream pipelines to:

- Find active users
- Find users with an admin role
- Extract unique email domains
- Count active vs inactive users

### Exercise 3: Grouping Orders

Create records:

```java
record Order(long id, String customerId, BigDecimal total, OrderStatus status) {
}

enum OrderStatus {
    NEW, PAID, SHIPPED, CANCELLED
}
```

Use collectors to calculate:

- Orders by status
- Total revenue from paid orders
- Number of orders per customer
- Highest value order per customer

### Exercise 4: Optional Refactor

Refactor null-heavy code into Optional-returning methods.

Start with:

```java
User user = repository.findUser(id);
if (user != null && user.email() != null) {
    return user.email().toLowerCase();
}
return "unknown";
```

Target style:

```java
return repository.findById(id)
        .map(User::email)
        .map(String::toLowerCase)
        .orElse("unknown");
```

### Exercise 5: Validation Rules

Build a reusable validation engine using `Predicate<T>`.

Requirements:

- A rule has a message and a predicate.
- The validator returns all failed rule messages.
- Rules can be composed.

### Exercise 6: Result Type

Create a sealed `Result<T>` type:

```java
sealed interface Result<T> permits Result.Success, Result.Failure {
    record Success<T>(T value) implements Result<T> {
    }

    record Failure<T>(String message) implements Result<T> {
    }
}
```

Add helper methods:

- `map`
- `flatMap`
- `orElse`
- `isSuccess`

### Exercise 7: Parallel Stream Experiment

Create a CPU-heavy calculation and compare:

- Sequential stream
- Parallel stream
- Plain loop

Measure runtime with realistic input sizes.

---

<a id="17-projects"></a>
## 17. Projects

### Project 1: Functional CSV Analyzer

Build a small CSV analyzer using records and streams.

Features:

- Read rows from a CSV file
- Parse rows into records
- Validate rows using predicates
- Group valid rows by category
- Produce summary statistics
- Report invalid rows with reasons

### Project 2: Order Pricing Engine

Build a pricing engine using function composition.

Features:

- Base price calculation
- Discount rules as functions
- Tax rules as functions
- Validation rules as predicates
- Final invoice as an immutable record

### Project 3: User Search API

Build an in-memory search service.

Features:

- Optional filters for role, country, status, and email domain
- Dynamic predicate composition
- Sorting with composed comparators
- Pagination with `skip` and `limit`
- Summary counts using collectors

### Project 4: Functional Log Processor

Build a log processing tool.

Features:

- Parse log lines into records
- Use sealed types for parse success/failure
- Group errors by type
- Find top N endpoints
- Summarize request durations

---

<a id="18-quick-reference-cheat-sheet"></a>
## 18. Quick Reference Cheat Sheet

### Lambdas

```java
x -> x + 1
(a, b) -> a + b
() -> Instant.now()
value -> {
    return value.trim();
}
```

### Method References

```java
String::trim
Integer::parseInt
System.out::println
ArrayList::new
```

### Functional Interfaces

```java
Predicate<T>       // T -> boolean
Function<T, R>     // T -> R
Consumer<T>        // T -> void
Supplier<T>        // () -> T
UnaryOperator<T>   // T -> T
BinaryOperator<T>  // (T, T) -> T
```

### Stream Pipeline

```java
List<R> result = values.stream()
        .filter(predicate)
        .map(mapper)
        .sorted(comparator)
        .toList();
```

### Collectors

```java
Collectors.toList()
Collectors.toSet()
Collectors.toMap(keyMapper, valueMapper)
Collectors.groupingBy(classifier)
Collectors.partitioningBy(predicate)
Collectors.counting()
Collectors.mapping(mapper, downstream)
Collectors.joining(", ")
```

### Optional

```java
Optional.of(value)
Optional.ofNullable(value)
Optional.empty()

optional.map(fn)
optional.flatMap(fnReturningOptional)
optional.filter(predicate)
optional.orElse(defaultValue)
optional.orElseGet(supplier)
optional.orElseThrow(exceptionSupplier)
optional.ifPresent(consumer)
optional.stream()
```

### Good Defaults

- Prefer `map` for transformations.
- Prefer `filter` for conditions.
- Prefer `flatMap` for nested collections or Optional values.
- Prefer `toList()` for unmodifiable results.
- Prefer `List.copyOf`, `Set.copyOf`, and `Map.copyOf` for defensive copies.
- Prefer records for immutable data carriers.
- Prefer sealed types for closed alternatives.
- Prefer named methods when lambdas become complex.
- Prefer loops when they are clearer.

### Review Checklist

- Does the pipeline avoid shared mutable state?
- Are side effects isolated to terminal operations or program boundaries?
- Are duplicate map keys handled intentionally?
- Is Optional used as a return type, not as a field or parameter?
- Are collections defensively copied in records?
- Is a stream pipeline still readable after formatting?
- Would a simple loop be clearer?
- Have parallel streams been measured before keeping them?
