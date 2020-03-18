package com.company;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private static Random random = new Random();
    private static Scanner scanner = new Scanner(System.in);
    static volatile boolean end = false;

    public static void main(String[] args) {

        Queue<Integer> queue = new LinkedList<>();
        Lock mutex = new ReentrantLock();
        Condition condition = mutex.newCondition();
        System.out.println("Enter number of Threads: ");
        int numTreads = scanner.nextInt();
        File file = new File("output.txt");
        if(!file.exists()) {
            FileWriter writer = null;
            try {
                writer = new FileWriter("output.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (file.length() / 1024 < 100) {

                int num = random.nextInt(1000) + 1;
                try {
                    if (writer != null) {
                        writer.write(num + System.getProperty("line.separator"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Thread producer = new Thread(new Producer(file, queue, mutex, condition));
        producer.start();
        Thread[] consumers = new Thread[numTreads];
        for (int i = 0; i < numTreads; i++) {
            consumers[i] = new Thread(new Consumer(queue, mutex, condition));
            consumers[i].start();
            System.out.println(consumers[i].getId() + " started");
        }
        for (int i = 0; i < numTreads; i++) {
            try {
                consumers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            producer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("This is the end");
    }
}


class Producer implements Runnable {

    private File file;
    private Queue<Integer> numbers;
    private Lock mutex;
    private Condition condition;

    public Producer(File file, Queue numbers, Lock mutex, Condition condition) {
        this.file = file;
        this.numbers = numbers;
        this.mutex = mutex;
        this.condition = condition;
    }

    @Override
    public void run() {
        try {

            FileReader fr = new FileReader(file);
            BufferedReader reader = new BufferedReader(fr);
            String line = reader.readLine();
            while (line != null) {
                int number = Integer.parseInt(line);
                System.out.println(Thread.currentThread().getId() +" прочитал " + number);
                mutex.lock();
                try {
                    numbers.add(number);
                    condition.signal();
                } finally {
                    mutex.unlock();
                }
                line = reader.readLine();
            }
            Main.end = true;
            mutex.lock();
            try {
                condition.signalAll();
            } finally {
                mutex.unlock();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Consumer implements Runnable {

    private Queue<Integer> numbers;
    private Lock mutex;
    private Condition condition;

    public Consumer(Queue numbers, Lock mutex, Condition condition) {
        this.numbers = numbers;
        this.mutex = mutex;
        this.condition = condition;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getId() + " i am here!");
        while (true) {
            try {
                mutex.lock();
                Integer number = numbers.poll();

                while (number == null) {
                    if (Main.end) {
                        System.out.println(Thread.currentThread().getId() + ": no elements");
                        return;
                    }
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    number = numbers.poll();
                }
                int fibNum = getFib(number);
                System.out.println(Thread.currentThread().getId() + ": Число Фиббоначи номер " + number + " равно " + fibNum);
            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                mutex.unlock();
            }
        }
    }

    private int getFib(int index) {
        int a = 0;
        int b = 1;
        for (int i = 2; i <= index; i++) {
            Integer next = a + b;
            a = b;
            b = next;
        }
        return b;
    }
}
