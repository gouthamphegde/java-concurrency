package io.github.gouthamphegde.concurrency.basics;

public final class ThreadBasicsExercise {

    private ThreadBasicsExercise() {
    }

    public static void run() throws InterruptedException {
        Thread[] workers = new Thread[5];

        for (int i = 0; i < workers.length; i++) {
            int workerNumber = i + 1;
            workers[i] = new Thread(() -> {
                for (int number = 1; number <= 10; number++) {
                    System.out.printf("worker-%d -> %d%n", workerNumber, number);
                }
            }, "worker-" + workerNumber);
            workers[i].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        System.out.println("All worker threads completed.");
    }
}

