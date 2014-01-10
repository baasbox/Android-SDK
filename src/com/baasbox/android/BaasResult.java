package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;

/**
 * Created by eto on 23/12/13.
 */
public abstract class BaasResult<T> {

    public static<T> BaasResult<T> failure(BAASBoxException t ){
        return new Immediate<T>(null,t);
    }

    public static<T> BaasResult<T> success(T result){
        return new Immediate<T>(result,null);
    }

    public static class Immediate<T> extends BaasResult<T>{

        private final T value;
        private final BAASBoxException error;

        Immediate(T value,BAASBoxException error){
            this.value =value;
            this.error =error;
        }

        @Override
        public boolean isFailed() {
            return error!=null;
        }

        @Override
        public boolean isSuccess() {
            return error == null;
        }

        @Override
        public BAASBoxException error() {
            return error;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public T get() throws BAASBoxException {
            if (error!=null)throw  error;
            return value;
        }
    }


    public abstract boolean isFailed();
    public abstract boolean isSuccess();

    public boolean isCanceled(){
        return false;
    }

    public boolean isPending(){
        return false;
    }


    public abstract BAASBoxException error();

    public abstract T value();

    public abstract T get() throws BAASBoxException;

}
