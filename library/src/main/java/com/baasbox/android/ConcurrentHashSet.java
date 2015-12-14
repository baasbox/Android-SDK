package com.baasbox.android;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by aktor on 25/11/15.
 */
class ConcurrentHashSet<T> extends AbstractSet<T> {

    private final ConcurrentHashMap<T,T> mMap;

    ConcurrentHashSet(){
        mMap = new ConcurrentHashMap<>();
    }

    @Override
    public Iterator<T> iterator() {
        return mMap.keySet().iterator();
    }


    @Override
    public int size() {
        return mMap.size();
    }

    @Override
    public boolean contains(Object object) {
        return mMap.get(object) != null;
    }

    @Override
    public boolean add(T object) {
        return mMap.put(object,object)==null;
    }

    @Override
    public boolean remove(Object object) {
        return mMap.remove(object,object);
    }

    @Override
    public void clear() {
        mMap.clear();
    }


}
