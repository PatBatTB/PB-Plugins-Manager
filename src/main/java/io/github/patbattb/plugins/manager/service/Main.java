package io.github.patbattb.plugins.manager.service;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Runnable runOne = () -> {
            System.out.println("runOne starts");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        Runnable runTwo = () -> {
            System.out.println("runTwo starts");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        try (PluginExecutor executor = new PluginExecutor(5)) {
            executor.invoke("one", runOne);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executor.invoke("two", runTwo);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executor.invoke("one", runOne);
            try {
                TimeUnit.SECONDS.sleep(6);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executor.invoke("one", runOne);
            try {
                TimeUnit.SECONDS.sleep(6);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executor.shutdown();
        }
    }
}
