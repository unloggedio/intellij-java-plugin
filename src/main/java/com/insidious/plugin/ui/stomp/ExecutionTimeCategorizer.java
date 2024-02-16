package com.insidious.plugin.ui.stomp;

public class ExecutionTimeCategorizer {

    public static ExecutionTimeCategory categorizeExecutionTime(double timeInMilliseconds) {
        if (timeInMilliseconds <= 100) { // up to 100 ms
            return ExecutionTimeCategory.INSTANTANEOUS;
        } else if (timeInMilliseconds <= 500) { // up to 1 second (500 ms)
            return ExecutionTimeCategory.FAST;
        } else if (timeInMilliseconds <= 1000) { // up to 5 seconds (1000 ms)
            return ExecutionTimeCategory.MODERATE;
        } else if (timeInMilliseconds <= 10000) { // up to 10 seconds (10000 ms)
            return ExecutionTimeCategory.SLOW;
        } else {
            return ExecutionTimeCategory.GLACIAL;
        }
    }

    public static String formatTimePeriod(long timeInMilliseconds) {
        if (timeInMilliseconds < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }

        if (timeInMilliseconds < 1000) {
            return timeInMilliseconds + "㎳"; // Display in milliseconds
        } else if (timeInMilliseconds < 60 * 1000) {
            return (timeInMilliseconds / 1000) + "′"; // Display in seconds
        } else {
            return (timeInMilliseconds / 60000) + "″"; // Display in minutes
        }
    }

    public static void main(String[] args) {
        // Example usage
        double executionTime = 5.0; // Example execution time in seconds
        ExecutionTimeCategory category = categorizeExecutionTime(executionTime);
        System.out.println("The execution time category is: " + category);
    }
}