package com.sbox.musicserviceplayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.sbox.baselib.ApkHelper;
import com.sbox.musicserviceplayer.MainActivity.PlayOrder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.util.Log;

public class PlayMusicService extends Service
{
    private static final String TAG = "PlayMusicService";
    private boolean mReflectFlg = false;
    public static final String ACTION_FOREGROUND = "com.example.android.apis.FOREGROUND";
    public static final String ACTION_BACKGROUND = "com.example.android.apis.BACKGROUND";
    private static final Class<?>[] mSetForegroundSignature = { Boolean.TYPE };
    private static final Class<?>[] mStartForegroundSignature = { Integer.TYPE,
            Notification.class };
    private static final Class<?>[] mStopForegroundSignature = { Boolean.TYPE };
    private NotificationManager mNM;
    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private static final int NOTIFICATION_ID = 999;
    private Notification notification;
    public static MediaPlayer player;
    private String pp;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate()
    {
        Log.i("sbox", "service onCreate");
        mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        try
        {
            mStartForeground = PlayMusicService.class.getMethod(
                    "startForeground", mStartForegroundSignature);
            mStopForeground = PlayMusicService.class.getMethod(
                    "stopForeground", mStopForegroundSignature);
        }
        catch (NoSuchMethodException e)
        {
            mStartForeground = mStopForeground = null;
        }

        try
        {
            mSetForeground = getClass().getMethod("setForeground",
                    mSetForegroundSignature);
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }
        displayNotification();

        DestoryPlayer();
        player = new MediaPlayer();
        player.setOnCompletionListener(new MyPlayerListener());
    }

    void startForegroundCompat(int id, Notification notification)
    {
        if (mReflectFlg)
        {
            // If we have the new startForeground API, then use it.
            if (mStartForeground != null)
            {
                mStartForegroundArgs[0] = Integer.valueOf(id);
                mStartForegroundArgs[1] = notification;
                invokeMethod(mStartForeground, mStartForegroundArgs);
                return;
            }

            // Fall back on the old API.
            mSetForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mSetForeground, mSetForegroundArgs);
            mNM.notify(id, notification);
        }
        else
        {
            /*
             * 还可以使用以下方法，当sdk大于等于5时，调用sdk现有的方法startForeground设置前台运行，
             * 否则调用反射取得的sdk level 5（对应Android 2.0）以下才有的旧方法setForeground设置前台运行
             */

            if (VERSION.SDK_INT >= 5)
            {
                startForeground(id, notification);
            }
            else
            {
                // Fall back on the old API.
                mSetForegroundArgs[0] = Boolean.TRUE;
                invokeMethod(mSetForeground, mSetForegroundArgs);
                mNM.notify(id, notification);
            }
        }
    }

    void stopForegroundCompat(int id)
    {
        if (mReflectFlg)
        {
            // If we have the new stopForeground API, then use it.
            if (mStopForeground != null)
            {
                mStopForegroundArgs[0] = Boolean.TRUE;
                invokeMethod(mStopForeground, mStopForegroundArgs);
                return;
            }

            // Fall back on the old API. Note to cancel BEFORE changing the
            // foreground state, since we could be killed at that point.
            mNM.cancel(id);
            mSetForegroundArgs[0] = Boolean.FALSE;
            invokeMethod(mSetForeground, mSetForegroundArgs);
        }
        else
        {
            /*
             * 还可以使用以下方法，当sdk大于等于5时，调用sdk现有的方法stopForeground停止前台运行， 否则调用反射取得的sdk
             * level 5（对应Android 2.0）以下才有的旧方法setForeground停止前台运行
             */

            if (VERSION.SDK_INT >= 5)
            {
                stopForeground(true);
            }
            else
            {
                mNM.cancel(id);
                mSetForegroundArgs[0] = Boolean.FALSE;
                invokeMethod(mSetForeground, mSetForegroundArgs);
            }
        }
    }

    private void invokeMethod(Method method, Object[] args)
    {
        try
        {
            method.invoke(this, args);
        }
        catch (InvocationTargetException e)
        {
            // Should not happen.
            Log.w("ApiDemos", "Unable to invoke method", e);
        }
        catch (IllegalAccessException e)
        {
            // Should not happen.
            Log.w("ApiDemos", "Unable to invoke method", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        int flag = intent.getFlags();
        pp = intent.getStringExtra("option");
        switch (flag)
        {
            case R.id.btn_next:
            {
                play();
                break;
            }
            case R.id.btn_previous:
            {
                play();
                break;
            }
            case R.id.btn_stop:
            {
                stop();
                System.out.println("---dfadsfads----");
                break;
            }
            case R.id.btn_play_puase:
            {
                if ("play".equals(pp))
                {
                    play();
                }
                else if ("pause".equals(pp))
                {
                    pause();
                }
                else
                {
                    resume();
                }
                break;
            }
            case -1:
            {
                break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        DestoryPlayer();
        stopForegroundCompat(NOTIFICATION_ID);
        Log.i("sbox", "onDestroy()");
    }

    public void DestoryPlayer()
    {
        if (player != null)
        {
            player.stop();
            player.release();
            player = null;
        }
    }

    private final class MyPlayerListener implements OnCompletionListener
    {
        public void onCompletion(MediaPlayer mp)
        {
            autoNext();
        }
    }

    private void autoNext()
    {
        if (MainActivity.currentPlayOrder.equals(PlayOrder.LOOP))
        {
            MainActivity.current = (MainActivity.current + 1)
                    % MainActivity.musicList.size();
        }
        else if (MainActivity.currentPlayOrder.equals(PlayOrder.ORDER))
        {
            if (MainActivity.current == MainActivity.musicList.size() - 1)
            {
                stop();
                return;
            }
            else
            {
                MainActivity.current = (MainActivity.current + 1)
                        % MainActivity.musicList.size();
            }
        }
        else if (MainActivity.currentPlayOrder.equals(PlayOrder.SINGLE_LOOP))
        {

        }
        else if (MainActivity.currentPlayOrder.equals(PlayOrder.RANDOM))
        {
            MainActivity.SetCurrentPos();
        }
        else
        {
            MainActivity.current = MainActivity.current - 1 < 0 ? MainActivity.musicList
                    .size() - 1 : MainActivity.current - 1;
        }

        play();
    }

    public void play()
    {
        try
        {
            // 重播
            player.reset();
            // 获取歌曲路径
            player.setDataSource((String) MainActivity.musicList.get(
                    MainActivity.current).get(UserHelper.Path));
            // 缓冲
            player.prepare();
            // 开始播放
            player.start();
            // 设置进度条长度
            MainActivity.nameTextView.setText((String) MainActivity.musicList
                    .get(MainActivity.current).get("title"));
            MainActivity.isPause = false;
            MainActivity.isStop = false;
            MainActivity.seekBar.setMax(PlayMusicService.player.getDuration());
            MainActivity.handler.post(new Runnable()
            {
                public void run()
                {
                    // 更新进度条状态
                    if (!MainActivity.isStartTrackingTouch)
                    {
                        try
                        {
                            MainActivity.seekBar
                                    .setProgress(PlayMusicService.player
                                            .getCurrentPosition());
                            MainActivity.curProgress.setText(Utils
                                    .ConvertHHMMSS(PlayMusicService.player
                                            .getCurrentPosition()));
                            MainActivity.laveProgress.setText(Utils
                                    .ConvertHHMMSS(PlayMusicService.player
                                            .getDuration()
                                            - PlayMusicService.player
                                                    .getCurrentPosition()));

                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    // 1秒之后再次发送
                    MainActivity.handler.postDelayed(this, 1000);
                }
            });
            displayNotification();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void displayNotification()
    {
        Intent startIntent = ApkHelper.getAppByPackageName(
                getApplicationContext(),
                MpsApplication.mContext.getPackageName());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                startIntent, 0);

        notification = new Notification(R.drawable.ic_launcher,
                "SBOX Player is runing", System.currentTimeMillis());
        String title = "Player is runing background";
        if (MainActivity.musicList != null && MainActivity.musicList.size() > 0)
        {
            title = (String) MainActivity.musicList.get(MainActivity.current)
                    .get(UserHelper.Artist)
                    + " - "
                    + (String) MainActivity.musicList.get(MainActivity.current)
                            .get(UserHelper.Title);
        }

        notification.setLatestEventInfo(this, "SBOX Player", title,
                contentIntent);

        startForegroundCompat(NOTIFICATION_ID, notification);
    }

    private void resume()
    {
        player.start();
    }

    private void stop()
    {
        player.stop();
    }

    private void pause()
    {
        if (player != null && player.isPlaying())
        {
            player.pause();
        }
    }

}
