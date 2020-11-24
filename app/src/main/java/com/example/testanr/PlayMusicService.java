package com.example.testanr;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class PlayMusicService extends Service {
    public static String TAG = "yangzhang";
    public static MediaPlayer mediaPlayer;
    //    private static String PATH = "sdcard/music";
    List<MusicItem> mMusicList;
    private int lastItem = -1;
    public MyBroadcastReceiver myBroadcastReceiver;
    //todo: Notification
    public static NotificationManager notificationManager;
    public static Notification notification;
    public static Notification.Builder notificationBuilder;
    private static final int NOTIFY_1 = 1;
    public static RemoteViews contentView;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Service启动onCreate()");
        super.onCreate();
        mMusicList = MainActivity.mMusicList; //传入音乐列表

        //创建MediaPlayer
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            //默认列表循环播放下一首
//            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    Log.d(TAG, "onCreate() onCompletion");
//                    playNext();
//                }
//            });
        }

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.testanr.play");
        intentFilter.addAction("com.example.testanr.next");
        intentFilter.addAction("com.example.testanr.previous");
        intentFilter.addAction("com.example.testanr.cancel");
        registerReceiver(myBroadcastReceiver, intentFilter);

        //notificationManager.notify(1, updateNotificationBar(true));
        startForeground(1, updateNotificationBar(true));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d(TAG, "Service onStartCommand()方法打开");
        switch (intent.getIntExtra("TYPE", -1)) { //传入TYPE。如果没有传入TYPE，默认为-1
            case MainActivity.PLAY_MUSIC:
                playMusic();
                break;

            case MainActivity.PAUSE_MUSIC:
                pauseMusic();
                break;

            case MainActivity.PLAY_NEXT:
                Log.d(TAG, "进入playMusicService::onStartCommand():: playNext");
                playNext();
                break;

            case MainActivity.PLAY_PREV:
                playPrevious();
                break;

            case MainActivity.STOP:
                stopMusic();
                break;

//            case 10:
//                Log.d(TAG, "主动点击savePlayData() ");
//                savePlayData();
        }
        return START_NOT_STICKY;//被杀后不重启，不保持启动状态，可以随时停止，适合定时数据轮询场景
    }

    private void stopMusic() {
        mediaPlayer.stop();
        notifyMusicChange("stop");
        //取消服务
        Intent intentStopService = new Intent(this, PlayMusicService.class);
        stopService(intentStopService);
    }

    private void pauseMusic() {
        Log.d(TAG, "进入playMusicService::onStartCommand::MainActivity.PAUSE_MUSIC 暂停音乐");
        mediaPlayer.pause();
        lastItem = MainActivity.currentMusicId;
        notifyMusicChange("stop");
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy()打开");
        //saveData的工作在MainActivity中调用
        unregisterReceiver(myBroadcastReceiver);
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        Log.d(TAG, "Service onDestroy()完毕");
        super.onDestroy();
    }

    private void playPrevious() {
        if (MainActivity.currentMusicId == 0) {
            MainActivity.currentMusicId = MainActivity.mMusicListLength - 1;
            Toast.makeText(getApplicationContext(), "已经到播放列表顶端啦！为你跳转到播放列表末端...", Toast.LENGTH_SHORT).show();
        } else {
            MainActivity.currentMusicId--;
        }
        Log.d(TAG, "进入playMusicService::playPrevious() ");
        setMediaPlayerAndStart();
        notifyMusicChange("pre");
    }

    private void playNext() {
        if (MainActivity.currentMusicId == MainActivity.mMusicListLength - 1) {
            MainActivity.currentMusicId = 0;
            Toast.makeText(getApplicationContext(), "已经到播放列表末端啦！为你跳转到播放列表顶端...", Toast.LENGTH_SHORT).show();
        } else {
            MainActivity.currentMusicId++;
        }
        Log.d(TAG, "进入playMusicService::playNext() ");
        setMediaPlayerAndStart();
        notifyMusicChange("next");
    }

    private void playMusic() {
        notifyMusicChange("start");
        Log.d(TAG, "进入playMusicService::onStartCommand::MainActivity.PLAY_MUSIC 播放音乐");
        if ((!MainActivity.isPaused) || lastItem != MainActivity.currentMusicId) {
            Log.d(TAG, "playMusic:: 启动，进入if");
            Log.d(TAG, "playMusic:: MainActivity.isPaused:" + MainActivity.isPaused);
            Log.d(TAG, "进入playMusicService::playMusic() ");
            setMediaPlayerAndStart();
            MainActivity.isPaused = false;
        } else if ((MainActivity.isPaused) && (mediaPlayer != null) && (lastItem == MainActivity.currentMusicId)) { //暂停后重新打开播放
            Log.d(TAG, "playMusic:: 启动，进入else if");
            mediaPlayer.start();
        }
    }

    private void setMediaPlayerAndStart() {
        Log.d(TAG, "setMediaPlayerAndStart()");
        mediaPlayer.reset();
        checkPlayMode();
        try {
            Log.d(TAG, "setDataSource, 现在播放： " + mMusicList.get(MainActivity.currentMusicId).name + " id = " + MainActivity.currentMusicId);
            mediaPlayer.setDataSource(getApplicationContext(), mMusicList.get(MainActivity.currentMusicId).songUri);
//            mediaPlayer.setDataSource(context, songUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        try {
//            mediaPlayer.prepare();
//        } catch (IOException e){
//            e.printStackTrace();
//        }
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @TargetApi(Build.VERSION_CODES.O)
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // 装载完毕回调
//                Log.d(TAG, "prepared完成，回调start()");
                if (MainActivity.isFirstTime) {
                    Log.d("yangzhang", "Service MainActivity.isFirstTime: " + MainActivity.isFirstTime);
                    Log.d("yangzhang", "Service MainActivity.lastMusicItem.currentPosition: " + MainActivity.lastMusicItem.currentPosition);
                    Log.d("yangzhang", "Service 跳转到上一曲的进度");
                    mediaPlayer.seekTo(MainActivity.lastMusicItem.currentPosition, MediaPlayer.SEEK_CLOSEST);
                    MainActivity.isFirstTime = false;
                }
                mediaPlayer.start();
                lastItem = MainActivity.currentMusicId;
//                savePlayData();
                updateNotificationBar(false);
            }
        });
    }

    private void checkPlayMode() {
        Log.d(TAG, "现在的播放模式： PlayStatusActivity.MODE = " + MainActivity.MODE);
        switch (MainActivity.MODE) {
            case MainActivity.MODE_CYCLE:
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d(TAG, "checkPlayMode onCompletion 播完了");
                        playNext();
                    }
                });
                break;

            case MainActivity.MODE_ONCE:
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d(TAG, "checkPlayMode onCompletion 播完了");
                        playMusic();
                    }
                });

                break;

            case MainActivity.MODE_RANDOM:
                Random rand = new Random();
                MainActivity.currentMusicId = rand.nextInt(MainActivity.mMusicListLength);

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        //todo 循环模式 随机，顺序，单次
                        Log.d(TAG, "checkPlayMode onCompletion 播完了");
                        Random rand = new Random();
                        MainActivity.currentMusicId = rand.nextInt(MainActivity.mMusicListLength);
                        setMediaPlayerAndStart();
                    }
                });
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    public Notification updateNotificationBar(boolean firstTime) {
        if (notification == null) {
            //如果为空，初始化通知
            String id = "my_channel_01";
            String name = "我是渠道名字";
            notificationBuilder = new Notification.Builder(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_MIN);
                //IMPORTANCE_LOW 开启通知，不会弹出，不发出提示音，状态栏中显示
                mChannel.setShowBadge(true);
                mChannel.setSound(null, null);
                notificationManager.createNotificationChannel(mChannel);
                notificationBuilder.setChannelId(id);
            }

            //初始化通知
            contentView = new RemoteViews(getPackageName(), R.layout.notification_control);
            //contentView绑定事件
            PendingIntent pIntentPlay = PendingIntent.getBroadcast(this, 0, new Intent("com.example.testanr.play"), 0);//新建意图，并设置action标记为"play"，用于接收广播时过滤意图信息
            contentView.setOnClickPendingIntent(R.id.notification_play, pIntentPlay);//为play控件注册事件

            PendingIntent pIntentNext = PendingIntent.getBroadcast(this, 0, new Intent("com.example.testanr.next"), 0);
            contentView.setOnClickPendingIntent(R.id.notification_playnext, pIntentNext);

            PendingIntent pIntentLast = PendingIntent.getBroadcast(this, 0, new Intent("com.example.testanr.previous"), 0);
            contentView.setOnClickPendingIntent(R.id.notification_playprev, pIntentLast);

            PendingIntent pIntentCancel = PendingIntent.getBroadcast(this, 0, new Intent("com.example.testanr.cancel"), 0);
            contentView.setOnClickPendingIntent(R.id.notification_cancel, pIntentCancel);

            PendingIntent pIntentClick = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
            notificationBuilder.setSmallIcon(R.mipmap.default_cover)
                    .setContentTitle("音乐播放器")
                    .setContentText("正在播放")
                    .setPriority(Notification.PRIORITY_MIN)
                    .setContentIntent(pIntentClick);
        }

        //更新音乐数据
        if (firstTime) {
            contentView.setTextViewText(R.id.notification_title, MainActivity.lastMusicItem.name);
            if (MainActivity.lastMusicItem.hasAlbumArt) {
                contentView.setImageViewBitmap(R.id.notification_album, MainActivity.lastMusicItem.thumb);
            } else {
                /*默认封面*/
                contentView.setImageViewResource(R.id.notification_album, R.mipmap.default_cover);
            }
            //MainActivity.isFirstTime = false;

        } else {
            contentView.setTextViewText(R.id.notification_title, mMusicList.get(MainActivity.currentMusicId).name);
            if (mMusicList.get(MainActivity.currentMusicId).hasAlbumArt) {
                contentView.setImageViewBitmap(R.id.notification_album, mMusicList.get(MainActivity.currentMusicId).thumb);
            } else {
                /*默认封面*/
                contentView.setImageViewResource(R.id.notification_album, R.mipmap.default_cover);
            }
        }

        switch (MainActivity.imgButtonFlag) {
            case MainActivity.FLAG_ON:
                contentView.setImageViewResource(R.id.notification_play, R.drawable.pause);
                break;

            case MainActivity.FLAG_OFF:
                contentView.setImageViewResource(R.id.notification_play, R.drawable.play);
                break;
        }

        notificationBuilder.setCustomContentView(contentView);
        Notification notification = notificationBuilder.build();
        notification.flags = notification.FLAG_NO_CLEAR;//设置通知点击或滑动时不被清除
//        notificationManager.notify(NOTIFY_1, notification);//开启通知
        return notification;
    }

    private void notifyMusicChange(String action) {
        Intent intent = new Intent("ACTION_MUSIC_CHANGE");
        intent.putExtra("action", action);
        sendBroadcast(intent);
        //notificationManager.notify(1, updateNotificationBar(false));
        startForeground(1, updateNotificationBar(false));
    }


    //    private void savePlayData(List<MusicItem> mMusicList, int currentItem, MediaPlayer mediaPlayer) {
    public static void savePlayData(Context context, MusicItem musicItem, int currentItem, MediaPlayer mediaPlayer, String mode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("playData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("songUri", musicItem.songUri.toString());//音乐uri, 传给mediaPlayer可以直接播放
        editor.putString("MODE", mode);//保存上次的播放模式
        editor.putInt("currentPosition", mediaPlayer.getCurrentPosition());//播放音乐的位置
        Log.d(TAG, "savePlayData::currentPosition: " + mediaPlayer.getCurrentPosition());
        editor.putString("musicName", musicItem.name);//音乐名, TextView直接展示
        Log.d(TAG, "savePlayData::musicName: " + musicItem.name);
        editor.putBoolean("hasAlbumArt", musicItem.hasAlbumArt);
        editor.putInt("currentItem", currentItem);
        if (musicItem.hasAlbumArt) {
            editor.putString("albumUri", musicItem.albumUri.toString());//音乐专辑Uri
        }
        editor.commit();
        Log.d(TAG, "savePlayData() 完毕");
    }

    public static class MySaveDataTask extends AsyncTask<Object, Void, Void> {
        private MusicItem musicItem;
        private int currentItem;
        private MediaPlayer mediaPlayer;
        private Context context;
        private String MODE;

        public MySaveDataTask(Context context, List<MusicItem> mMusicList, int currentItem, MediaPlayer mediaPlayer, String mode) {
//            Log.d("yangzhang", "MySaveDataTask构造启动");
            this.musicItem = mMusicList.get(currentItem);
            this.currentItem = currentItem;
            this.mediaPlayer = mediaPlayer;
            this.context = context;
            this.MODE = mode;
//            Log.d("yangzhang", "MySaveDataTask构造完成");
        }

        @Override
        protected Void doInBackground(Object... objects) {
            savePlayData(context, musicItem, currentItem, mediaPlayer, MODE);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
//            Log.d("yangzhang", "SaveDataTask异步任务完成");
        }
    }
}