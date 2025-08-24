package com.imagesorter;

public class MemUtils {
    public static String printHeapUsage() {
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();   // Current heap allocated to JVM
        long freeMemory  = runtime.freeMemory();    // Unused part of allocated heap

        long usedMemory  = (totalMemory - freeMemory)/(1024 * 1024);
        long maxMemory   = runtime.maxMemory()/(1024 * 1024);     // Max heap JVM can grow to (-Xmx)

        Double percent = (double) usedMemory / maxMemory * 100;

        return String.format("Used: %.2f%% /%d MB  ", percent, maxMemory );
    }

    public static void main(String[] args) {
        printHeapUsage();
    }
}
