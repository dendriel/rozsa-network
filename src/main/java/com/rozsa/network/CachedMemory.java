package com.rozsa.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

class CachedMemory {
    private final int maxBufCacheCount;
    private final Random random;

    private ReentrantLock bufCacheLock;
    private List<byte[]> bufCache;

    CachedMemory(int maxBufCacheCount) {
        this.maxBufCacheCount = maxBufCacheCount;
        random = new Random();
        bufCacheLock = new ReentrantLock();

        bufCache = new ArrayList<>();

    }

    byte[] allocBuffer(int minimumLength) {
        bufCacheLock.lock();
        try {
            for (int i = 0; i < bufCache.size(); i++) {
                if (bufCache.get(i).length < minimumLength) {
                    continue;
                }
                byte[] buf = bufCache.get(i);
                bufCache.remove(i);
                return buf;
            }
        } finally {
            bufCacheLock.unlock();
        }

        return new byte[minimumLength];
    }

    void freeBuffer(byte[] buf) {
        bufCacheLock.lock();
        try {
            if (bufCache.size() >= maxBufCacheCount) {
                // remove random buf entry if cache is full. This action avoids having many cached bufs of the same size
                // unnecessarily.
                int toRemoveBufIdx = random.nextInt(maxBufCacheCount);
                bufCache.remove(toRemoveBufIdx);
            }
            bufCache.add(buf);

        } finally {
            bufCacheLock.unlock();
        }
    }
}
