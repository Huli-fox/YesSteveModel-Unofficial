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
}
