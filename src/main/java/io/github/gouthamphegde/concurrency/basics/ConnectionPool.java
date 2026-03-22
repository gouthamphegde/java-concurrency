package io.github.gouthamphegde.concurrency.basics;

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