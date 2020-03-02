package com.rozsa.network;

class Clock {
    static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    static long getCurrentTimeInNanos() {
        return System.nanoTime();
    }

    static long getTimePassedSince(long time) {
        return getCurrentTime() - time;
    }

    static long getTimePassedSinceInNanos(long time) {
        return getCurrentTimeInNanos() - time;
    }

    static long secondsToMillis(float seconds) {
        return (long)(seconds * 1000);
    }

    static long secondsToNanos(float seconds) {
        return (long)(seconds * 1000000000);
    }

    static long nanosToMillis(long nanos) {
        return nanos / 1000000;
    }

    static long nanosToMicros(long nanos) {
        return nanos / 1000;
    }
}
