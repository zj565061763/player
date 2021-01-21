package com.sd.lib.player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ObserverHolder<T>
{
    private final Map<T, String> mHolder = new ConcurrentHashMap<>();

    public void add(T observer)
    {
        if (observer == null)
            return;

        mHolder.put(observer, "");
    }

    public void remove(T observer)
    {
        if (observer == null)
            return;

        mHolder.remove(observer);
    }

    public boolean isEmpty()
    {
        return mHolder.isEmpty();
    }

    public void foreach(ForeachCallback<T> callback)
    {
        for (T observer : mHolder.keySet())
        {
            callback.onNext(observer);
        }
    }

    public interface ForeachCallback<T>
    {
        void onNext(T observer);
    }
}
