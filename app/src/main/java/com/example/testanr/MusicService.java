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
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MusicService extends Service {
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
    private static final  int NOTIFY_1 = 1 ;
    public static RemoteViews contentView;
    private MusicBinder mBinder = null;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return  mBinder;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Service启动onCreate()");
        super.onCreate();
        mMusicList = MainActivity.mMusicList; //传入音乐列表
        mBinder = new MusicBinder();

        //创建MediaPlayer
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //todo 循环模式 随机，顺序，单次
                    Log.d(TAG, "onCompletion 播完了");
                    playNext();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mediaPlayer.reset();
                    playPrevious();
                    return false;
                }
            });
        }

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.testanr.play");
        intentFilter.addAction("com.example.testanr.next");
        intentFilter.addAction("com.example.testanr.previous");
        intentFilter.addAction("com.example.testanr.cancel");
        registerReceiver(myBroadcastReceiver, intentFilter);

        notificationManager.notify(1, updateNotificationBar(true));
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

    void stopMusic() {
        //取消服务
        Intent intentStopService = new Intent(this, MusicService.class);
        stopService(intentStopService);
    }

    void pauseMusic() {
        Log.d(TAG, "进入playMusicService::onStartCommand::MainActivity.PAUSE_MUSIC 暂停音乐");
        mediaPlayer.pause();
        lastItem = MainActivity.currentMusicId;
        notifyMusicChange("stop");
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onDestroy() {
//        savePlayData();
        Log.d(TAG, "Service onDestroy()打开");
//        MySaveDataTask mySaveDataTask= new MySaveDataTask(getApplicationContext(), mMusicList, currentItem, mediaPlayer);
//        Log.d(TAG, "Service mySaveDataTask 新建完成");
//        mySaveDataTask.execute();
        unregisterReceiver(myBroadcastReceiver);
//        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        Log.d(TAG, "Service onDestroy()完毕");
        super.onDestroy();

    }

    void playPrevious() {
        if (MainActivity.currentMusicId == 0) {
            MainActivity.currentMusicId = MainActivity.mMusicListLength - 1;
            Toast.makeText(getApplicationContext(), "已经到播放列表顶端啦！为你跳转到播放列表末端...", Toast.LENGTH_SHORT).show();
        } else {
            MainActivity.currentMusicId--;
        }
        setMediaPlayerAndStart();
        notifyMusicChange("pre");
    }

    void playNext() {
        if (MainActivity.currentMusicId == MainActivity.mMusicListLength - 1) {
            MainActivity.currentMusicId = 0;
            Toast.makeText(getApplicationContext(), "已经到播放列表末端啦！为你跳转到播放列表顶端...", Toast.LENGTH_SHORT).show();
        } else {
            MainActivity.currentMusicId++;
        }
        setMediaPlayerAndStart();
        notifyMusicChange("next");
    }

    void playMusic() {
        notifyMusicChange("start");
        //                Log.d(TAG, "lastItem: "+lastItem+", currentItem: "+currentItem);
        Log.d(TAG, "进入playMusicService::onStartCommand::MainActivity.PLAY_MUSIC 播放音乐");
//        Log.d(TAG, "playMusic:: 启动");
//        Log.d(TAG, "lastItem: " + lastItem + "  currentItem " + currentItem);
//        Log.d(TAG, "playMusic:: MainActivity.isPaused:"+MainActivity.isPaused);
//        Log.d(TAG, "playMusic:: lastItem:"+lastItem);
//        Log.d(TAG, "playMusic:: currentItem:"+currentItem);
        if ((!MainActivity.isPaused) || lastItem != MainActivity.currentMusicId) {
            Log.d(TAG, "playMusic:: 启动，进入if");
            Log.d(TAG, "playMusic:: MainActivity.isPaused:"+MainActivity.isPaused);
            setMediaPlayerAndStart();
            MainActivity.isPaused = false;
        } else if ((MainActivity.isPaused) && (mediaPlayer != null) && (lastItem == MainActivity.currentMusicId)) { //暂停后重新打开播放
            Log.d(TAG, "playMusic:: 启动，进入else if");
            mediaPlayer.start();
        }
    }

    private void setMediaPlayerAndStart() {
        Log.d(TAG, "进入到setMediaPlayerAndStart()");
        mediaPlayer.reset();

        try {
            Log.d(TAG, "setDataSource, 现在播放： "+ mMusicList.get(MainActivity.currentMusicId).name);
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
                if (MainActivity.isFirstTime){
                    Log.d("yangzhang", "Service MainActivity.isFirstTime: " + MainActivity.isFirstTime);
                    Log.d("yangzhang", "Service MainActivity.lastMusicItem.currentPosition: "+MainActivity.lastMusicItem.currentPosition);
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

    @TargetApi(Build.VERSION_CODES.N)
    public Notification updateNotificationBar(boolean firstTime) {
        if (notification == null){
            //如果为空，初始化通知
            String id = "my_channel_01";
            String name="我是渠道名字";
            notificationBuilder = new Notification.Builder(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);//IMPORTANCE_LOW 开启通知，不会弹出，不发出提示音，状态栏中显示
                mChannel.setShowBadge(true);
//            Toast.makeText(this, mChannel.toString(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "mChannel.toString(): "+mChannel.toString());
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
//                .setVisibility(Notification.VISIBILITY_PUBLIC)
//                .setWhen(System.currentTimeMillis())
//                .setTicker("收到音乐播放器的消息")
                    .setDefaults(Notification.DEFAULT_SOUND)
//                .setAutoCancel(true)
                    .setContentIntent(pIntentClick);
        }

        //更新音乐数据
        if(firstTime){
            contentView.setTextViewText(R.id.notification_title, MainActivity.lastMusicItem.name);
            if (MainActivity.lastMusicItem.hasAlbumArt){
                contentView.setImageViewBitmap(R.id.notification_album, MainActivity.lastMusicItem.thumb);
            }
            else  {
                /*默认封面*/
                contentView.setImageViewResource(R.id.notification_album, R.mipmap.default_cover);
            }
            //MainActivity.isFirstTime = false;

        } else  {
            contentView.setTextViewText(R.id.notification_title, mMusicList.get(MainActivity.currentMusicId).name);
            if (mMusicList.get(MainActivity.currentMusicId).hasAlbumArt){
                contentView.setImageViewBitmap(R.id.notification_album, mMusicList.get(MainActivity.currentMusicId).thumb);
            }
            else  {
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
//        Log.d(TAG, "notification on");//从0开始
    }



    private void notifyMusicChange(String action) {
        Intent intent = new Intent("ACTION_MUSIC_CHANGE");
        intent.putExtra("action", action);
        sendBroadcast(intent);
        notificationManager.notify(1, updateNotificationBar(false));
        startForeground(1, updateNotificationBar(false));
    }


    //    private void savePlayData(List<MusicItem> mMusicList, int currentItem, MediaPlayer mediaPlayer) {
    public static void savePlayData(Context context, MusicItem musicItem, int currentItem, MediaPlayer mediaPlayer) {
//        Log.d(TAG, "savePlayData() 开始");
//        MusicItem musicItem = mMusicList.get(currentItem);
        SharedPreferences sharedPreferences = context.getSharedPreferences("playData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("songUri", musicItem.songUri.toString());//音乐uri, 传给mediaPlayer可以直接播放
        editor.putInt("currentPosition", mediaPlayer.getCurrentPosition());//播放音乐的位置
        Log.d(TAG, "savePlayData::currentPosition: "+mediaPlayer.getCurrentPosition());
        editor.putString("musicName", musicItem.name);//音乐名, TextView直接展示
        Log.d(TAG, "savePlayData::musicName: "+musicItem.name);
        editor.putBoolean("hasAlbumArt", musicItem.hasAlbumArt);
        editor.putInt("currentItem", currentItem);
        if (musicItem.hasAlbumArt){
            editor.putString("albumUri", musicItem.albumUri.toString());//音乐专辑Uri
        }
        editor.commit();
        Log.d(TAG, "savePlayData() 完毕");
    }

    public static class MySaveDataTask extends AsyncTask<Object, Void, Void> {
        MusicItem musicItem;
        int currentItem;
        MediaPlayer mediaPlayer;
        Context context;

        public MySaveDataTask(Context context, List<MusicItem> mMusicList, int currentItem, MediaPlayer mediaPlayer){
//            Log.d("yangzhang", "MySaveDataTask构造启动");
            this.musicItem = mMusicList.get(currentItem);
            this.currentItem = currentItem;
            this.mediaPlayer = mediaPlayer;
            this.context = context;
//            Log.d("yangzhang", "MySaveDataTask构造完成");
        }

        @Override
        protected Void doInBackground(Object... objects) {
            savePlayData(context, musicItem, currentItem, mediaPlayer);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
//            Log.d("yangzhang", "SaveDataTask异步任务完成");
        }
    }


    /*这个方法获取音乐列表*/
//    private void musicList() {
//        try {
//            File sdCard = Environment.getExternalStorageDirectory(); //获取外部存储路径
//            File directory = new File(sdCard.getAbsolutePath() + "/Music/");
//            File[] myFileList = directory.listFiles();
//            if (myFileList.length > 0) {
//                for (int i = 0; i < myFileList.length; i++) {
//                    File file = myFileList[i];
//                    musicList.add(directory + "/" + file.getName().toString());
//                    Log.d(TAG, "获取音乐文件: " + musicList.get(i));
//                }
//            }

//    }


//    public class getMusicInfomation {
//
//        private String tilte;//歌名
//        private String artist;//歌手
//        private Bitmap bitmap;//专辑海报
//        private long duration;//时长
//        private String path_music;//当前播放的歌曲路径
//
//        ContentResolver musicResolver = getContentResolver();
//        Cursor cursor = null;
//
//        cursor =musicResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Audio.Media.DATA +"= ? ", null,null);
//
//
//    if(cursor !=null&&cursor.getCount()>0)
//
//        {
//            cursor.moveToFirst();
//
//            long ID = cursor.getLong(cursor
//                    .getColumnIndex(MediaStore.Audio.Media._ID));    //音乐id
//            tilte = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
//            artist = cursor.getString(cursor
//                    .getColumnIndex(MediaStore.Audio.Media.ARTIST)); // 艺术家
//            String album = cursor.getString(cursor
//                    .getColumnIndex(MediaStore.Audio.Media.ALBUM));    //专辑
//            duration = cursor.getLong(cursor
//                    .getColumnIndex(MediaStore.Audio.Media.DURATION)); // 时长
//            long size = cursor.getLong(cursor
//                    .getColumnIndex(MediaStore.Audio.Media.SIZE));
//            String url = cursor.getString(cursor
//                    .getColumnIndex(MediaStore.Audio.Media.DATA));
//
//            long albumId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
//
//            bitmap = getMusicBitemp(getApplicationContext(), ID, albumId);
//
//        }
//
//        public Bitmap getMusicBitemp(){
//            private static final Uri sArtworkUri = Uri
//                    .parse("content://media/external/audio/albumart");
//
//            public static Bitmap getMusicBitemp(Context context, long songid, long albumid) {
//                Bitmap bm = null;
//// 专辑id和歌曲id小于0说明没有专辑、歌曲，并抛出异常
//                if (albumid < 0 && songid < 0) {
//                    throw new IllegalArgumentException(
//                            "Must specify an album or a song id");
//                }
//                try {
//                    if (albumid < 0) {
//                        Uri uri = Uri.parse("content://media/external/audio/media/"
//                                + songid + "/albumart");
//                        ParcelFileDescriptor pfd = context.getContentResolver()
//                                .openFileDescriptor(uri, "r");
//                        if (pfd != null) {
//                            FileDescriptor fd = pfd.getFileDescriptor();
//                            bm = BitmapFactory.decodeFileDescriptor(fd);
//                        }
//                    } else {
//                        Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
//                        ParcelFileDescriptor pfd = context.getContentResolver()
//                                .openFileDescriptor(uri, "r");
//                        if (pfd != null) {
//                            FileDescriptor fd = pfd.getFileDescriptor();
//                            bm = BitmapFactory.decodeFileDescriptor(fd);
//
//
//                        } else {
//                            return null;
//                        }
//                    }
//                } catch (FileNotFoundException ex) {
//                }
////如果获取的bitmap为空，则返回一个默认的bitmap
//                if (bm == null) {
//                    Resources resources = context.getResources();
//                    Drawable drawable = resources.getDrawable(R.drawable.back_iv);
//                    //Drawable 转 Bitmap
//                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
//                    bm = bitmapDrawable.getBitmap();
//                }
//
//                return Bitmap.createScaledBitmap(bm, 150, 150, true);
//            }
//        }
//    }
}

/*下面都是没有使用的类class*/
//    class MusicFilter implements FilenameFilter {
//        @Override
//        public boolean accept(File dir, String filename) {
//            return filename.endsWith(".mp3");
//        }
//    }


//
//    public class getMusicListThread implements Runnable{
//        @Override
//        public void run() {
//            musicList();
//            Log.d(TAG, "子线程里面： musicList() 结束");
//        }
//    }
//}
