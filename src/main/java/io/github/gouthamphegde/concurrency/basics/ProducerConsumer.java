package io.github.gouthamphegde.concurrency.basics;

import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumer {

    private final Queue<Integer> queue = new LinkedList<>();
    private static final int CAPACITY = 2;


    public synchronized void produce(int item) throws InterruptedException {
        while(queue.size() == CAPACITY) {
            System.out.println("T1: Waiting for queue to be empty");
            wait();
        }

        queue.offer(item);
        queue.offer(item);
        System.out.println("T1: Produced : "+item+" Queue size: "+queue.size());
        notifyAll();
    }

    public synchronized int consume() throws InterruptedException {
        while(queue.isEmpty()) {
            System.out.println("T2: Waiting for queue to be non empty");
            wait();
        }
        Integer item = queue.poll();
        System.out.println("T2: Consumed : "+item+" Queue size: "+queue.size());
        notifyAll();
        return item;
    }

    public static void main(String[] args) throws InterruptedException {
        ProducerConsumer pc = new ProducerConsumer();

        Thread producer = new Thread(()->{
            for(int i=1; i<=10; i++) {
                try {
                    pc.produce(i);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread consumer = new Thread(()->{
            for(int i=1; i<=10; i++) {
                try {
                    pc.consume();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        consumer.start();
        producer.start();
    }
}
