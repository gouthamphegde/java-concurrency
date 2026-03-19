package io.github.gouthamphegde.concurrency.counter;

/**
 * Intentionally unsafe counter used for race-condition practice.
 */
public class BrokenCounter {
    private int count;

    public void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}

