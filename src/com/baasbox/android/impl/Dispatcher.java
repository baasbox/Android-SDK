/*
 * Copyright (C) 2014. BaasBox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android.impl;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.Logger;
import com.baasbox.android.RequestToken;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andrea Tortorella on 20/01/14.
 */
public final class Dispatcher {
    private final static AtomicInteger SEQUENCE = new AtomicInteger();


    private final PriorityBlockingQueue<Task<?>> taskQueue;
    private final ConcurrentHashMap<Integer, Task<?>> liveAsyncs;
    private final ExceptionHandler exceptionHandler;
    private final Worker[] workers;
    private final BaasBox box;
    private volatile boolean quit;

    final Handler defaultMainHandler = new Handler(Looper.getMainLooper());

    public Dispatcher(BaasBox box) {
        this.box = box;
        this.exceptionHandler = setHandler(box.config.EXCEPTION_HANDLER);
        this.workers = createWorkers(box.config.NUM_THREADS);
        this.taskQueue = new PriorityBlockingQueue<Task<?>>(16);
        this.liveAsyncs = new ConcurrentHashMap<Integer, Task<?>>(16, 0.75f, 1);
    }

    private static ExceptionHandler setHandler(ExceptionHandler handler) {
        if (handler == null) {
            Logger.warn("EXCEPTION_HANDLER is null: set using default");
            return ExceptionHandler.DEFAULT;
        }
        return handler;
    }

    private static Worker[] createWorkers(int threads) {
        if (threads < 0) {
            Logger.warn("Ignoring NUM_THREADS: less than 0 threads, default will be used");
            threads = 0;
        }
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors();
            Logger.info("Using default number of threads configuration %s", threads);
        }
        return new Worker[threads];
    }

    public void start() {
        stop();
        quit = false;
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(this);
            workers[i].start();
        }
    }

    public void stop() {
        quit = true;
        for (int i = 0; i < workers.length; i++) {
            if (workers[i] != null) {
                workers[i].interrupt();
                workers[i] = null;
            }
        }
    }

    void finish(Task<?> req) {
        this.liveAsyncs.remove(req.seqNumber, req);
        Logger.info("%s finished", req);
    }

    public RequestToken post(Task<?> request) {
        final int seqNumber = SEQUENCE.getAndIncrement();
        request.bind(seqNumber, this);
        RequestToken token = new RequestToken(seqNumber);
        liveAsyncs.put(seqNumber, request);
        taskQueue.add(request);
        return token;
    }

    public <R> boolean resume(RequestToken token, BaasHandler<R> handler) {
        Task<R> task = (Task<R>) liveAsyncs.get(token.requestId);
        if (task == null) {
            Task.st("not found");
            return false;
        }
        return task.resume(handler);
    }

    public boolean cancel(RequestToken token, boolean immediate) {
        Task<?> task = liveAsyncs.get(token.requestId);
        if (task == null) return false;
        if (immediate) {
            return task.abort();
        } else {
            return task.cancel();
        }
    }

    public boolean suspend(RequestToken token) {
        Task<?> task = liveAsyncs.get(token.requestId);
        return task != null && task.suspend();
    }


    private static final class Worker extends Thread {
        private final PriorityBlockingQueue<Task<?>> queue;
        private final Dispatcher dispatcher;

        Worker(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
            this.queue = dispatcher.taskQueue;
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Task<?> task;
            while (true) {
                try {
                    task = queue.take();
                } catch (InterruptedException e) {
                    if (dispatcher.quit) return;
                    continue;
                }
                try {
                    task.execute();
                    task.post();

                } catch (Throwable t) {
                    if (dispatcher.exceptionHandler.onError(t)) {
                        continue;
                    }
                }
            }
        }
    }

}
