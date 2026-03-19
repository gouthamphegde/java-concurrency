package io.github.gouthamphegde.concurrency.virtualthreads;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class VirtualThreadPing {

    private VirtualThreadPing() {
    }

    public static void run(int taskCount) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= taskCount; i++) {
                int taskId = i;
                executor.submit(() -> {
                    TimeUnit.MILLISECONDS.sleep(50);
                    System.out.printf("task-%d executed by %s%n", taskId, Thread.currentThread());
                    return taskId;
                });
            }
        }

        System.out.printf("Completed %d virtual-thread tasks.%n", taskCount);
    }
}

