package io.github.gouthamphegde.concurrency.counter;

public class SafeCounter {
    private int count;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}

