package io.github.gouthamphegde.concurrency.basics;

import java.util.ArrayList;
import java.util.List;

public class DeadlockDemo {

    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("T1: Holding Lock A");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
                synchronized (LOCK_B) {
                    // Waits for Lock B held by T2
                    System.out.println("T1: Holding Lock A & B");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("T2: Holding Lock B");
                try {
                    Thread.sleep(5000);
                    throw new RuntimeException();
                } catch (Exception e) {
                    System.out.println("T2: Interrupted while sleeping");
                }
                synchronized (LOCK_A) {
                    // Waits for Lock A held by T1
                    System.out.println("T2: Holding Lock A & B");
                }
            }
        });
        try {
            t2.start();
            t1.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
