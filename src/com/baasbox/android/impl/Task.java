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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android.impl;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.baasbox.android.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Created by Andrea Tortorella on 20/01/14.
 */
public abstract class Task<R> implements Runnable, Comparable<Task<R>> {
// ------------------------------ FIELDS ------------------------------

    volatile BaasResult<R> result;
    int seqNumber;
    volatile CountDownLatch latch;
    protected BaasBox box;


    private AtomicBoolean taken = new AtomicBoolean(false);
    private Handler postOn;
    private Dispatcher dispatcher;
    private int priority;
    private final AtomicReference<BaasHandler<?>> suspendableHandler = new AtomicReference<BaasHandler<?>>();

// --------------------------- CONSTRUCTORS ---------------------------
    protected Task(int flags, BaasHandler<R> handler) {
        this.priority = parsePriority(flags);
        this.suspendableHandler.set(handler == null ? BaasHandler.NOOP : handler);
    }

    private static int parsePriority(int flags){
        return flags;
    }
// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;

        Task task = (Task) o;

        if (seqNumber != task.seqNumber) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return seqNumber;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Comparable ---------------------

    @Override
    public int compareTo(Task<R> another) {
        int me = priority;
        int you = another.priority;
        return me == you ?
                seqNumber - another.seqNumber :
                you - me;
    }

// --------------------- Interface Runnable ---------------------

    @Override
    public final void run() {
        for (; ; ) {
            final BaasHandler<?> curr = suspendableHandler.get();
            // at this stage we are in the callback thread of execution
            // we get the current handler and choose the state to reach
            if (curr == Signal.COMMITTED) {
                //the callback has already run
                // really we don't have to make any work
                return;
            }

            final BaasHandler<?> target;
            if (curr == Signal.SUSPENDED || curr == Signal.DELIVERED) {
                //if the current state is SUSPENDED the target state
                //to reach is DELIVERED
                //handlers in DELIVERED state won't change and remain the same
                //so for both the cases the target state would be
                // DELIVERED and there would be no more work to do
                target = Signal.DELIVERED;
            } else {
                // we have a real handler to execute
                // and we need to commit the execution
                target = Signal.COMMITTED;
            }

            if (suspendableHandler.compareAndSet(curr, target)) {
                //at this point the transition is completed
                //if the target we reached is COMMITTED than we have
                //to execute the callback and cleanup the dispatcher
                if (target == Signal.COMMITTED) {
                    if (curr != null) {
                        ((BaasHandler<R>) curr).handle(result);
                    }
                    finish();
                }
                return;
            }
        }
    }

// -------------------------- OTHER METHODS --------------------------

    final boolean abort() {
        if (!taken.get()) {
            // aborting always runs before connection
            // happens. If we reach this point the request
            // can be active or suspended
            // but nor delivered or committed
            result = BaasResult.cancel();
            // we can simply forcefully set the value
            // to ABORTED to let the resource been cleaned up
            suspendableHandler.set(Signal.ABORTED);
            finish();
            return true;
        }
        return false;
    }

    private void finish() {
        Logger.debug("FINISHING");
        dispatcher.finish(this);
    }

    public void await() {
        latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    final void bind(int seqNumber, Dispatcher dispatcher) {
        this.seqNumber = seqNumber;
        this.dispatcher = dispatcher;

        if (postOn == null) {
            Looper looper = Looper.myLooper();
            if (looper == Looper.getMainLooper() || looper == null) {
                this.postOn = dispatcher.defaultMainHandler;
            } else {
                this.postOn = new Handler(looper);
            }
        }
    }

    final boolean cancel() {
        if (!taken.get()) {
            result = BaasResult.cancel();
            return true;
        }
        return false;
    }

    final void execute() {
        if (!takeAndVerifyCancel()) {
            try {
                R value = asyncCall();
                result = BaasResult.success(value);
            } catch (BaasException e) {
                result = BaasResult.failure(e);
            }
        }
    }

    private boolean takeAndVerifyCancel() {
        taken.set(true);
        return isCanceled();
    }

    final boolean isCanceled() {
        BaasResult<R> r = result;
        return r != null && r.isCanceled();
    }

    protected abstract R asyncCall() throws BaasException;

    final boolean isSuspended() {
        BaasHandler<?> h = suspendableHandler.get();
        return h == Signal.SUSPENDED || h == Signal.DELIVERED;
    }

    boolean resume(BaasHandler<R> handler) {
        for (; ; ) {
            BaasHandler<?> curr = suspendableHandler.get();
            if (curr == Signal.COMMITTED) {
                //the request has already been committed
                //so we cannot resume it again
                //but if we found it than it's still known to the dispatcher
                //so we simply cleanup it
                finish();
                return false;
            }
            boolean submit;
            if (curr == Signal.SUSPENDED) {
                // the request is suspended and it's
                // still in flight
                submit = false;
            } else if (curr == Signal.DELIVERED) {
                // the request is resumed after delivery
                // we than need to resubmit it to it's handler
                submit = true;
            } else {
                // the request is not suspended
                // so we cannot resume it: just leave
                return false;
            }
            if (suspendableHandler.compareAndSet(curr, handler)) {
                // we now have an handler set
                if (submit) {
                    // if we where in a delivered status
                    // we need to repost the callback for execution
                    post();
                }
                return true;
            }
            // if we get here another thread modified the request status
            // so repeat
        }
    }

    final void post() {
        postOn.post(this);
    }

    public int seq() {
        return seqNumber;
    }

    boolean suspend() {
        for (; ; ) {
            BaasHandler<?> curr = suspendableHandler.get();
            // we want suspend to be an idempotent action
            // if we suspend multiple times a request
            // this should not fail and do not update the status
            if (curr == Signal.SUSPENDED || curr == Signal.DELIVERED) {
                // the callback is already suspended
                // in one of the three states:
                //   * SUSPENDED still in flight
                //   * DELIVERED completed needs wakeup
                //   * ABORTED  non resumable
                // we had success
                return true;
            } else if (curr == Signal.COMMITTED || curr == Signal.ABORTED) {
                //if the request is committed we cannot cancel it anymore
                //the other case is that the handler has been erased but
                //just before removing the request from the ones still in flight
                // suspend is called in this case we are in a ABORTED status
                // so we cannot suspend
                return false;
            } else if (suspendableHandler.compareAndSet(curr, Signal.SUSPENDED)) {
                // we had success transitioning to a suspended state;
                // the request is now suspended and the handler is unbound
                return true;
            }
        }
    }

    final void unlock() {
        if (latch != null) {
            latch.countDown();
        }
    }

// -------------------------- ENUMERATIONS --------------------------

    private enum Signal implements BaasHandler {
        ABORTED,
        SUSPENDED,
        DELIVERED,
        COMMITTED;

        @Override
        public void handle(BaasResult result) {
            // those are signals they do nothing
        }
    }
}
