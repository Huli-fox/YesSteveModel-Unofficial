package com.fox.ysmu.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ThreadTools {

    @SuppressWarnings("all")
    public static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(
        0,
        10,
        30,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue());

    public static void configureThreadCount(int maxThreads) {
        if (THREAD_POOL instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) THREAD_POOL;
            int safeMax = Math.max(1, maxThreads);
            executor.setMaximumPoolSize(safeMax);
        }
    }
}
