package com.sbox.musicserviceplayer;

import android.app.Application;
import android.content.Context;

public class MpsApplication extends Application
{
    /**
     * 应用上下文
     */
    public static Context mContext = null;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mContext = this;
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
    }

    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
    }
}
