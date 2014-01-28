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
import android.util.Log;
import com.baasbox.android.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Created by Andrea Tortorella on 20/01/14.
 */
public abstract class Task<R> implements Runnable, Comparable<Task<R>> {

    final static void st(String step) {
        Log.e("STEPPER", step);
    }


    private AtomicBoolean taken = new AtomicBoolean(false);
    volatile BaasResult<R> result;
    int seqNumber;
    private Handler postOn;
    private Dispatcher dispatcher;
    private Priority priority;
    protected BaasBox box;
    private final AtomicReference<BaasHandler<?>> suspendableHandler = new AtomicReference<BaasHandler<?>>();
    volatile CountDownLatch latch;

    protected Task(Priority priority, BaasHandler<R> handler) {
        this.priority = priority == null ? Priority.NORMAL : priority;
        this.suspendableHandler.set(handler == null ? BaasHandler.NOOP : handler);
    }

    @Override
    public int compareTo(Task<R> another) {
        Priority me = priority;
        Priority you = another.priority;
        return me == you ?
                seqNumber - another.seqNumber :
                you.ordinal() - me.ordinal();
    }

    public int seq() {
        return seqNumber;
    }

    public void await(){
        latch=new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private enum Signal implements BaasHandler {
        ABORTED,
        SUSPENDED,
        DELIVERED,
        COMMITTED;

        @Override
        public void handle(BaasResult result) {
        }
    }


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
//            dispatcher.finish(this);
            return true;
        }
        return false;
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
//                dispatcher.finish(this);
                st("not resuming: already committed");
                return false;
            }
            boolean submit;
            if (curr == Signal.SUSPENDED) {
                // the request is suspended and it's
                // still in flight
                st("suspended still in flight");
                submit = false;
            } else if (curr == Signal.DELIVERED) {
                // the request is resumed after delivery
                // we than need to resubmit it to it's handler
                st("suspendend missing callback");
                submit = true;
            } else {
                // the request is not suspended
                // so we cannot resume it: just leave
                st("not suspended it's: " + curr);
                return false;
            }
            if (suspendableHandler.compareAndSet(curr, handler)) {
                // we now have an handler set
                if (submit) {
                    // if we where in a delivered status
                    // we need to repost the callback for execution
                    post();
                }
                st("can resume");
                return true;
            }
            st("resume recheck");
            // if we get here another thread modified the request status
            // so repeat
        }
    }


    @Override
    public final void run() {
        for (; ; ) {
            final BaasHandler<?> curr = suspendableHandler.get();
            // at this stage we are in the callback thread of execution
            // we get the current handler and choose the state to reach
            if (curr == Signal.COMMITTED) {
                //the callback has already run
                // really we don't have to make any work
                st("running finish already done");
                return;
            }

            final BaasHandler<?> target;
            if (curr == Signal.SUSPENDED || curr == Signal.DELIVERED) {
                //if the current state is SUSPENDED the target state
                //to reach is DELIVERED
                //handlers in DELIVERED state won't change and remain the same
                //so for both the cases the target state would be
                // DELIVERED and there would be no more work to do
                st("finishing in suspended state");
                target = Signal.DELIVERED;
            } else {
                // we have a real handler to execute
                // and we need to commit the execution
                st("will commit now");
                target = Signal.COMMITTED;
            }

            if (suspendableHandler.compareAndSet(curr, target)) {
                //at this point the transition is completed
                //if the target we reached is COMMITTED than we have
                //to execute the callback and cleanup the dispatcher
                st("last transition");
                if (target == Signal.COMMITTED) {

                    if (curr != null) {
                        st("handling");
                        ((BaasHandler<R>) curr).handle(result);
                    }
                    finish();
//                    dispatcher.finish(this);
                }
                return;

            }
            st("exec repeat");
        }
    }

    private void finish(){
        Logger.debug("FINISHING");
        dispatcher.finish(this);
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
                st("already suspended");
                return true;
            } else if (curr == Signal.COMMITTED || curr == Signal.ABORTED) {
                //if the request is committed we cannot cancel it anymore
                //the other case is that the handler has been erased but
                //just before removing the request from the ones still in flight
                // suspend is called in this case we are in a ABORTED status
                // so we cannot suspend
                st("cannot suspend");
                return false;
            } else if (suspendableHandler.compareAndSet(curr, Signal.SUSPENDED)) {
                // we had success transitioning to a suspended state;
                // the request is now suspended and the handler is unbound
                st("suspending");
                return true;
            }
            st("retry suspend");
        }
    }

    final boolean cancel() {
        if (!taken.get()) {
            result = BaasResult.cancel();
            return true;
        }
        return false;
    }

    final boolean isCanceled() {
        BaasResult<R> r = result;
        return r != null && r.isCanceled();
    }

    private boolean takeAndVerifyCancel() {
        taken.set(true);
        return isCanceled();
    }

    final boolean isSuspended() {
        BaasHandler<?> h = suspendableHandler.get();
        return h == Signal.SUSPENDED || h == Signal.DELIVERED;
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
        if (latch!=null){
            latch.countDown();
        }

    }

    protected abstract R asyncCall() throws BaasException;

    final void post() {
        postOn.post(this);
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
}
