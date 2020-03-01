package com.rozsa.network;

class Clock {
    static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    static long getTimePassedSince(long time) {
        return getCurrentTime() - time;
    }

    static long secondsToMillis(float seconds) {
        return (long)(seconds * 1000);
    }
}
