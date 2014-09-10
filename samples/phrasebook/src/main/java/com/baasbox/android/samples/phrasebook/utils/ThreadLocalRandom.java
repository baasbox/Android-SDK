/*
 * Copyright (C) 2014.
 *
 * BaasBox - info@baasbox.com
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

package com.baasbox.android.samples.phrasebook.utils;

import java.util.Random;

/**
 * Created by Andrea Tortorella on 09/09/14.
 */
public class ThreadLocalRandom extends Random {

    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) -1;

    private long rnd;

    boolean initialized;

    private long pad0,pad1,pad2,pad3,pad4,pad5,pad6,pad7;

    private static final ThreadLocal<ThreadLocalRandom> LOCAL_RANDOM =
            new ThreadLocal<ThreadLocalRandom>() {
                @Override
                protected ThreadLocalRandom initialValue() {
                    return new ThreadLocalRandom();
                }
            };

    ThreadLocalRandom(){
        super();
        initialized = true;
    }

    public static ThreadLocalRandom current(){
        return LOCAL_RANDOM.get();
    }

    public void setSeed(long seed) {
        if (initialized){
            throw new UnsupportedOperationException();
        }
        rnd = (seed ^ MULTIPLIER) & MASK;
    }

    protected int next(int bits) {
        rnd = (rnd * MULTIPLIER + ADDEND)& MASK;
        return (int) (rnd >>> (48-bits));
    }

    public int nextInt(int least,int bound) {
        if (least >= bound){
            throw new IllegalArgumentException();
        }
        return nextInt(bound-least)+least;
    }

    public long nextLong(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        long offset = 0;
        while (n >= Integer.MAX_VALUE) {
            int bits = next(2);
            long half = n >>> 1;
            long nextn = ((bits & 2) == 0) ? half : n-half;
            if ((bits & 1) == 0) {
                offset += n -nextn;
            }
            n = nextn;
        }
        return offset + nextInt((int)n);
    }

    public long nextLong(long least,long bound){
        if (least >= bound){
            throw new IllegalArgumentException();
        }
        return nextLong((bound-least)) + least;
    }

    public double nextDouble(double n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        return nextDouble()*n;
    }

    public double nextDouble(double least,double bound) {
        if (least>=bound){
            throw new IllegalArgumentException();
        }
        return nextDouble()*(bound-least)+least;
    }

    private static final long serialVersionUID = -5851777807851030925L;
}
