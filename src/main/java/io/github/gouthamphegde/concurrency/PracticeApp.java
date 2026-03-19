package io.github.gouthamphegde.concurrency;

import io.github.gouthamphegde.concurrency.basics.ThreadBasicsExercise;
import io.github.gouthamphegde.concurrency.virtualthreads.VirtualThreadPing;

/**
 * Small entry point so you can run one practice topic quickly.
 */
public final class PracticeApp {

    private PracticeApp() {
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String topic = args[0].trim().toLowerCase();
        switch (topic) {
            case "threads" -> ThreadBasicsExercise.run();
            case "virtual" -> VirtualThreadPing.run(20);
            default -> {
                System.out.println("Unknown topic: " + args[0]);
                printUsage();
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: mvn exec:java -Dexec.mainClass=io.github.gouthamphegde.concurrency.PracticeApp -Dexec.args=\"<topic>\"");
        System.out.println("Topics:");
        System.out.println("  threads  - run basic thread interleaving demo");
        System.out.println("  virtual  - run virtual thread demo");
    }
}

