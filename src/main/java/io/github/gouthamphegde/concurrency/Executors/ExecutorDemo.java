package io.github.gouthamphegde.concurrency.Executors;

import java.util.concurrent.*;

public class ExecutorDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);

        executor.submit(() -> {
            System.out.println("Hello from the executor! Thread: " + Thread.currentThread().getName());
        });
        Callable<String> task = () -> {
            Thread.sleep(1000);
            return "Task result";
        };
        Future<String> ans = executor.submit(task);

        scheduled.scheduleAtFixedRate(() -> {
            System.out.println("Scheduled task running at: " + System.currentTimeMillis());
        }, 0, 2, TimeUnit.SECONDS);


        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(5, 10, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        threadPool.submit(task);
    }
}
