package com.sd.lib.player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ObserverHolder<T>
{
    private final List<T> mList = new CopyOnWriteArrayList<>();

    public void add(T observer)
    {
        if (observer == null)
            return;
        if (mList.contains(observer))
            return;

        mList.add(observer);
    }

    public void remove(T observer)
    {
        mList.remove(observer);
    }

    public void foreach(ForeachCallback<T> callback)
    {
        for (T observer : mList)
        {
            callback.onNext(observer);
        }
    }

    public interface ForeachCallback<T>
    {
        void onNext(T observer);
    }
}
