# Concurrency And Multi-Threading in Java

#### ArrayBlockingQueue: 
- A thread-safe queue that blocks when trying to add an element to a full queue or retrieve an element from an empty queue.
- It uses reentrant locks and condition variables to manage concurrent access. 
- It is a bounded queue, meaning it has a fixed capacity.

#### Executor Framework:
- A framework that provides a high-level API for managing and executing threads.
- It includes classes like `Executor`, `ExecutorService`, and `ScheduledExecutorService` for managing thread pools and scheduling tasks.
- We submit a runnable or callable task to an executor, and it handles the execution of the task in a separate thread.
- There are different types of thread pools available, such as `FixedThreadPool`, `CachedThreadPool`, and `SingleThreadExecutor`, each with its own characteristics and use cases.
- Always shutdown the executor service after use to free up resources.
- FixedThreadPool uses a unbounded LinkedBlockingQueue, which can lead to resource exhaustion if too many tasks are submitted.
- For production code consider building a custom thread pool with specific configurations to better manage resources and performance.
  - This will have core and maximum pool sizes, keep-alive times, and a work queue to manage tasks.
  - Also includes rejection policies to handle tasks that cannot be executed when the pool is saturated.

#### Callable, Future & CompletableFuture
- Issue with runnable is that it cannot return a result or throw a checked exception.
- Callable is a functional interface that can return a result and throw checked exceptions.
- Future represents the result of an asynchronous computation. It provides methods to check if the computation is complete, to wait for its completion, and to retrieve the result of the computation.
- Future.get() is a blocking call that waits for the computation to complete and returns the result. If the computation is not complete, it will block until it is.
- CompletableFuture is an extension of Future that provides a more powerful and flexible API for handling asynchronous computations. It allows you to chain multiple asynchronous operations together and handle their results in a more convenient way.
- CompletableFuture can be used to create complex asynchronous workflows, handle exceptions, and combine multiple asynchronous operations in a more readable and maintainable way. It also provides methods for handling timeouts and cancellations, making it a powerful tool for managing asynchronous tasks in Java
- Complete CompletableFuture API Map
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
- You can do CompletableFuture.allOf() to wait for multiple futures to complete and CompletableFuture.anyOf() to wait for any one of them to complete.


#### Locks,Semaphores & Barriers:
- synchronized blocks have some problems:
  - They can't interrupt a thread waiting to acquire a lock, which can lead to deadlocks.
  - They don't support fairness, meaning that threads may acquire locks in an unpredictable order.
  - Can't try to acquire a lock without blocking, which can lead to performance issues.
##### ReentrantLock:
 ```java
    private final ReentrantLock lock = new ReentrantLock();
    public void someMethod() {
        lock.lock();
        try {
            // critical section
        } finally {
            //always release lock in a finally block to ensure it gets released even if an exception occurs
            lock.unlock();
        }
    }
    
    //tryLock
    public void someMethod() {
        if (lock.tryLock()) {
            try {
                // critical section
            } finally {
                lock.unlock();
            }
        } else {
            // handle case where lock is not available
        }
    }
    
    //also comes with a timeout version of tryLock that allows you to specify how long to wait for the lock before giving up
    //lock.tryLock(1, java.util.concurrent.TimeUnit.SECONDS)
    
    //lock.lockInterruptibly() allows a thread to be interrupted while waiting for the lock, which can help prevent deadlocks.
``` 

##### ReadWriteLock:
- Its useful when you have a shared resource thats **read more often** than its written to , say a cache.
- It has 2 locks , a read lock that allows multiple threads to acquire it simultaneously as long as no thread holds the write lock, 
- and a write lock that is exclusive and can only be held by one thread at a time.
- A read lock cannot be acquired if a thread holds the write lock, and a write lock cannot be acquired if any thread holds the read lock.
- This also has tryLock and lockInterruptibly methods like ReentrantLock.
```java
    private final Map<K,V> cache = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    public V get(K key) {
        rwLock.readLock().lock();
        try {
           return cache.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public void put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            cache.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
```

#### StampedLock:
- A more advanced lock that supports three modes: read, write, and optimistic read.
- It provides better performance than ReentrantReadWriteLock in scenarios with a high contention for read locks, as it allows multiple threads to read the shared resource simultaneously without blocking each other.
- So optimistic reads don't need to aquire a lock , once they read they check if there was a write while reading. If so only then they acquire a read lock and read again. 
- This can significantly improve performance in scenarios where reads are much more frequent than writes.
- **StampedeLock does not support reentracy**, meaning that a thread cannot acquire the same lock multiple times without releasing it first. This can lead to issues if not used carefully, as it may result in deadlocks or other synchronization problems if a thread tries to acquire a lock it already holds.
```java
    private final Map<K,V> cache = new HashMap<>();
    private final StampedLock lock = new StampedLock();
    
    public V get(K key) {
        long stamp = lock.tryOptimisticRead();
        V value = cache.get(key);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                value = cache.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return value;
    }
    
    public void put(K key, V value) {
        long stamp = lock.writeLock();
        try {
            cache.put(key, value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
```
#### Semaphore:
- controls access to a shared resource by maintaining a set of permits. 
- Before accessing the resource, a thread must acquire a permit (acquire()); 
- when done, it releases the permit (release()). 
- If no permits are available, acquire() blocks until one is released.
- Semaphore with a single permit can be used as a mutex.
```java
    private final Semaphore semaphore = new Semaphore(3); // allows up to 3 threads to access the resource concurrently
    
    public void accessResource() throws InterruptedException {
        semaphore.acquire();
        try {
            // access the shared resource
        } finally {
            semaphore.release();
        }
    }
```
#### CountdownLatch:
- A synchronization aid that allows one or more threads to wait until a set of operations being performed in other threads completes. 
- It is initialized with a count, and threads can call await() to wait until the count reaches zero.
- Syntax : 
```java
    CountDownLatch latch = new CountDownLatch(3); // initialize with count of 3
    
    // In worker threads:
    latch.countDown(); // call this when a task is completed to decrement the count
    
    // In waiting thread:
    latch.await(); // call this to wait until the count reaches zero
```
#### CyclicBarrier:
- A synchronization aid that allows a set of threads to all wait for each other to reach a common barrier point. 
- It is initialized with the number of threads that must wait at the barrier before they can proceed. 
- Once all threads have reached the barrier, they are released to continue their execution.
- Syntax:
```java
    CyclicBarrier barrier = new CyclicBarrier(3); // initialize with number of threads  
    // In worker threads:
    barrier.await(); // call this to wait at the barrier until all threads have reached it
```