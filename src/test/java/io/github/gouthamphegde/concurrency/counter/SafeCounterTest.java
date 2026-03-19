package io.github.gouthamphegde.concurrency.counter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

class SafeCounterTest {

    @Test
    void incrementsAreThreadSafe() throws InterruptedException {
        SafeCounter counter = new SafeCounter();

        int threadCount = 20;
        int incrementsPerThread = 10_000;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Thread.startVirtualThread(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();

        assertEquals(threadCount * incrementsPerThread, counter.getCount());
    }
}

