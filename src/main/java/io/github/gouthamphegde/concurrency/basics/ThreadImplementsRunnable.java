package io.github.gouthamphegde.concurrency.basics;

public class ThreadImplementsRunnable implements Runnable {


    @Override
    public void run() {
        System.out.println("Running in thread: " + Thread.currentThread().getName());
    }
}
