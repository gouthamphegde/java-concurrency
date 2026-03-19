# Java 21 Concurrency, Multithreading & Parallelism — Complete Learning Guide

> A comprehensive, hands-on guide covering everything from basic threads to Java 21's Virtual Threads, Structured Concurrency, and Scoped Values.

---

## Table of Contents

1. [Foundations: Processes, Threads & Concurrency](#1-foundations)
2. [Creating Threads in Java](#2-creating-threads)
3. [Thread Lifecycle & Control](#3-thread-lifecycle)
4. [Synchronization & Thread Safety](#4-synchronization)
5. [Inter-Thread Communication](#5-inter-thread-communication)
6. [The `java.util.concurrent` Package](#6-java-util-concurrent)
7. [Executor Framework](#7-executor-framework)
8. [Callable, Future & CompletableFuture](#8-callable-future-completablefuture)
9. [Concurrent Collections](#9-concurrent-collections)
10. [Locks, Semaphores & Barriers](#10-locks-semaphores-barriers)
11. [Atomic Variables & CAS](#11-atomic-variables)
12. [Fork/Join Framework](#12-forkjoin-framework)
13. [Parallel Streams](#13-parallel-streams)
14. [Virtual Threads (Project Loom) — Java 21](#14-virtual-threads)
15. [Structured Concurrency — Java 21 Preview](#15-structured-concurrency)
16. [Scoped Values — Java 21 Preview](#16-scoped-values)
17. [Common Concurrency Patterns](#17-concurrency-patterns)
18. [Debugging & Testing Concurrent Code](#18-debugging-testing)
19. [Exercises](#19-exercises)
20. [Projects](#20-projects)
21. [Quick Reference Cheat Sheet](#21-cheat-sheet)

---

<a id="1-foundations"></a>
## 1. Foundations: Processes, Threads & Concurrency

### 1.1 Key Definitions

| Term | Definition |
|------|-----------|
| **Process** | An independent program in execution with its own memory space |
| **Thread** | The smallest unit of execution within a process; shares the process's memory |
| **Concurrency** | Managing multiple tasks that *can* overlap in time (may run on one core) |
| **Parallelism** | Actually executing multiple tasks *simultaneously* (requires multiple cores) |
| **Multithreading** | A technique where a single process spawns multiple threads |

### 1.2 Concurrency vs Parallelism

```
Concurrency (single core):        Parallelism (multi-core):

Thread A: ██░░██░░██              Thread A: ██████████
Thread B: ░░██░░██░░              Thread B: ██████████
                                            ↑ truly simultaneous
          ↑ interleaved (time-slicing)
```

**Concurrency** is about *dealing with* lots of things at once.
**Parallelism** is about *doing* lots of things at once.

### 1.3 Why Concurrency Matters

- **Responsiveness**: UI threads remain responsive while background work runs
- **Throughput**: Web servers handle thousands of requests concurrently
- **Resource Utilization**: Keep CPUs busy instead of waiting for I/O
- **Scalability**: Serve more users without proportionally more hardware

### 1.4 Java's Memory Model (JMM) — Essentials

The JMM defines how threads interact through memory:

- **Visibility**: Changes made by one thread may not be immediately visible to another
- **Ordering**: Instructions may be reordered by the compiler/CPU for optimization
- **Happens-Before**: A formal relationship guaranteeing visibility between actions

Key happens-before rules:
1. Program order within a thread
2. Monitor lock/unlock (synchronized)
3. `volatile` write → subsequent `volatile` read
4. `Thread.start()` → any action in the started thread
5. Any action in a thread → `Thread.join()` return

---

<a id="2-creating-threads"></a>
## 2. Creating Threads in Java

### 2.1 Extending the `Thread` Class

```java
public class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Running in: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        MyThread t = new MyThread();
        t.start(); // start(), not run()!
    }
}
```

> ⚠️ **Common Mistake**: Calling `run()` directly executes in the *current* thread. Always use `start()`.

### 2.2 Implementing `Runnable`

```java
public class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Running in: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        Thread t = new Thread(new MyRunnable());
        t.start();
    }
}
```

### 2.3 Using Lambda Expressions

```java
public class LambdaThread {
    public static void main(String[] args) {
        Thread t = new Thread(() -> {
            System.out.println("Hello from " + Thread.currentThread().getName());
        });
        t.start();
    }
}
```

### 2.4 Comparison Table

| Approach | Pros | Cons |
|----------|------|------|
| Extend `Thread` | Simple, direct | Can't extend another class |
| Implement `Runnable` | Flexible, separates task from thread | Slightly more verbose |
| Lambda | Concise, modern | Only for simple tasks |

### Tutorial 2A: Thread Basics

```java
/**
 * Create 5 threads that each print numbers 1-10 with their thread name.
 * Observe how output is interleaved.
 */
public class Tutorial2A {
    public static void main(String[] args) {
        for (int i = 1; i <= 5; i++) {
            final int threadNum = i;
            new Thread(() -> {
                for (int j = 1; j <= 10; j++) {
                    System.out.printf("Thread-%d: %d%n", threadNum, j);
                }
            }, "Worker-" + i).start();
        }
    }
}
```

**Run this multiple times** — notice the output order changes each time. This is *non-determinism* in action.

---

<a id="3-thread-lifecycle"></a>
## 3. Thread Lifecycle & Control

### 3.1 Thread States

```
         start()
NEW ──────────► RUNNABLE ◄────────────────┐
                  │                         │
                  │ (scheduler)             │
                  ▼                         │
               RUNNING                      │
                  │                         │
        ┌─────┬──┴──┬─────────┐            │
        │     │     │         │            │
        ▼     ▼     ▼         ▼            │
    BLOCKED WAITING TIMED   TERMINATED     │
        │     │   WAITING     (dead)       │
        │     │     │                      │
        └─────┴─────┴─────────────────────┘
             (condition met)
```

| State | Trigger |
|-------|---------|
| `NEW` | Thread created, not yet started |
| `RUNNABLE` | `start()` called, eligible to run |
| `BLOCKED` | Waiting to acquire a monitor lock |
| `WAITING` | `wait()`, `join()`, `LockSupport.park()` |
| `TIMED_WAITING` | `sleep()`, `wait(timeout)`, `join(timeout)` |
| `TERMINATED` | `run()` completed or exception thrown |

### 3.2 Thread Control Methods

```java
public class ThreadControlDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                System.out.println("Worker started");
                Thread.sleep(2000); // TIMED_WAITING
                System.out.println("Worker finished");
            } catch (InterruptedException e) {
                System.out.println("Worker was interrupted!");
                Thread.currentThread().interrupt(); // preserve interrupt status
            }
        });

        worker.start();
        System.out.println("State after start: " + worker.getState()); // RUNNABLE

        Thread.sleep(500);
        System.out.println("State during sleep: " + worker.getState()); // TIMED_WAITING

        worker.join(); // Main thread waits for worker to finish
        System.out.println("State after join: " + worker.getState()); // TERMINATED
    }
}
```

### 3.3 Daemon Threads

```java
Thread daemon = new Thread(() -> {
    while (true) {
        System.out.println("Background task...");
        try { Thread.sleep(500); } catch (InterruptedException e) { break; }
    }
});
daemon.setDaemon(true); // Must set BEFORE start()
daemon.start();
// JVM exits when only daemon threads remain
```

### 3.4 Thread Interruption — The Cooperative Model

```java
public class InterruptDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                // Do work...
                System.out.println("Working...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted during sleep");
                    Thread.currentThread().interrupt(); // re-set the flag
                    break;
                }
            }
            System.out.println("Worker shutting down gracefully");
        });

        worker.start();
        Thread.sleep(3000);
        worker.interrupt(); // Request interruption
        worker.join();
    }
}
```

> 💡 **Rule**: Never use `Thread.stop()` (deprecated). Always use the interruption mechanism.

---

<a id="4-synchronization"></a>
## 4. Synchronization & Thread Safety

### 4.1 The Problem: Race Conditions

```java
public class RaceConditionDemo {
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) {
                counter++; // NOT atomic: read → increment → write
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        t1.join(); t2.join();

        // Expected: 200000, Actual: varies (e.g., 156823)
        System.out.println("Counter: " + counter);
    }
}
```

### 4.2 The `synchronized` Keyword

#### Synchronized Method

```java
public class SafeCounter {
    private int count = 0;

    public synchronized void increment() {
        count++; // Only one thread can execute this at a time
    }

    public synchronized int getCount() {
        return count;
    }
}
```

#### Synchronized Block (finer granularity)

```java
public class FineGrainedSync {
    private final Object lockA = new Object();
    private final Object lockB = new Object();
    private int resourceA = 0;
    private int resourceB = 0;

    public void updateA() {
        synchronized (lockA) {
            resourceA++;
        }
    }

    public void updateB() {
        synchronized (lockB) {
            resourceB++;
        }
    }
}
```

### 4.3 The `volatile` Keyword

```java
public class VolatileDemo {
    private volatile boolean running = true; // Guarantees visibility across threads

    public void stop() {
        running = false;
    }

    public void run() {
        while (running) {
            // Without volatile, the thread might cache 'running' and loop forever
        }
        System.out.println("Stopped");
    }
}
```

**`volatile` guarantees:**
- ✅ Visibility (writes are immediately visible to other threads)
- ✅ Ordering (prevents reordering around volatile access)
- ❌ Atomicity (compound operations like `count++` are NOT atomic)

### 4.4 Deadlocks

```java
public class DeadlockDemo {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("T1: Holding Lock A");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (LOCK_B) { // Waits for Lock B held by T2
                    System.out.println("T1: Holding Lock A & B");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("T2: Holding Lock B");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (LOCK_A) { // Waits for Lock A held by T1
                    System.out.println("T2: Holding Lock A & B");
                }
            }
        });

        t1.start(); t2.start();
        // 💀 DEADLOCK — both threads wait forever
    }
}
```

**Deadlock Prevention Strategies:**
1. **Lock ordering**: Always acquire locks in the same order
2. **Lock timeout**: Use `tryLock()` with timeout (see §10)
3. **Avoid nested locks**: Minimize lock scope
4. **Use higher-level abstractions**: `java.util.concurrent`

### Tutorial 4A: Build a Thread-Safe Bank Account

```java
public class BankAccount {
    private double balance;
    private final String accountId;

    public BankAccount(String accountId, double initialBalance) {
        this.accountId = accountId;
        this.balance = initialBalance;
    }

    public synchronized void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        balance += amount;
        System.out.printf("[%s] Deposited %.2f, Balance: %.2f%n",
                Thread.currentThread().getName(), amount, balance);
    }

    public synchronized void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (amount > balance) {
            System.out.printf("[%s] Insufficient funds for %.2f, Balance: %.2f%n",
                    Thread.currentThread().getName(), amount, balance);
            return;
        }
        balance -= amount;
        System.out.printf("[%s] Withdrew %.2f, Balance: %.2f%n",
                Thread.currentThread().getName(), amount, balance);
    }

    public synchronized double getBalance() {
        return balance;
    }

    // Transfer between accounts — careful with lock ordering to avoid deadlocks!
    public static void transfer(BankAccount from, BankAccount to, double amount) {
        // Always lock in a consistent order based on accountId
        BankAccount first = from.accountId.compareTo(to.accountId) < 0 ? from : to;
        BankAccount second = first == from ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
    }
}
```

---

<a id="5-inter-thread-communication"></a>
## 5. Inter-Thread Communication

### 5.1 wait(), notify(), notifyAll()

```java
public class ProducerConsumer {
    private final java.util.LinkedList<Integer> buffer = new java.util.LinkedList<>();
    private static final int CAPACITY = 5;

    public synchronized void produce(int value) throws InterruptedException {
        while (buffer.size() == CAPACITY) {
            System.out.println("Buffer full, producer waiting...");
            wait(); // Release lock and wait
        }
        buffer.add(value);
        System.out.println("Produced: " + value + " | Buffer size: " + buffer.size());
        notifyAll(); // Wake up waiting consumers
    }

    public synchronized int consume() throws InterruptedException {
        while (buffer.isEmpty()) {
            System.out.println("Buffer empty, consumer waiting...");
            wait();
        }
        int value = buffer.removeFirst();
        System.out.println("Consumed: " + value + " | Buffer size: " + buffer.size());
        notifyAll(); // Wake up waiting producers
        return value;
    }

    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer();

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    pc.produce(i);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    pc.consume();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        producer.start();
        consumer.start();
    }
}
```

> 💡 **Always use `while` (not `if`)** when checking conditions with `wait()` — spurious wakeups can occur.

### 5.2 Modern Alternative: `BlockingQueue`

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ModernProducerConsumer {
    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

        // Producer
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    queue.put(i); // Blocks if full
                    System.out.println("Produced: " + i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Consumer
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    int val = queue.take(); // Blocks if empty
                    System.out.println("Consumed: " + val);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        producer.start();
        consumer.start();
    }
}
```

---

<a id="6-java-util-concurrent"></a>
## 6. The `java.util.concurrent` Package

### Overview of Key Components

```
java.util.concurrent
├── Executors & Thread Pools
│   ├── ExecutorService
│   ├── ScheduledExecutorService
│   └── Executors (factory)
├── Synchronizers
│   ├── CountDownLatch
│   ├── CyclicBarrier
│   ├── Semaphore
│   └── Phaser
├── Concurrent Collections
│   ├── ConcurrentHashMap
│   ├── CopyOnWriteArrayList
│   ├── BlockingQueue variants
│   └── ConcurrentLinkedQueue
├── Locks
│   ├── ReentrantLock
│   ├── ReadWriteLock
│   └── StampedLock
├── Atomic Variables
│   ├── AtomicInteger, AtomicLong
│   ├── AtomicReference
│   └── LongAdder, LongAccumulator
└── Future & CompletableFuture
```

---

<a id="7-executor-framework"></a>
## 7. Executor Framework

### 7.1 Why Executors?

Creating a new thread for each task is expensive. Executors manage a pool of reusable threads.

### 7.2 Types of Thread Pools

```java
import java.util.concurrent.*;

public class ExecutorDemo {
    public static void main(String[] args) throws Exception {
        // Fixed thread pool — good for CPU-bound tasks
        ExecutorService fixedPool = Executors.newFixedThreadPool(4);

        // Cached thread pool — good for short-lived I/O tasks
        ExecutorService cachedPool = Executors.newCachedThreadPool();

        // Single thread executor — tasks execute sequentially
        ExecutorService singleThread = Executors.newSingleThreadExecutor();

        // Scheduled executor — for delayed/periodic tasks
        ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);

        // Virtual thread executor — Java 21! (see §14)
        ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor();

        // Submit tasks
        fixedPool.submit(() -> {
            System.out.println("Task in: " + Thread.currentThread().getName());
        });

        // Schedule a task with delay
        scheduled.schedule(
            () -> System.out.println("Delayed task"),
            2, TimeUnit.SECONDS
        );

        // Schedule periodic task
        scheduled.scheduleAtFixedRate(
            () -> System.out.println("Periodic: " + System.currentTimeMillis()),
            0, 1, TimeUnit.SECONDS
        );

        Thread.sleep(5000);

        // Always shut down executors!
        fixedPool.shutdown();
        cachedPool.shutdown();
        singleThread.shutdown();
        scheduled.shutdown();
        virtualPool.shutdown();
    }
}
```

### 7.3 Proper Shutdown

```java
public static void shutdownGracefully(ExecutorService executor) {
    executor.shutdown(); // No new tasks accepted
    try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow(); // Force shutdown
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate");
            }
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

### 7.4 Custom Thread Pool (Recommended for Production)

```java
ThreadPoolExecutor customPool = new ThreadPoolExecutor(
    4,                 // corePoolSize
    8,                 // maximumPoolSize
    60L,               // keepAliveTime
    TimeUnit.SECONDS,  // time unit
    new LinkedBlockingQueue<>(100),  // work queue with bounded capacity
    new ThreadFactory() {
        private int count = 0;
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "custom-worker-" + count++);
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
);
```

**Rejection Policies:**
| Policy | Behavior |
|--------|----------|
| `AbortPolicy` | Throws `RejectedExecutionException` (default) |
| `CallerRunsPolicy` | Caller's thread runs the task |
| `DiscardPolicy` | Silently discards the task |
| `DiscardOldestPolicy` | Discards oldest queued task |

---

<a id="8-callable-future-completablefuture"></a>
## 8. Callable, Future & CompletableFuture

### 8.1 Callable & Future

```java
import java.util.concurrent.*;

public class CallableDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Callable returns a value (unlike Runnable)
        Callable<String> task = () -> {
            Thread.sleep(1000);
            return "Result from " + Thread.currentThread().getName();
        };

        Future<String> future = executor.submit(task);

        // Do other work while task executes...
        System.out.println("Doing other work...");

        // Block until result is ready
        String result = future.get(); // Blocks!
        System.out.println(result);

        // With timeout
        Future<String> future2 = executor.submit(task);
        try {
            String result2 = future2.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("Task timed out!");
            future2.cancel(true);
        }

        executor.shutdown();
    }
}
```

### 8.2 Invoking Multiple Tasks

```java
public class MultiTaskDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<String>> tasks = List.of(
            () -> { Thread.sleep(1000); return "Task 1 done"; },
            () -> { Thread.sleep(2000); return "Task 2 done"; },
            () -> { Thread.sleep(1500); return "Task 3 done"; }
        );

        // invokeAll — waits for all tasks to complete
        List<Future<String>> futures = executor.invokeAll(tasks);
        for (Future<String> f : futures) {
            System.out.println(f.get());
        }

        // invokeAny — returns result of first completed task
        String first = executor.invokeAny(tasks);
        System.out.println("First to finish: " + first);

        executor.shutdown();
    }
}
```

### 8.3 CompletableFuture — Async Programming

`CompletableFuture` is the powerhouse for composable asynchronous programming.

#### Basic Usage

```java
import java.util.concurrent.CompletableFuture;

public class CompletableFutureBasics {
    public static void main(String[] args) {
        // Run async (no return value)
        CompletableFuture<Void> cf1 = CompletableFuture.runAsync(() -> {
            System.out.println("Running in: " + Thread.currentThread().getName());
        });

        // Supply async (with return value)
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
            return "Hello from " + Thread.currentThread().getName();
        });

        cf2.thenAccept(System.out::println).join();
    }
}
```

#### Chaining & Composition

```java
public class CompletableFutureChaining {
    public static void main(String[] args) {
        CompletableFuture.supplyAsync(() -> "Hello")
            // Transform the result
            .thenApply(s -> s + " World")
            // Transform again (async)
            .thenApplyAsync(s -> s.toUpperCase())
            // Consume the result
            .thenAccept(System.out::println)
            // Handle exceptions
            .exceptionally(ex -> {
                System.err.println("Error: " + ex.getMessage());
                return null;
            })
            .join(); // Wait for completion
    }
}
```

#### Combining Multiple Futures

```java
public class CombiningFutures {
    public static void main(String[] args) {
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Goutham";
        });

        CompletableFuture<String> emailFuture = CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return "goutham@example.com";
        });

        // Combine two futures
        CompletableFuture<String> combined = userFuture.thenCombine(emailFuture,
            (name, email) -> name + " <" + email + ">");
        System.out.println(combined.join()); // Goutham <goutham@example.com>

        // Wait for all futures
        CompletableFuture<Void> all = CompletableFuture.allOf(userFuture, emailFuture);
        all.join();

        // Race — first to complete wins
        CompletableFuture<Object> any = CompletableFuture.anyOf(userFuture, emailFuture);
        System.out.println("First: " + any.join());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### Complete CompletableFuture API Map

```
supplyAsync / runAsync
       │
       ├── thenApply(fn)        → transform result
       ├── thenApplyAsync(fn)   → transform on different thread
       ├── thenAccept(consumer) → consume result
       ├── thenRun(runnable)    → run after completion
       ├── thenCompose(fn)      → flatMap (chain dependent futures)
       ├── thenCombine(other, fn) → combine two futures
       ├── handle(fn)           → handle result OR exception
       ├── exceptionally(fn)    → handle exception only
       ├── whenComplete(action) → callback on completion
       └── completeOnTimeout(val, timeout, unit)
```

---

<a id="9-concurrent-collections"></a>
## 9. Concurrent Collections

### 9.1 ConcurrentHashMap

```java
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentMapDemo {
    public static void main(String[] args) throws InterruptedException {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // Atomic operations
        map.put("counter", 0);
        map.compute("counter", (key, val) -> val + 1);      // atomic compute
        map.merge("counter", 1, Integer::sum);                // atomic merge
        map.putIfAbsent("newKey", 42);                        // atomic put-if-absent
        map.computeIfAbsent("factorial", k -> factorial(10)); // lazy compute

        // Parallel bulk operations (threshold parameter controls parallelism)
        map.put("a", 1); map.put("b", 2); map.put("c", 3);

        // forEach in parallel
        map.forEach(1, (key, value) ->
            System.out.println(key + "=" + value + " [" + Thread.currentThread().getName() + "]")
        );

        // search in parallel (returns first non-null result)
        String found = map.search(1, (key, value) -> value > 2 ? key : null);
        System.out.println("Found: " + found);

        // reduce in parallel
        int sum = map.reduce(1, (key, value) -> value, Integer::sum);
        System.out.println("Sum: " + sum);
    }

    static int factorial(int n) { return n <= 1 ? 1 : n * factorial(n - 1); }
}
```

### 9.2 CopyOnWriteArrayList

```java
import java.util.concurrent.CopyOnWriteArrayList;

// Great for read-heavy, write-rare scenarios (e.g., listener lists)
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("A");
list.add("B");

// Safe to iterate while modifying — iterator sees snapshot
for (String s : list) {
    list.add("C"); // Does NOT cause ConcurrentModificationException
}
```

### 9.3 BlockingQueue Variants

| Implementation | Bounded | Ordering | Special Feature |
|---------------|---------|----------|----------------|
| `ArrayBlockingQueue` | Yes | FIFO | Fixed-size array |
| `LinkedBlockingQueue` | Optional | FIFO | Linked nodes |
| `PriorityBlockingQueue` | No | Priority | Natural/custom ordering |
| `SynchronousQueue` | N/A | N/A | No storage; direct handoff |
| `DelayQueue` | No | Delay | Elements available after delay |
| `LinkedTransferQueue` | No | FIFO | Transfer semantics |

---

<a id="10-locks-semaphores-barriers"></a>
## 10. Locks, Semaphores & Barriers

### 10.1 ReentrantLock

```java
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
    private final ReentrantLock lock = new ReentrantLock();
    private int balance = 1000;

    public void withdraw(int amount) {
        lock.lock();
        try {
            if (balance >= amount) {
                balance -= amount;
                System.out.println("Withdrew: " + amount + ", Balance: " + balance);
            }
        } finally {
            lock.unlock(); // ALWAYS unlock in finally!
        }
    }

    // tryLock — non-blocking attempt
    public boolean tryWithdraw(int amount) {
        if (lock.tryLock()) {
            try {
                if (balance >= amount) {
                    balance -= amount;
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    // tryLock with timeout — prevents deadlocks
    public boolean tryWithdrawTimeout(int amount) throws InterruptedException {
        if (lock.tryLock(1, java.util.concurrent.TimeUnit.SECONDS)) {
            try {
                if (balance >= amount) {
                    balance -= amount;
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
```

### 10.2 ReadWriteLock

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CacheWithReadWriteLock<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public V get(K key) {
        rwLock.readLock().lock(); // Multiple readers allowed
        try {
            return cache.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        rwLock.writeLock().lock(); // Exclusive access
        try {
            cache.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

### 10.3 StampedLock (Optimistic Reads)

```java
import java.util.concurrent.locks.StampedLock;

public class Point {
    private double x, y;
    private final StampedLock lock = new StampedLock();

    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // Optimistic read — no locking overhead if no concurrent write
    public double distanceFromOrigin() {
        long stamp = lock.tryOptimisticRead(); // Non-blocking!
        double currentX = x, currentY = y;
        if (!lock.validate(stamp)) {
            // A write occurred — fall back to read lock
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }
}
```

### 10.4 Semaphore

```java
import java.util.concurrent.Semaphore;

public class ConnectionPool {
    private final Semaphore semaphore;

    public ConnectionPool(int maxConnections) {
        this.semaphore = new Semaphore(maxConnections); // permits = max connections
    }

    public void useConnection() throws InterruptedException {
        semaphore.acquire(); // Blocks if no permits available
        try {
            System.out.println(Thread.currentThread().getName() + " using connection");
            Thread.sleep(2000); // Simulate work
        } finally {
            semaphore.release(); // Return permit
        }
    }

    public static void main(String[] args) {
        ConnectionPool pool = new ConnectionPool(3); // Only 3 concurrent users

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try { pool.useConnection(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "Thread-" + i).start();
        }
    }
}
```

### 10.5 CountDownLatch

```java
import java.util.concurrent.CountDownLatch;

public class StartupManager {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3); // 3 services must start

        Runnable service = (String name) -> () -> {
            System.out.println(name + " starting...");
            try { Thread.sleep((long)(Math.random() * 3000)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println(name + " ready!");
            latch.countDown();
        };

        // Simulating with inline lambdas for correctness:
        for (String name : List.of("Database", "Cache", "MessageQueue")) {
            String svc = name;
            new Thread(() -> {
                System.out.println(svc + " starting...");
                try { Thread.sleep((long)(Math.random() * 3000)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println(svc + " ready!");
                latch.countDown();
            }).start();
        }

        System.out.println("Waiting for all services...");
        latch.await(); // Blocks until count reaches 0
        System.out.println("All services started! Application ready.");
    }
}
```

### 10.6 CyclicBarrier

```java
import java.util.concurrent.CyclicBarrier;

public class ParallelMatrixComputation {
    public static void main(String[] args) {
        int numWorkers = 4;
        CyclicBarrier barrier = new CyclicBarrier(numWorkers, () -> {
            System.out.println("--- All workers reached barrier, merging results ---");
        });

        for (int i = 0; i < numWorkers; i++) {
            final int workerId = i;
            new Thread(() -> {
                for (int phase = 1; phase <= 3; phase++) {
                    System.out.println("Worker " + workerId + " phase " + phase);
                    try {
                        Thread.sleep((long)(Math.random() * 1000));
                        barrier.await(); // Wait for all workers — REUSABLE
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }
}
```

**Comparison:**
| Feature | `CountDownLatch` | `CyclicBarrier` |
|---------|-----------------|-----------------|
| Reusable | No (one-shot) | Yes (cyclic) |
| Threads wait on | count → 0 | all parties arriving |
| Barrier action | None | Optional Runnable |
| Use case | Wait for N events | Synchronize N threads per phase |

---

<a id="11-atomic-variables"></a>
## 11. Atomic Variables & CAS

### 11.1 What is CAS (Compare-And-Swap)?

CAS is a hardware-level atomic operation:
```
if (currentValue == expectedValue) {
    currentValue = newValue;
    return true;
}
return false;
```
It's lock-free — no thread blocking, just retrying.

### 11.2 AtomicInteger, AtomicLong, AtomicBoolean

```java
import java.util.concurrent.atomic.*;

public class AtomicDemo {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final AtomicLong longCounter = new AtomicLong(0);
    private static final AtomicBoolean flag = new AtomicBoolean(false);

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) {
                counter.incrementAndGet();  // Atomic increment
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        t1.join(); t2.join();

        System.out.println("Counter: " + counter.get()); // Always 200000

        // Other atomic operations
        counter.compareAndSet(200000, 0);      // CAS
        counter.getAndAdd(10);                  // Returns old value, adds 10
        counter.updateAndGet(x -> x * 2);      // Atomic update with function
        counter.accumulateAndGet(5, Integer::sum); // Atomic accumulate
    }
}
```

### 11.3 AtomicReference

```java
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceDemo {
    record User(String name, int age) {}

    private static final AtomicReference<User> currentUser =
        new AtomicReference<>(new User("Guest", 0));

    public static void main(String[] args) {
        // Atomic compare-and-set on reference
        User guest = currentUser.get();
        boolean updated = currentUser.compareAndSet(guest, new User("Goutham", 25));
        System.out.println("Updated: " + updated + ", User: " + currentUser.get());
    }
}
```

### 11.4 LongAdder & LongAccumulator (High Contention)

```java
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.LongAccumulator;

// LongAdder — faster than AtomicLong under heavy contention
LongAdder adder = new LongAdder();
adder.increment();
adder.add(10);
long sum = adder.sum(); // Get current sum

// LongAccumulator — generalized version
LongAccumulator max = new LongAccumulator(Long::max, Long.MIN_VALUE);
max.accumulate(42);
max.accumulate(99);
System.out.println(max.get()); // 99
```

> 💡 Use `LongAdder` instead of `AtomicLong` when you have many threads doing frequent updates and reads are infrequent (e.g., counters, metrics).

---

<a id="12-forkjoin-framework"></a>
## 12. Fork/Join Framework

### 12.1 Concept: Divide & Conquer

```
         [Large Task]
          /        \
    [Sub-task A]  [Sub-task B]     ← FORK
       /    \        /    \
     [A1]  [A2]   [B1]   [B2]    ← FORK
      |      |      |      |
     [r1]  [r2]   [r3]   [r4]    ← COMPUTE
      \     /       \     /
     [Result A]   [Result B]      ← JOIN
          \        /
       [Final Result]              ← JOIN
```

### 12.2 RecursiveTask (returns a value)

```java
import java.util.concurrent.*;

public class ParallelSum extends RecursiveTask<Long> {
    private static final int THRESHOLD = 10_000;
    private final long[] array;
    private final int start, end;

    public ParallelSum(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        int length = end - start;
        if (length <= THRESHOLD) {
            // Base case: compute directly
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        }

        // Recursive case: fork & join
        int mid = start + length / 2;
        ParallelSum left = new ParallelSum(array, start, mid);
        ParallelSum right = new ParallelSum(array, mid, end);

        left.fork();  // Execute left in another thread
        long rightResult = right.compute(); // Compute right in current thread
        long leftResult = left.join();       // Wait for left

        return leftResult + rightResult;
    }

    public static void main(String[] args) {
        long[] array = new long[10_000_000];
        for (int i = 0; i < array.length; i++) array[i] = i + 1;

        ForkJoinPool pool = ForkJoinPool.commonPool();
        long sum = pool.invoke(new ParallelSum(array, 0, array.length));
        System.out.println("Sum: " + sum);
    }
}
```

### 12.3 RecursiveAction (no return value)

```java
import java.util.concurrent.RecursiveAction;

public class ParallelMergeSort extends RecursiveAction {
    private static final int THRESHOLD = 1024;
    private final int[] array;
    private final int start, end;

    public ParallelMergeSort(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected void compute() {
        if (end - start <= THRESHOLD) {
            java.util.Arrays.sort(array, start, end); // Sequential sort for small arrays
            return;
        }

        int mid = (start + end) / 2;
        ParallelMergeSort left = new ParallelMergeSort(array, start, mid);
        ParallelMergeSort right = new ParallelMergeSort(array, mid, end);

        invokeAll(left, right); // Fork both, join both

        merge(array, start, mid, end);
    }

    private void merge(int[] arr, int start, int mid, int end) {
        int[] temp = java.util.Arrays.copyOfRange(arr, start, mid);
        int i = 0, j = mid, k = start;
        while (i < temp.length && j < end) {
            arr[k++] = (temp[i] <= arr[j]) ? temp[i++] : arr[j++];
        }
        while (i < temp.length) arr[k++] = temp[i++];
    }
}
```

### 12.4 Work-Stealing

The ForkJoinPool uses **work-stealing**: idle threads steal tasks from busy threads' queues. This keeps all cores busy and is ideal for irregular workloads.

---

<a id="13-parallel-streams"></a>
## 13. Parallel Streams

### 13.1 Basic Usage

```java
import java.util.stream.*;
import java.util.List;

public class ParallelStreamDemo {
    public static void main(String[] args) {
        List<Integer> numbers = IntStream.rangeClosed(1, 1_000_000)
            .boxed()
            .toList();

        // Sequential
        long seqSum = numbers.stream()
            .mapToLong(Integer::longValue)
            .sum();

        // Parallel — uses ForkJoinPool.commonPool()
        long parSum = numbers.parallelStream()
            .mapToLong(Integer::longValue)
            .sum();

        // Convert sequential to parallel
        long parSum2 = numbers.stream()
            .parallel()
            .mapToLong(Integer::longValue)
            .sum();
    }
}
```

### 13.2 When to Use Parallel Streams

| Use Parallel When... | Avoid Parallel When... |
|---------------------|----------------------|
| Large data sets (>10,000 elements) | Small data sets |
| CPU-intensive per-element operations | I/O operations |
| No shared mutable state | Operations with side effects |
| Easily splittable source (arrays, ArrayList) | LinkedList, iterators |
| Stateless, associative operations | Order-dependent operations |

### 13.3 Custom ForkJoinPool for Parallel Streams

```java
// Don't pollute the common pool — use a custom one
ForkJoinPool customPool = new ForkJoinPool(8);
long result = customPool.submit(() ->
    numbers.parallelStream()
        .filter(n -> n % 2 == 0)
        .mapToLong(Integer::longValue)
        .sum()
).get();
customPool.shutdown();
```

### 13.4 Pitfalls

```java
// ❌ BAD: Shared mutable state
List<Integer> results = new ArrayList<>(); // NOT thread-safe!
numbers.parallelStream().forEach(results::add); // Race condition

// ✅ GOOD: Use collect()
List<Integer> results = numbers.parallelStream()
    .filter(n -> n > 500_000)
    .collect(Collectors.toList()); // Thread-safe collection
```

---

<a id="14-virtual-threads"></a>
## 14. Virtual Threads (Project Loom) — Java 21 ⭐

### 14.1 What Are Virtual Threads?

Virtual threads are lightweight threads managed by the JVM (not the OS). They are the **biggest concurrency feature since Java 5**.

```
Traditional (Platform) Threads:        Virtual Threads:
┌─────────────────────────┐           ┌─────────────────────────┐
│  Java Thread (1:1 OS)   │           │  Virtual Thread (M:N)   │
│  ~1MB stack each        │           │  ~few KB stack each     │
│  ~thousands max         │           │  ~millions possible     │
│  Expensive to create    │           │  Cheap to create        │
│  OS-scheduled           │           │  JVM-scheduled          │
└─────────────────────────┘           └─────────────────────────┘
```

### 14.2 Creating Virtual Threads

```java
public class VirtualThreadBasics {
    public static void main(String[] args) throws InterruptedException {
        // Method 1: Thread.startVirtualThread()
        Thread vt1 = Thread.startVirtualThread(() -> {
            System.out.println("Virtual thread: " + Thread.currentThread());
            System.out.println("Is virtual: " + Thread.currentThread().isVirtual());
        });
        vt1.join();

        // Method 2: Thread.ofVirtual()
        Thread vt2 = Thread.ofVirtual()
            .name("my-virtual-thread")
            .start(() -> {
                System.out.println("Named virtual thread: " + Thread.currentThread().getName());
            });
        vt2.join();

        // Method 3: Factory
        ThreadFactory factory = Thread.ofVirtual()
            .name("worker-", 0) // worker-0, worker-1, worker-2, ...
            .factory();

        Thread vt3 = factory.newThread(() -> System.out.println("From factory"));
        vt3.start();
        vt3.join();

        // Method 4: ExecutorService (MOST COMMON)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100_000; i++) {
                executor.submit(() -> {
                    Thread.sleep(1000); // Blocks the virtual thread, NOT the OS thread
                    return "Done";
                });
            }
        } // Auto-shutdown & awaits all tasks
    }
}
```

### 14.3 One Million Virtual Threads!

```java
import java.time.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class MillionVirtualThreads {
    public static void main(String[] args) throws Exception {
        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 1_000_000).forEach(i -> {
                executor.submit(() -> {
                    Thread.sleep(Duration.ofSeconds(1));
                    return i;
                });
            });
        }

        Duration elapsed = Duration.between(start, Instant.now());
        System.out.println("Completed 1M virtual threads in: " + elapsed);
        // Typically ~1-2 seconds! (vs. impossible with platform threads)
    }
}
```

### 14.4 Virtual Threads & I/O

Virtual threads shine for **I/O-bound** workloads. When a virtual thread blocks on I/O, the carrier (platform) thread is released to run other virtual threads.

```java
public class VirtualThreadIO {
    public static void main(String[] args) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Simulate 10,000 concurrent HTTP calls
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 10_000; i++) {
                futures.add(executor.submit(() -> {
                    // Each virtual thread performs blocking I/O
                    // The carrier thread handles thousands of these efficiently
                    return fetchUrl("https://api.example.com/data");
                }));
            }

            for (Future<String> f : futures) {
                f.get(); // Collect results
            }
        }
    }

    static String fetchUrl(String url) throws Exception {
        Thread.sleep(100); // Simulating network I/O
        return "Response from " + url;
    }
}
```

### 14.5 When to Use Virtual Threads

| ✅ Use Virtual Threads | ❌ Don't Use Virtual Threads |
|------------------------|------------------------------|
| I/O-bound tasks (HTTP, DB, file I/O) | CPU-bound computation |
| High-concurrency servers | Tasks requiring OS thread features |
| Tasks that mostly wait | Code using `synchronized` heavily with long-held locks* |
| Replacing platform thread pools for I/O | Real-time systems needing consistent timing |

> \* `synchronized` blocks can "pin" virtual threads to carrier threads. Prefer `ReentrantLock` with virtual threads.

### 14.6 Pinning — The Gotcha

```java
// ❌ AVOID: synchronized blocks pin virtual threads to carrier threads
synchronized (lock) {
    Thread.sleep(1000); // Carrier thread is pinned and can't be reused!
}

// ✅ PREFER: ReentrantLock doesn't pin
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    Thread.sleep(1000); // Carrier thread is released during sleep
} finally {
    lock.unlock();
}
```

Detect pinning with: `java -Djdk.tracePinnedThreads=short`

### 14.7 Migrating from Thread Pools to Virtual Threads

```java
// BEFORE: Traditional thread pool
ExecutorService executor = Executors.newFixedThreadPool(200);

// AFTER: Virtual threads — one line change!
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// The rest of your code stays the same
Future<String> future = executor.submit(() -> {
    return callExternalService();
});
```

### Tutorial 14A: Virtual Thread Web Server Simulation

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualThreadServer {
    private static final AtomicInteger requestCount = new AtomicInteger();
    private static final AtomicInteger activeCount = new AtomicInteger();
    private static volatile int peakActive = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Simulate 50,000 concurrent requests
            for (int i = 0; i < 50_000; i++) {
                executor.submit(() -> handleRequest());
            }
        }

        System.out.println("Total requests: " + requestCount.get());
        System.out.println("Peak concurrency: " + peakActive);
    }

    static String handleRequest() {
        int active = activeCount.incrementAndGet();
        peakActive = Math.max(peakActive, active);

        try {
            // Simulate I/O (DB query, external API call)
            Thread.sleep(100);
            int count = requestCount.incrementAndGet();
            if (count % 10_000 == 0) {
                System.out.printf("Processed %d requests, active: %d%n", count, active);
            }
            return "OK";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR";
        } finally {
            activeCount.decrementAndGet();
        }
    }
}
```

---

<a id="15-structured-concurrency"></a>
## 15. Structured Concurrency — Java 21 (Preview)

> ⚠️ Preview feature in Java 21. Enable with `--enable-preview`.

### 15.1 The Problem with Unstructured Concurrency

```java
// ❌ Unstructured: What if fetchUser fails but fetchOrder keeps running?
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
Future<User> userFuture = executor.submit(() -> fetchUser(userId));
Future<Order> orderFuture = executor.submit(() -> fetchOrder(orderId));
// If fetchUser throws, fetchOrder is still running wastefully
// If we forget to close executor, threads leak
```

### 15.2 StructuredTaskScope

Structured concurrency ensures that concurrent subtasks are treated as a unit:
- If any subtask fails, others are cancelled
- The parent task doesn't complete until all subtasks complete
- No thread leaks

```java
import java.util.concurrent.StructuredTaskScope;

public class StructuredConcurrencyDemo {

    record User(String name) {}
    record Order(String item) {}
    record Response(User user, Order order) {}

    // ShutdownOnFailure: Cancel all if ANY task fails
    static Response fetchUserAndOrder(String userId, String orderId) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Fork subtasks
            StructuredTaskScope.Subtask<User> userTask =
                scope.fork(() -> fetchUser(userId));
            StructuredTaskScope.Subtask<Order> orderTask =
                scope.fork(() -> fetchOrder(orderId));

            scope.join();            // Wait for all subtasks
            scope.throwIfFailed();   // Propagate any exception

            // Both succeeded — get results
            return new Response(userTask.get(), orderTask.get());
        }
    }

    // ShutdownOnSuccess: Return first successful result, cancel rest
    static String fetchFromAnyMirror(String resource) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> fetchFromMirror1(resource));
            scope.fork(() -> fetchFromMirror2(resource));
            scope.fork(() -> fetchFromMirror3(resource));

            scope.join();
            return scope.result(); // First successful result
        }
    }

    static User fetchUser(String id) throws InterruptedException {
        Thread.sleep(500);
        return new User("Goutham");
    }

    static Order fetchOrder(String id) throws InterruptedException {
        Thread.sleep(300);
        return new Order("Laptop");
    }

    static String fetchFromMirror1(String res) throws InterruptedException {
        Thread.sleep(200);
        return "Mirror 1: " + res;
    }

    static String fetchFromMirror2(String res) throws InterruptedException {
        Thread.sleep(100);
        return "Mirror 2: " + res;
    }

    static String fetchFromMirror3(String res) throws InterruptedException {
        Thread.sleep(300);
        return "Mirror 3: " + res;
    }
}
```

### 15.3 Benefits of Structured Concurrency

| Aspect | Unstructured | Structured |
|--------|-------------|-----------|
| Lifetime management | Manual | Automatic (scope-based) |
| Error propagation | Easy to miss | Automatic |
| Cancellation | Must track all tasks | Automatic on failure |
| Thread leaks | Possible | Impossible |
| Observability | Hard to trace | Clear parent-child |

---

<a id="16-scoped-values"></a>
## 16. Scoped Values — Java 21 (Preview)

> ⚠️ Preview feature in Java 21. Enable with `--enable-preview`.

### 16.1 ThreadLocal vs ScopedValue

`ThreadLocal` has problems with virtual threads (memory overhead, inheritance issues). `ScopedValue` is the modern replacement.

```java
import java.lang.ScopedValue;

public class ScopedValueDemo {
    // Declare a scoped value (immutable, bounded lifetime)
    private static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();

    public static void main(String[] args) {
        // Bind a value for a specific scope
        ScopedValue.runWhere(CURRENT_USER, "Goutham", () -> {
            System.out.println("User: " + CURRENT_USER.get()); // "Goutham"
            handleRequest();
        });

        // Outside the scope, value is not bound
        System.out.println("Bound: " + CURRENT_USER.isBound()); // false
    }

    static void handleRequest() {
        // Access the scoped value anywhere in the call chain
        String user = CURRENT_USER.get();
        System.out.println("Handling request for: " + user);
        processData();
    }

    static void processData() {
        // Still accessible — inherited through the call stack
        System.out.println("Processing data for: " + CURRENT_USER.get());
    }
}
```

### 16.2 ScopedValue with StructuredTaskScope

```java
public class ScopedValueWithStructuredConcurrency {
    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    static void handleRequest(String requestId) throws Exception {
        ScopedValue.runWhere(REQUEST_ID, requestId, () -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                // Forked tasks inherit the scoped value
                scope.fork(() -> {
                    System.out.println("[" + REQUEST_ID.get() + "] Fetching user");
                    Thread.sleep(100);
                    return "user-data";
                });
                scope.fork(() -> {
                    System.out.println("[" + REQUEST_ID.get() + "] Fetching orders");
                    Thread.sleep(150);
                    return "order-data";
                });

                scope.join();
                scope.throwIfFailed();
            }
        });
    }
}
```

### 16.3 ThreadLocal vs ScopedValue Comparison

| Feature | `ThreadLocal` | `ScopedValue` |
|---------|--------------|---------------|
| Mutability | Mutable (`set()` anytime) | Immutable per scope |
| Lifetime | Unbounded (manual cleanup) | Automatic (scope-based) |
| Inheritance | `InheritableThreadLocal` | Automatic with structured concurrency |
| Memory overhead | Per-thread map entry | Minimal |
| Virtual thread friendly | ❌ (memory overhead × millions) | ✅ Designed for virtual threads |

---

<a id="17-concurrency-patterns"></a>
## 17. Common Concurrency Patterns

### 17.1 Thread-Safe Singleton (Double-Checked Locking)

```java
public class Singleton {
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {                    // First check (no locking)
            synchronized (Singleton.class) {
                if (instance == null) {            // Second check (with locking)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// Even better — use an enum
public enum SingletonEnum {
    INSTANCE;

    public void doSomething() { }
}
```

### 17.2 Producer-Consumer with BlockingQueue

```java
public class ProducerConsumerPattern {
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);
    private static final String POISON_PILL = "STOP";

    static class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 100; i++) {
                    queue.put("Item-" + i);
                }
                queue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String item = queue.take();
                    if (POISON_PILL.equals(item)) break;
                    process(item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void process(String item) {
            System.out.println("Processing: " + item);
        }
    }
}
```

### 17.3 Async Event Bus

```java
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AsyncEventBus {
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>> handlers =
        new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        var eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (Consumer<?> handler : eventHandlers) {
                executor.submit(() -> ((Consumer<T>) handler).accept(event));
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
```

### 17.4 Rate Limiter (Token Bucket)

```java
import java.util.concurrent.Semaphore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private final Semaphore semaphore;
    private final int maxPermits;
    private final ScheduledExecutorService scheduler;

    public RateLimiter(int permitsPerSecond) {
        this.maxPermits = permitsPerSecond;
        this.semaphore = new Semaphore(permitsPerSecond);
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Refill permits every second
        scheduler.scheduleAtFixedRate(() -> {
            int permitsToRelease = maxPermits - semaphore.availablePermits();
            if (permitsToRelease > 0) {
                semaphore.release(permitsToRelease);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
```

### 17.5 Thread-Safe Cache with Expiry

```java
import java.util.concurrent.*;
import java.time.*;

public class ExpiringCache<K, V> {
    private record CacheEntry<V>(V value, Instant expiresAt) {
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final Duration ttl;

    public ExpiringCache(Duration ttl) {
        this.ttl = ttl;
        // Cleanup expired entries periodically
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().factory()
        );
        cleaner.scheduleAtFixedRate(this::evictExpired, 1, 1, TimeUnit.MINUTES);
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, Instant.now().plus(ttl)));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    public V computeIfAbsent(K key, java.util.function.Function<K, V> loader) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }
        V value = loader.apply(key);
        put(key, value);
        return value;
    }

    private void evictExpired() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
```

---

<a id="18-debugging-testing"></a>
## 18. Debugging & Testing Concurrent Code

### 18.1 Common Concurrency Bugs

| Bug | Description | Detection |
|-----|-------------|-----------|
| **Race Condition** | Outcome depends on thread scheduling | Vary thread counts, add `Thread.sleep()` |
| **Deadlock** | Threads waiting for each other's locks | Thread dump (`jstack`), visualVM |
| **Livelock** | Threads actively trying but making no progress | CPU at 100% but no work done |
| **Starvation** | Some threads never get CPU time | Fair locks, priority adjustment |
| **Memory Visibility** | Thread caches stale value | Use `volatile`, `synchronized` |

### 18.2 Thread Dump Analysis

```bash
# Get thread dump (Unix)
jstack <pid>

# Or within code
Thread.getAllStackTraces().forEach((thread, stack) -> {
    System.out.println(thread.getName() + " [" + thread.getState() + "]");
    for (StackTraceElement elem : stack) {
        System.out.println("  at " + elem);
    }
});
```

### 18.3 Testing with CountDownLatch

```java
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ThreadSafeCounterTest {
    @Test
    void testConcurrentIncrements() throws InterruptedException {
        SafeCounter counter = new SafeCounter();
        int threadCount = 100;
        int incrementsPerThread = 10_000;
        CountDownLatch startGate = new CountDownLatch(1);    // Ensures all start together
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startGate.await(); // Wait for signal
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        startGate.countDown();    // Start all threads simultaneously!
        endGate.await();          // Wait for all to finish

        assertEquals(threadCount * incrementsPerThread, counter.getCount());
    }
}
```

### 18.4 Detecting Deadlocks Programmatically

```java
import java.lang.management.*;

public class DeadlockDetector {
    public static void detectDeadlocks() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();

        if (deadlockedThreadIds != null) {
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreadIds, true, true);
            System.err.println("DEADLOCK DETECTED!");
            for (ThreadInfo info : threadInfos) {
                System.err.println(info);
            }
        }
    }
}
```

### 18.5 JVM Flags for Debugging

```bash
# Trace virtual thread pinning
java -Djdk.tracePinnedThreads=short MyApp

# Enable structured concurrency & scoped values (preview)
java --enable-preview --source 21 MyApp.java

# Useful JFR events for concurrency profiling
java -XX:StartFlightRecording=duration=30s,filename=recording.jfr MyApp
```

---

<a id="19-exercises"></a>
## 19. Exercises

### Beginner Exercises

#### Exercise 1: Thread Basics
Create a program with 3 threads that print the letters A-Z. Each thread should print a different range:
- Thread 1: A-H
- Thread 2: I-P
- Thread 3: Q-Z

Ensure each thread's output is labeled with its name.

<details>
<summary>💡 Hint</summary>
Use `Thread.ofVirtual().name("xxx").start(...)` and pass the character range as constructor/lambda parameters.
</details>

---

#### Exercise 2: Race Condition Fix
The following code has a race condition. Fix it using three different approaches:
a) `synchronized`
b) `AtomicInteger`
c) `ReentrantLock`

```java
public class BrokenCounter {
    private int count = 0;

    public void increment() { count++; }
    public int getCount() { return count; }

    public static void main(String[] args) throws InterruptedException {
        BrokenCounter counter = new BrokenCounter();
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100_000; j++) counter.increment();
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        System.out.println("Expected: 1000000, Got: " + counter.getCount());
    }
}
```

---

#### Exercise 3: Daemon Thread Logger
Create a background daemon thread that prints the timestamp every 2 seconds. The main thread should do some work (e.g., count to 10 with 1-second delays) and then exit. Observe that the daemon thread stops when main exits.

---

### Intermediate Exercises

#### Exercise 4: Dining Philosophers
Implement the classic Dining Philosophers problem with 5 philosophers and 5 forks. Ensure no deadlock occurs. Use `ReentrantLock` with `tryLock()` and timeout.

<details>
<summary>💡 Hint</summary>

```java
public class DiningPhilosophers {
    private static final int NUM = 5;
    private final ReentrantLock[] forks = new ReentrantLock[NUM];

    // Strategy: Each philosopher tries to pick up both forks with tryLock.
    // If they can't get both, they put down any acquired fork and retry.
}
```
</details>

---

#### Exercise 5: Bounded Buffer
Implement a generic bounded buffer `BoundedBuffer<T>` using `ReentrantLock` and `Condition` variables (not `BlockingQueue`). Support:
- `put(T item)` — blocks if full
- `T take()` — blocks if empty
- `int size()` — current number of items
- Test with multiple producer and consumer threads.

---

#### Exercise 6: Parallel File Word Counter
Write a program that:
1. Takes a directory path as input
2. Finds all `.txt` files recursively
3. Uses a `ForkJoinPool` to count words in all files in parallel
4. Reports: total files, total words, top 10 most common words

---

#### Exercise 7: CompletableFuture Pipeline
Build an asynchronous data processing pipeline:
1. Fetch raw data (simulated — return a list of strings)
2. Parse the data (convert strings to records)
3. Validate each record
4. Transform valid records
5. Save results

Use `CompletableFuture` chaining. Each step should run asynchronously. Handle errors at each stage.

---

### Advanced Exercises

#### Exercise 8: Thread Pool Implementation
Implement your own simple thread pool from scratch:
- Constructor takes pool size
- `submit(Runnable task)` method
- `shutdown()` and `shutdownNow()` methods
- Internal work queue using `BlockingQueue`
- Worker threads that consume from the queue

---

#### Exercise 9: Virtual Thread Performance Comparison
Write a benchmark comparing:
- Platform threads (`Executors.newFixedThreadPool(200)`)
- Virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`)

For each, submit 100,000 tasks that simulate I/O (sleep 100ms). Measure:
- Total execution time
- Memory usage (`Runtime.getRuntime().totalMemory()`)
- Peak thread count

```java
// Skeleton
public class VirtualVsPlatformBenchmark {
    static final int TASK_COUNT = 100_000;
    static final Duration IO_DURATION = Duration.ofMillis(100);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Platform Threads ===");
        benchmark(Executors.newFixedThreadPool(200));

        System.out.println("=== Virtual Threads ===");
        benchmark(Executors.newVirtualThreadPerTaskExecutor());
    }

    static void benchmark(ExecutorService executor) throws Exception {
        // TODO: Submit tasks, measure time, memory, thread count
    }
}
```

---

#### Exercise 10: Structured Concurrency — Microservice Aggregator
Build a simulated microservice aggregator using `StructuredTaskScope`:

1. Fetch user profile from "User Service" (200ms)
2. Fetch recommendations from "Recommendation Service" (300ms)
3. Fetch notifications from "Notification Service" (150ms)

Requirements:
- Use `ShutdownOnFailure` — if any service fails, cancel all
- Add a timeout of 500ms for the entire operation
- Randomly simulate failures (20% chance)
- Return an aggregated response record

---

#### Exercise 11: Lock-Free Stack
Implement a lock-free (non-blocking) stack using `AtomicReference` and CAS:

```java
public class LockFreeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>();

    private record Node<T>(T value, Node<T> next) {}

    public void push(T value) { /* CAS loop */ }
    public T pop() { /* CAS loop */ }
}
```

---

#### Exercise 12: Concurrent Web Crawler
Build a concurrent web crawler (simulated):
- Start with a seed URL
- Discover links on each page (simulated)
- Crawl up to a configurable depth
- Use virtual threads for I/O
- Use `ConcurrentHashMap` to avoid visiting the same URL twice
- Implement a `Semaphore`-based rate limiter (max 10 concurrent requests)
- Print a summary: total pages crawled, unique domains found

---

#### Exercise 13: ReadWriteLock-Based Cache with Refresh
Implement a cache that:
- Allows concurrent reads
- Exclusively locks for writes
- Automatically refreshes entries after a TTL using a scheduled virtual thread
- Uses `StampedLock` with optimistic reads for maximum read performance
- Tracks cache hit/miss statistics using `LongAdder`

---

#### Exercise 14: Phaser-Based Pipeline
Implement a multi-phase data processing pipeline using `Phaser`:
- Phase 1: Load data (5 worker threads load from different sources)
- Phase 2: Transform data (workers transform their loaded data)
- Phase 3: Aggregate results (workers contribute to a shared result)
- Each phase must wait for all workers before proceeding to the next

---

#### Exercise 15: ScopedValue Request Context
Build a simulated HTTP server that:
- Creates a virtual thread per request
- Uses `ScopedValue` to propagate:
  - Request ID
  - Authenticated user info
  - Trace/correlation ID
- Each layer of the call stack accesses context via ScopedValues
- Forked tasks (via `StructuredTaskScope`) automatically inherit the context

---

<a id="20-projects"></a>
## 20. Mini Projects

### Project 1: Concurrent Download Manager
Build a file download manager that:
- Accepts multiple download URLs
- Downloads them in parallel using virtual threads
- Shows progress for each download
- Supports pause/resume (using thread interruption)
- Limits concurrent downloads (using `Semaphore`)
- Retries failed downloads (max 3 times with exponential backoff)

### Project 2: Real-Time Chat Server
Build a multi-user chat server:
- Server accepts connections using virtual threads
- Each client gets its own virtual thread
- Broadcast messages to all connected clients
- Use `ConcurrentHashMap` for client management
- Use `ScopedValue` for user session context
- Implement rooms with independent concurrent access

### Project 3: Parallel MapReduce Engine
Build a mini MapReduce framework:
- `map()` phase: process input splits in parallel using Fork/Join
- `shuffle()` phase: group by key using `ConcurrentHashMap`
- `reduce()` phase: reduce each group in parallel
- Test with: word count, average calculation, inverted index

### Project 4: Stock Trading Simulation
Build a stock exchange simulation:
- `OrderBook` per stock with concurrent buy/sell operations
- `StampedLock` for price reads (optimistic) and order matching (write)
- Virtual threads for order processing
- `LongAdder` for volume tracking
- Real-time price updates using a publish-subscribe pattern
- Detect and prevent deadlocks in cross-stock transactions

---

<a id="21-cheat-sheet"></a>
## 21. Quick Reference Cheat Sheet

### Thread Creation

```java
// Platform thread
Thread.ofPlatform().name("worker").start(() -> { ... });

// Virtual thread (Java 21)
Thread.ofVirtual().name("vt").start(() -> { ... });
Thread.startVirtualThread(() -> { ... });

// Executors
Executors.newFixedThreadPool(n);
Executors.newCachedThreadPool();
Executors.newVirtualThreadPerTaskExecutor();  // Java 21
```

### Synchronization

```java
// Intrinsic lock
synchronized (object) { ... }

// ReentrantLock
var lock = new ReentrantLock();
lock.lock();
try { ... } finally { lock.unlock(); }

// ReadWriteLock
var rwLock = new ReentrantReadWriteLock();
rwLock.readLock().lock();   // Multiple concurrent readers
rwLock.writeLock().lock();  // Exclusive writer

// StampedLock (optimistic)
var sl = new StampedLock();
long stamp = sl.tryOptimisticRead();
// ... read ...
if (!sl.validate(stamp)) { stamp = sl.readLock(); ... }
```

### Atomic Operations

```java
AtomicInteger ai = new AtomicInteger(0);
ai.incrementAndGet();         // ++i
ai.getAndIncrement();         // i++
ai.compareAndSet(old, new);   // CAS
ai.updateAndGet(x -> x * 2); // Atomic function

LongAdder adder = new LongAdder(); // High-contention counter
adder.increment();
adder.sum();
```

### CompletableFuture

```java
CompletableFuture.supplyAsync(() -> value)
    .thenApply(v -> transform(v))
    .thenCompose(v -> asyncOp(v))     // flatMap
    .thenCombine(other, (a,b) -> merge)
    .exceptionally(ex -> fallback)
    .thenAccept(v -> use(v))
    .join();
```

### Virtual Threads (Java 21)

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> { ... });
}
```

### Structured Concurrency (Java 21 Preview)

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var task1 = scope.fork(() -> fetchA());
    var task2 = scope.fork(() -> fetchB());
    scope.join();
    scope.throwIfFailed();
    return combine(task1.get(), task2.get());
}
```

### Scoped Values (Java 21 Preview)

```java
static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
ScopedValue.runWhere(CURRENT_USER, user, () -> { ... });
CURRENT_USER.get(); // anywhere in the call chain
```

### Key Rules

1. **Never call `Thread.stop()`** — use interruption
2. **Always unlock in `finally`** blocks
3. **Use `while` with `wait()`** — never `if`
4. **Prefer `ReentrantLock` over `synchronized`** with virtual threads
5. **Close `ExecutorService`** — use try-with-resources (Java 21)
6. **Don't share mutable state** — or protect it properly
7. **Use `volatile` for flags** — not for compound operations
8. **Virtual threads for I/O** — platform threads for CPU-bound work
9. **Avoid `ThreadLocal` with virtual threads** — use `ScopedValue`
10. **Test concurrency** — run many threads, use latches for timing

---

## Recommended Learning Path

```
Week 1: Sections 1-4   (Foundations, Threads, Synchronization)
Week 2: Sections 5-7   (Communication, j.u.c., Executors)
Week 3: Sections 8-9   (CompletableFuture, Concurrent Collections)
Week 4: Sections 10-11 (Locks, Atomic Variables)
Week 5: Sections 12-13 (Fork/Join, Parallel Streams)
Week 6: Sections 14-16 (Virtual Threads, Structured Concurrency, Scoped Values)
Week 7: Section 17      (Patterns — implement each one)
Week 8: Exercises 1-7   (Beginner + Intermediate)
Week 9: Exercises 8-15  (Advanced)
Week 10: Projects       (Pick 1-2 and build them)
```

---

## Further Resources

- **Java 21 JEP 444**: Virtual Threads — https://openjdk.org/jeps/444
- **Java 21 JEP 453**: Structured Concurrency (Preview) — https://openjdk.org/jeps/453
- **Java 21 JEP 446**: Scoped Values (Preview) — https://openjdk.org/jeps/446
- **Book**: *Java Concurrency in Practice* by Brian Goetz
- **Book**: *Modern Java in Action* by Urma, Fusco, Mycroft
- **Java Language Specification**: Chapter 17 (Memory Model)

---

*Happy concurrent coding! 🧵*
