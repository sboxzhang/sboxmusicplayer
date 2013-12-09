package com.sbox.musicserviceplayer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import com.sbox.musicserviceplayer.R;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity
{
    private PlayerListAdapter listItemAdapter;
    private ListView listView;
    private ScanSdReceiver scanSdReceiver;
    private Timer tExit = new Timer();
    private ExitTimerTask exitTimerTask = new ExitTimerTask();
    private ImageButton ppButton;
    private AudioManager mAudioManager;
    private HeadsetPlugReceiver headsetPlugReceiver;
    private PhoneStateReceiver phoneReceiver;
    private Intent serviceIntent;

    public static ArrayList<HashMap<String, Object>> musicList;
    public static Handler handler = new Handler();
    public static TextView nameTextView;
    public static TextView curProgress;
    public static TextView laveProgress;
    public static SeekBar seekBar;
    public static int current;
    public static boolean isPause;
    public static boolean isStartTrackingTouch;
    public static boolean isStop = true;
    public static boolean isFirst = true;
    public static String currentPlayOrder;
    public static Cursor mAudioCursor;

    public class PlayOrder
    {
        /**
         * 列表循环播放
         */
        public static final String LOOP = "loop";

        /**
         * 随机播放
         */
        public static final String RANDOM = "random";

        /**
         * 单曲循环
         */
        public static final String SINGLE_LOOP = "singleloop";

        /**
         * 顺序播放
         */
        public static final String ORDER = "order";

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        curProgress = (TextView) findViewById(R.id.current_time);
        laveProgress = (TextView) findViewById(R.id.lave_time);
        listView = (ListView) findViewById(android.R.id.list);
        try
        {
            currentPlayOrder = GetShareData("PlayOrder").toString();
        }
        catch (Exception e)
        {
            currentPlayOrder = PlayOrder.LOOP;
            SaveShareData("PlayOrder", PlayOrder.LOOP);
        }
        Utils.showWaitingDialog(this, "", "正在初始化音乐列表");
        new Thread(new myThread(1)).start();
        nameTextView = (TextView) findViewById(R.id.lbl_music_title);
        seekBar = (SeekBar) findViewById(R.id.music_seekbar);
        ppButton = (ImageButton) findViewById(R.id.btn_play_puase);

        seekBar.setOnSeekBarChangeListener(new MusicSeekBarListener());
        // 创建一个电话服务
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        manager.listen(new MyPhoneStateListener(),
                PhoneStateListener.LISTEN_CALL_STATE);

        registerHeadsetPlugReceiver();
        registerPhoneStateReceiver();

    }

    public void SaveShareData(String key, String value)
    {
        SharedPreferences.Editor sharedata = getSharedPreferences("data", 0)
                .edit();
        sharedata.putString(key, value);
        sharedata.commit();
    }

    public Object GetShareData(String key)
    {
        Object retValue;
        SharedPreferences sharedata = getSharedPreferences("data", 0);
        retValue = sharedata.getString(key, null);
        return retValue;
    }

    private void registerHeadsetPlugReceiver()
    {
        headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(headsetPlugReceiver, intentFilter);
    }

    public class HeadsetPlugReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (PlayMusicService.player != null)
            {
                if (intent.hasExtra("state"))
                {
                    if (intent.getIntExtra("state", 0) == 0)
                    {
                        if (PlayMusicService.player.isPlaying())
                        {
                            pause();
                        }
                    }
                    else if (intent.getIntExtra("state", 0) == 1)
                    {
                        if (isPause)
                        {
                            resume();
                        }
                        else
                        {
                            play();
                        }
                    }
                }
            }
        }
    }

    private final class MyPhoneStateListener extends PhoneStateListener
    {
        public void onCallStateChanged(int state, String incomingNumber)
        {
            if (PlayMusicService.player != null)
            {
                switch (state)
                {
                    case TelephonyManager.CALL_STATE_IDLE:
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        pause();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        pause();
                    default:
                        break;
                }
            }
        }
    }

    private void registerPhoneStateReceiver()
    {
        phoneReceiver = new PhoneStateReceiver();
        IntentFilter filter = new IntentFilter();
        // 播出电话暂停音乐播放
        filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        registerReceiver(phoneReceiver, filter);
    }

    public final class PhoneStateReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            if (PlayMusicService.player != null)
            {
                pause();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_refresh_music:
            {
                Utils.showWaitingDialog(this, "", "正在刷新音乐库...");
                scanSdCard();
                break;
            }
            case R.id.action_loop:
            {
                currentPlayOrder = PlayOrder.LOOP;
                SaveShareData("PlayOrder", PlayOrder.LOOP);
                break;
            }
            case R.id.action_order:
            {
                currentPlayOrder = PlayOrder.ORDER;
                SaveShareData("PlayOrder", PlayOrder.ORDER);
                break;
            }
            case R.id.action_random:
            {
                currentPlayOrder = PlayOrder.RANDOM;
                SaveShareData("PlayOrder", PlayOrder.RANDOM);
                break;
            }
            case R.id.action_singleloop:
            {
                currentPlayOrder = PlayOrder.SINGLE_LOOP;
                SaveShareData("PlayOrder", PlayOrder.SINGLE_LOOP);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void GetMusicList()
    {
        mAudioCursor = this.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,// 字段　没有字段　就是查询所有信息　相当于SQL语句中的　“
                "(" + MediaStore.Audio.AudioColumns.SIZE + "/1000/60) >0", // 查询条件
                null, // 条件的对应?的参数
                MediaStore.Audio.AudioColumns.TITLE);// 排序方式

        musicList = new ArrayList<HashMap<String, Object>>();

        // 获取表的内容
        while (mAudioCursor.moveToNext())
        {
            HashMap<String, Object> map = new HashMap<String, Object>();

            int indexTitle = mAudioCursor
                    .getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
            int indexDuration = mAudioCursor
                    .getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
            int indexSize = mAudioCursor
                    .getColumnIndex(MediaStore.Audio.AudioColumns.SIZE);
            int indexPath = mAudioCursor
                    .getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
            int indexAuthor = mAudioCursor
                    .getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            int indexDate = mAudioCursor
                    .getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED);

            map.put(UserHelper.Path, mAudioCursor.getString(indexPath));
            map.put(UserHelper.Title, mAudioCursor.getString(indexTitle));
            map.put(UserHelper.Duration,
                    Utils.ConvertHHMMSS(mAudioCursor.getInt(indexDuration)));
            map.put(UserHelper.Size,
                    Utils.m2(mAudioCursor.getDouble(indexSize) / 1024 / 1024)
                            + "M");
            map.put(UserHelper.Artist, mAudioCursor.getString(indexAuthor));
            map.put(UserHelper.Date,
                    Utils.ConvertYYYYMMDD(mAudioCursor.getInt(indexDate)));

            musicList.add(map);
        }
    }

    public void BindListView()
    {
        if (musicList != null)
        {
            listItemAdapter = new PlayerListAdapter(this, musicList);
            listView.setAdapter(listItemAdapter);
            listView.setOnItemClickListener(new itemListener());

            // listItemAdapter = new SimpleAdapter(this, musicList,
            // R.layout.listview,
            // new String[] { UserHelper.Title, UserHelper.Size,
            // UserHelper.Artist, UserHelper.Date }, new int[] {
            // R.id.listview_music_title,
            // R.id.listview_music_size, R.id.listview_artist,
            // R.id.listview_date });

        }
    }

    public void RefreshData()
    {
        listItemAdapter.notifyDataSetChanged();
    }

    private final class itemListener implements OnItemClickListener
    {
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id)
        {
            current = position;
            play();
        }
    }

    public void PausePlay(View view)
    {
        if (isStop)
        {
            play();
        }
        else if (!isPause)
        {
            pause();
        }
        else
        {
            resume();
        }
    }

    public void Stop(View view)
    {
        ppButton.setImageResource(R.drawable.play);
        stop();
    }

    public void Next(View view)
    {
        next();
    }

    public void Previous(View view)
    {
        previous();
    }

    private void previous()
    {
        if (isCanPlay())
        {
            if (this.currentPlayOrder.equals(PlayOrder.LOOP)
                    || this.currentPlayOrder.equals(PlayOrder.SINGLE_LOOP)
                    || currentPlayOrder.equals(PlayOrder.ORDER))
            {
                current = current - 1 < 0 ? musicList.size() - 1 : current - 1;
            }
            else if (this.currentPlayOrder.equals(PlayOrder.RANDOM))
            {
                SetCurrentPos();
            }
            else
            {
                current = current - 1 < 0 ? musicList.size() - 1 : current - 1;
            }
            play();
        }
        else
        {
            ShowMessage("音乐列表是空的，请添加音乐文件");
        }
    }

    private void next()
    {
        if (isCanPlay())
        {
            if (this.currentPlayOrder.equals(PlayOrder.LOOP)
                    || this.currentPlayOrder.equals(PlayOrder.SINGLE_LOOP)
                    || this.currentPlayOrder.equals(PlayOrder.ORDER))
            {
                current = (current + 1) % musicList.size();
            }
            else if (this.currentPlayOrder.equals(PlayOrder.RANDOM))
            {
                SetCurrentPos();
            }
            else
            {
                current = current - 1 < 0 ? musicList.size() - 1 : current - 1;
            }

            play();
        }
        else
        {
            ShowMessage("音乐列表是空的，请添加音乐文件");
        }
    }

    private boolean isCanPlay()
    {
        if (musicList == null || musicList.size() < 1)
            return false;
        else
            return true;
    }

    private void play()
    {
        try
        {
            if (isCanPlay())
            {
                serviceIntent = new Intent();
                serviceIntent.setClass(this, PlayMusicService.class);
                serviceIntent.setFlags(R.id.btn_play_puase);
                serviceIntent.putExtra("option", "play");
                startService(serviceIntent);
                ppButton.setImageResource(R.drawable.pause);// 播放按钮样式
            }
            else
            {
                Utils.showToast("音乐列表是空的，请添加音乐文件");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void resume()
    {
        if (isPause)
        {
            serviceIntent = new Intent(this, PlayMusicService.class);
            serviceIntent.setFlags(R.id.btn_play_puase);
            serviceIntent.putExtra("option", "resume");
            startService(serviceIntent);
            isPause = false;
            isStop = false;
            ppButton.setImageResource(R.drawable.pause);
        }
    }

    private void stop()
    {
        if (!isStop)
        {
            seekBar.setMax(0);
            seekBar.setProgress(0);
            isPause = false;
            isStop = true;

            serviceIntent = new Intent(this, PlayMusicService.class);
            serviceIntent.setFlags(R.id.btn_stop);
            serviceIntent.putExtra("option", "");
            startService(serviceIntent);
        }
    }

    private void startServiceDoNothing()
    {
        serviceIntent = new Intent(this, PlayMusicService.class);
        serviceIntent.setFlags(-1);
        serviceIntent.putExtra("option", "");
        startService(serviceIntent);
    }

    private void pause()
    {
        if (PlayMusicService.player != null
                && PlayMusicService.player.isPlaying())
        {
            serviceIntent = new Intent(this, PlayMusicService.class);
            serviceIntent.setFlags(R.id.btn_play_puase);
            serviceIntent.putExtra("option", "pause");
            startService(serviceIntent);

            isPause = true;
            isStop = false;
            ppButton.setImageResource(R.drawable.play);
        }
    }

    public long GetCurrentSecond()
    {
        if (PlayMusicService.player.getCurrentPosition() > 0)
        {
            return PlayMusicService.player.getCurrentPosition() / 1000;
        }
        return 0;
    }

    public long GetCurrentMillisecond()
    {
        if (PlayMusicService.player.getCurrentPosition() > 0)
        {
            return PlayMusicService.player.getCurrentPosition();
        }
        return 0;
    }

    private void scanSdCard()
    {
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        scanSdReceiver = new ScanSdReceiver();
        registerReceiver(scanSdReceiver, intentFilter);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                Uri.parse("file://"
                        + Environment.getExternalStorageDirectory()
                                .getAbsolutePath())));
    }

    public void ShowMessage(String mess)
    {
        Toast.makeText(this, mess, Toast.LENGTH_SHORT).show();
    }

    public void ShowMessage(int messId)
    {
        Toast.makeText(this, getString(messId), Toast.LENGTH_SHORT).show();
    }

    private final class MusicSeekBarListener implements OnSeekBarChangeListener
    {
        // 移动触发
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser)
        {

        }

        // 起始触发
        public void onStartTrackingTouch(SeekBar seekBar)
        {
            isStartTrackingTouch = true;
        }

        // 结束触发
        public void onStopTrackingTouch(SeekBar seekBar)
        {
            PlayMusicService.player.seekTo(seekBar.getProgress());
            isStartTrackingTouch = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = mAudioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC);

        switch (keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                if (!UserHelper.getIsExit())
                {
                    UserHelper.setIsExit(true);
                    ShowMessage(R.string.alert_exit);
                    if (tExit != null)
                    {
                        if (exitTimerTask != null)
                        {
                            // 将原任务从队列中移除(必须的，否则报错)
                            exitTimerTask.cancel();
                        }
                        // 新建一个任务
                        exitTimerTask = new ExitTimerTask();
                        tExit.schedule(exitTimerTask, 2 * 1000);
                    }
                }
                else
                {
                    UserHelper.setIsExit(false);
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (currentVolume > 0)
                {
                    currentVolume = currentVolume - 1;
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            currentVolume, 0);
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (currentVolume < maxVolume)
                {
                    currentVolume = currentVolume + 1;
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            currentVolume, 0);
                }
                break;
        }
        return true;
    }

    public class ScanSdReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action))
            {
                // // 当系统开始扫描sd卡时，为了用户体验，可以加上一个等待框
                // new Thread(new myThread(1)).start();
            }
            if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action))
            {
                // 当系统扫描完毕时，重新查询ContentProvider，并且邦定数据
                new Thread(new myThread(2)).start();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    final Handler MyHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            Utils.closeWaitingDialog();
            switch (msg.what)
            {
                case 1:
                {
                    BindListView();
                    if (isStop)
                    {
                        play();
                    }
                    else if (!isPause)
                    {
                        pause();
                    }
                    else
                    {
                        resume();
                    }
                    break;
                }
                case 2:
                {
                    RefreshData();
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };

    public class myThread implements Runnable
    {
        int type = 0;

        myThread(int type)
        {
            this.type = type;
        }

        @Override
        public void run()
        {
            switch (type)
            {
                case 1:
                {
                    GetMusicList();
                    break;
                }
                case 2:
                {
                    GetMusicList();
                    break;
                }
            }
            MyHandler.sendMessage(MyHandler.obtainMessage(this.type));
        }
    }

    public static void SetCurrentPos()
    {
        int min = 0;
        int max = musicList.size();
        current = (int) Math.round(Math.random() * (max - min) + min);
        Log.i("sbox", "" + (int) Math.round(Math.random() * (max - min) + min));
    }

    @Override
    protected void onPause()
    {
        Log.i("SBOX", "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart()
    {
        Log.i("SBOX", "onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        resume();
    }

    @Override
    protected void onDestroy()
    {
        Log.i("SBOX", "on DESTORY");
        unregisterReceiver(headsetPlugReceiver);
        unregisterReceiver(phoneReceiver);
        // if (serviceIntent != null)
        // stopService(serviceIntent);
        super.onDestroy();
    }

}
