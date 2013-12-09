package com.sbox.musicserviceplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

public class MyToast
{
    private static Toast mToast = null;

    @SuppressLint("ShowToast")
    public static Toast makeText(Context context, CharSequence text,
            int duration)
    {
        if (mToast == null)
        {
            mToast = Toast.makeText(context, text, duration);
        }
        else
        {
            mToast.setText(text);
            mToast.setDuration(duration);
        }

        return mToast;
    }

    public static void cancel()
    {
        if (mToast != null)
        {
            mToast.cancel();
        }
    }
}