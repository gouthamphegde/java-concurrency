package io.github.gouthamphegde.concurrency.basics;

public class ThreadMainApp {

    public static void main(String[] args) {
        //threadInitializationBasics();
        tutorial2A();
    }

    private static void threadInitializationBasics() {
        Thread thread = new MyThread();
        thread.start();


        //Thread with a runnable
        Thread runnableThread = new Thread(new ThreadImplementsRunnable());
        runnableThread.start();

        //Using lambda
        Thread lambdaThread = new Thread(() -> {
            System.out.println("Hello World from "+Thread.currentThread().getName());
        });

        lambdaThread.start();

        System.out.println("Running in: " + Thread.currentThread().getName());
    }

    private static void tutorial2A() {
        //The order of thread running is non-deterministic. You will see different output each time you run the program.
        for (int i = 0; i <=5; i++) {
            final int threadNumber = i;
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    System.out.println("Thread " + threadNumber + "j: "+j);
                }
            },"Worker-"+i).start();
        }
    }
}
