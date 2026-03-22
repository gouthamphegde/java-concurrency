package io.github.gouthamphegde.concurrency.basics;

public class ThreadControlMethods {

    public static void main(String[] args) throws InterruptedException {
        Thread mainThread = Thread.currentThread();

        Thread worker = new Thread(() -> {
            try {
                System.out.println("Worker started");
                Thread.sleep(5000);
                System.out.println("Worker completed");
            }catch(InterruptedException e) {
                System.out.println("Worker interrupted");
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        System.out.println("State after start : "+worker.getState());

        Thread.sleep(1000);
        System.out.println("State during sleep : "+worker.getState());

        Thread monitor = new Thread(() -> {
            try {
                while (worker.isAlive()) {
                    System.out.println(
                            "Monitor -> main: " + mainThread.getState() +
                                    ", worker: " + worker.getState()
                    );
                    Thread.sleep(1000);
                }
                System.out.println(
                        "Monitor -> main after join: " + mainThread.getState() +
                                ", worker: " + worker.getState()
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "monitor");

        Thread daemon = new Thread(() -> {
            while (true) {
                System.out.println("Background task...");
                try { Thread.sleep(10000);
                System.out.println("Thread finished");}
                catch (InterruptedException e) {
                    break; }
            }
        });
        daemon.setDaemon(true); // Must set BEFORE start()
        daemon.start();

        monitor.start();
        System.out.println("Main is about to call join()");
        worker.join();
        //Also log main thread's state
        System.out.println("State after join : "+Thread.currentThread().getState());
        System.out.println("State after join : "+worker.getState());
        monitor.join();
    }
}
