package com.example.testanr;

//import androidx.annotation.VisibleForTesting;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements View.OnClickListener {

     /* 规定开始音乐，暂停，结束音乐的标志*/
    public static final String  TAG = "yangzhang";
    public static final int PLAY_MUSIC = 0;
    public static final int PAUSE_MUSIC = 1;
    public static final int STOP = 2;
    public static final int PLAY_NEXT = 3;
    public static final int PLAY_PREV = 4;
    private ImageButton imgbtn_playMusic;
    private ImageButton imgbtn_playPrevious;
    private ImageButton imgbtn_playNext;
    public static boolean isPaused = false;
    private MusicUpdateTask mMusicUpdateTask;
    private LinearLayout linearLayout;
    public static List<MusicItem> mMusicList;
    private ListView mMusicListView;
    public static int currentMusicId;
    public static int mMusicListLength = 0;
    public static MusicItem lastMusicItem;
    public static final int FLAG_ON = 1;
    public static final int FLAG_OFF = 0;
    public static int imgButtonFlag = FLAG_OFF;//默认为off
    public  static boolean isFirstTime = false;
    public static final String MODE_CYCLE = "CYCLE";
    public static final String MODE_ONCE = "ONCE";
    public static final String MODE_RANDOM = "RANDOM";
    public static String MODE;
    public static AudioManager audioManager;
    public boolean hasAlerted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "OnCreate方法启动");

        imgbtn_playMusic = (ImageButton) findViewById(R.id.buttonPlayMusic);
        imgbtn_playPrevious = (ImageButton) findViewById(R.id.buttonPlayPrevious);
        imgbtn_playNext = (ImageButton) findViewById(R.id.buttonPlayNext);
        linearLayout = (LinearLayout) findViewById(R.id.buttomPlayStatus);

        linearLayout.setOnClickListener(this);
        imgbtn_playMusic.setOnClickListener(this);
        imgbtn_playPrevious.setOnClickListener(this);
        imgbtn_playNext.setOnClickListener(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        //检查权限
        if (isStoragePermissionGranted()) {
//            Log.d(TAG, "StoragePermissionGranted");
        }

        //mMusicList view 展示播放列表
        mMusicList = new ArrayList<MusicItem>();
        mMusicListView = (ListView) findViewById(R.id.music_list); //一个简单的listView
        mMusicListView.setOnItemClickListener(mOnMusicItemClickListener);
        MusicItemAdapter adapter = new MusicItemAdapter(this, R.layout.music_item, mMusicList);
        mMusicListView.setAdapter(adapter);

        //Async线程异步获取音乐列表
        mMusicUpdateTask = new MusicUpdateTask();
        mMusicUpdateTask.execute();

        //启动后读取上次的数据, 并设置底部button
        lastMusicItem = loadPlayData();
        if (this.MODE == null){
            this.MODE = MODE_CYCLE;
        }
        setBottomPlayStatus(lastMusicItem);
        currentMusicId = lastMusicItem.currentItem;
        isFirstTime = true;

        //启动service挂在后台
//        Intent intent = new Intent(this, PlayMusicService.class);
//        startService(intent);
        bindToService();

        IntentFilter filter = new IntentFilter("ACTION_MUSIC_CHANGE");
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(mBroadcastReceiver, filter);
    }

    MusicService musicService = null;
    MusicService.MusicBinder mBinder = null;

    private ServiceConnection conn = new ServiceConnection() {
        // 绑定成功后该方法回调，并获得服务端IBinder的引用
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected:musicService");
            mBinder = (MusicService.MusicBinder)service;
            // 通过获得的IBinder获取PlayMusicService的引用
            musicService = mBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected:musicService");
        }

    };

    private void bindToService() {
        bindService(new Intent(MainActivity.this,
                       com.example.testanr.MusicService.class), conn,
                Service.BIND_AUTO_CREATE);
        Log.d(TAG, "bindToService()");
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setBottomPlayStatus(mMusicList.get(currentMusicId));
//            String action = intent.getStringExtra("action");
            String action = intent.getAction();
            Log.d("yangzhang", "onReceive:action = " + action);

            if (action == "android.media.VOLUME_CHANGED_ACTION"){
                if (audioManager == null){
                    audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                }
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//                Log.d("yangzhang", "MainActivity: onReceive:" +currentVolume);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//                Log.d("yangzhang", "MainActivity: onReceive:threshold = " +threshold);

                //音量超过60%提醒用户
                if (currentVolume > 0.618*maxVolume){
                    showAlterDialog();
                    hasAlerted = true;
                }
            }
        }
    };

    private void showAlterDialog() {
        final AlertDialog.Builder alterDiaglogBuilder = new AlertDialog.Builder(MainActivity.this);
        alterDiaglogBuilder.setIcon(R.drawable.volumealert);//图标
        alterDiaglogBuilder.setTitle("音量超标啦");//文字
        alterDiaglogBuilder.setMessage("后面的朋友能听见我吗？");//提示消息

        alterDiaglogBuilder.show();
    }

//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    public static boolean isNotificationEnabled(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            //8.0手机以上
//            if (((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).getImportance() == NotificationManager.IMPORTANCE_NONE) {
//                return false;
//            }
//        }
//
//        String CHECK_OP_NO_THROW = "checkOpNoThrow";
//        String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
//
//        AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
//        ApplicationInfo appInfo = context.getApplicationInfo();
//        String pkg = context.getApplicationContext().getPackageName();
//        int uid = appInfo.uid;
//
//        Class appOpsClass = null;
//
//        try {
//            appOpsClass = Class.forName(AppOpsManager.class.getName());
//            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE,
//                    String.class);
//            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
//
//            int value = (Integer) opPostNotificationValue.get(Integer.class);
//            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    private AdapterView.OnItemClickListener mOnMusicItemClickListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //添加播放音乐的代码
            Log.d(TAG, "position: "+position + " id: "+id);//从0开始
            currentMusicId = position;
            imgbtn_playMusic.setActivated(true);
            imgButtonFlag = FLAG_ON;
            initiatePlayMusicService(PLAY_MUSIC);
        }
    };

    public void onClick(View view) { //使用activity类继承OnClickListener
        switch (view.getId()) {
            //开始音乐
            case R.id.buttonPlayMusic:
                switch (imgButtonFlag){
                    case FLAG_OFF:
                        Log.d(TAG, "buttonPlayMusic imgButtonFlag:"+ imgButtonFlag);
                        imgbtn_playMusic.setActivated(true);
                        imgButtonFlag = FLAG_ON;
                        initiatePlayMusicService(PLAY_MUSIC);
                        break;

                    case FLAG_ON:
                        Log.d(TAG, "buttonPlayMusic imgButtonFlag:"+ imgButtonFlag);
                        imgbtn_playMusic.setActivated(false);
                        imgButtonFlag = FLAG_OFF;
                        isPaused = true;
                        initiatePlayMusicService(PAUSE_MUSIC);
                        break;
                }
                break;

            case R.id.buttonPlayNext:
                imgbtn_playMusic.setActivated(true);
                imgButtonFlag = FLAG_ON;
                isPaused = false;
                initiatePlayMusicService(PLAY_NEXT);
                break;

            case R.id.buttonPlayPrevious:
                imgbtn_playMusic.setActivated(true);
                imgButtonFlag = FLAG_ON;
                isPaused = false;
                initiatePlayMusicService(PLAY_PREV);
                break;

            case R.id.buttomPlayStatus:
                Intent intent = new Intent(this, PlayStatusActivity.class);
                startActivity(intent);
                break;

        }
    }

    private void setBottomPlayStatus(MusicItem musicItem) {
        ImageView imageView = (ImageView) findViewById(R.id.music_thumb_bottom);
        TextView textView = (TextView) findViewById(R.id.music_title_bottom);

        textView.setText(musicItem.name);
        textView.setMarqueeRepeatLimit(Integer.MAX_VALUE);
        textView.setFocusable(true);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setFocusableInTouchMode(true);
        textView.setHorizontallyScrolling(true);
        textView.requestFocus();

        if (musicItem.hasAlbumArt){
            imageView.setImageBitmap(musicItem.thumb);
        }
        else  {
            /*默认封面*/
            imageView.setImageResource(R.mipmap.default_cover);
        }

        switch (imgButtonFlag) {
            case FLAG_OFF:
                imgbtn_playMusic.setActivated(false);
                break;

            case FLAG_ON:
                imgbtn_playMusic.setActivated(true);
                break;
        }
    }

    public MusicItem loadPlayData() {
        MusicItem musicItem = new MusicItem(null, null, null, 0, null, 0, null);

        File f = new File("/data/data/com.example.testanr/shared_prefs/playData.xml");
        if (f.exists()) {
            SharedPreferences sharedPreferences = getSharedPreferences("playData", MODE_PRIVATE);
            Log.d("yangzhang", "sharedPreferences is null?" + sharedPreferences);
            musicItem.songUri = Uri.parse(sharedPreferences.getString("songUri", null));
            musicItem.currentPosition = sharedPreferences.getInt("currentPosition", 0);
            musicItem.name = sharedPreferences.getString("musicName", null);
            musicItem.hasAlbumArt = sharedPreferences.getBoolean("hasAlbumArt", false);
            musicItem.currentItem = sharedPreferences.getInt("currentItem", 0);
            Log.d(TAG, "MainActivity loadPlayData启动， currentPosition： "+musicItem.currentPosition +"and item: "+musicItem.currentItem);
            if (musicItem.hasAlbumArt){
                musicItem.albumUri = Uri.parse(sharedPreferences.getString("albumUri", null));
            }
            this.MODE = sharedPreferences.getString("MODE", null);
            Log.d("TAG","SharedPreferences com.example.testanr : exist");
        } else {
            Log.d("TAG", "SharedPreferences com.example.testanr : does not exist");
        }
        return musicItem;
    }

    public void initiatePlayMusicService(int TYPE) {
        switch (TYPE) {
            case MainActivity.PLAY_MUSIC:
                musicService.playMusic();
                break;

            case MainActivity.PAUSE_MUSIC:
                musicService.pauseMusic();
                break;

            case MainActivity.PLAY_NEXT:
                Log.d(TAG, "进入playMusicService::onStartCommand():: playNext");
                musicService.playNext();
                break;

            case MainActivity.PLAY_PREV:
                musicService.playPrevious();
                break;

            case MainActivity.STOP:
                musicService.stopMusic();
                break;
        }

        Log.d("yangzhang", "Mainactivity: initiatePlayMusicService");
        Intent intent = new Intent(this, PlayMusicService.class);
        intent.putExtra("TYPE", TYPE);
//        startService(intent);
        if ((TYPE != PAUSE_MUSIC) && (!isPaused)){
//            Log.d(TAG, "setBottomPlayStatus启动,  ispaused: "+ isPaused);
            setBottomPlayStatus(mMusicList.get(currentMusicId));
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "MainActivity onResume启动 ");
        super.onResume();
//        if (mMusicList.size() > 0 || PlayMusicService.mediaPlayer!=null) {
//            setBottomPlayStatus();
//        }
        if (mMusicList.size() > 0) { //这个条件意味着初次启动不会更新状态栏
            setBottomPlayStatus(mMusicList.get(currentMusicId));
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "MainActivity onPause启动 ");
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("duration", 1000);
        Log.d(TAG, "MainActivity onSaveInstanceState 启动 ");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int dur = savedInstanceState.getInt("duration");
        Log.d(TAG, "MainActivity onRestoreInstanceState, dur = " + dur);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity onDestroy启动 ");

        //取消服务
//        Intent intent = new Intent(this, PlayMusicService.class);
//        stopService(intent);
        unbindService(conn);

        /*退出程序同时退出异步同步*/
        if(mMusicUpdateTask != null && mMusicUpdateTask.getStatus() == AsyncTask.Status.RUNNING) {
            mMusicUpdateTask.cancel(true);
        }
        mMusicUpdateTask = null;

        //新建一个异步线程保存数据
        PlayMusicService.MySaveDataTask mySaveDataTask= new PlayMusicService.MySaveDataTask(getApplicationContext(), mMusicList, currentMusicId, PlayMusicService.mediaPlayer, MODE);
        Log.d(TAG, "Service mySaveDataTask 新建完成");
        mySaveDataTask.execute();

        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy()完成");

        //手动回收图片使用的资源
        for (MusicItem item : mMusicList){
            if (item.thumb != null){
                item.thumb.recycle();
                item.thumb = null;
            }
        }
        mMusicList.clear();

        unregisterReceiver(mBroadcastReceiver);
    }


    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            final Context context = getApplicationContext();
            int readPermissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (readPermissionCheck == PackageManager.PERMISSION_GRANTED
                    && writePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                Log.v("juno", "Permission is granted");
                return true;
            } else {
                Log.v("juno", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("juno", "onRequestPermissionsResult requestCode ： " + requestCode
                + " Permission: " + permissions[0] + " was " + grantResults[0]
                + " Permission: " + permissions[1] + " was " + grantResults[1]
        );
    }

    /*创建Async异步获取音乐列表*/
    private class MusicUpdateTask extends AsyncTask<Object, MusicItem, Void> {

        /*Execute()之前执行onPreExecute*/
        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "加载中...", Toast.LENGTH_SHORT).show();
        }

        /*工作线程，处理耗时任务*/
        @Override
        protected Void doInBackground(Object... objects) {
//            Log.d(TAG,"MusicUpdateTask::doInBackground 启动");

            try {
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String[] searchKey = new String[] {
                        MediaStore.Audio.Media._ID,//在数据库检索的ID
                        MediaStore.Audio.Media.TITLE,//文件的标题，名
                        MediaStore.Audio.Media.ALBUM_ID,//专辑ID
                        MediaStore.Audio.Media.DATA,//文件存放位置
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST
                };
                String where = MediaStore.Audio.Media.DATA + " like \"%" + "/music" + "%\"";
                String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

                ContentResolver contentResolver = getContentResolver();
                Cursor cursor = contentResolver.query(uri, searchKey, where, null, sortOrder);

                if (cursor!=null){
                    while (cursor.moveToNext() && !isCancelled()){
                        //path
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                        //id
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                        //Music URI = path + id
                        Uri musicUri = Uri.withAppendedPath(uri, id);
                        //name
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                        String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                        //duration
                        Long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                        int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                        Uri albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId);

                        Bitmap thumbnail = CustomUtils.createAlbumArt(path); //这里传入的filePath是音乐文件的路径
                        MusicItem musicItem = new MusicItem(musicUri, albumUri, name, artist, duration, thumbnail);

                        publishProgress(musicItem);
//                        Log.d(TAG,"MusicUpdateTask::doInBackground,  publishProgress(musicItem);");
                    }
                    cursor.close();
//                    Log.d(TAG,"MusicUpdateTask::doInBackground,  cursor.close()");
                }
                else {
                    return null;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(MusicItem... values) {//不定参数的写法。MusicItem 是values的类型。values作为数组传入。所以后面用了values[0]
            MusicItem musicItem = values[0];

            //把扫描到的音乐添加到音乐的展示列表中
            mMusicList.add(musicItem);
            MusicItemAdapter adapter = (MusicItemAdapter) mMusicListView.getAdapter();
            adapter.notifyDataSetChanged();
//            Log.d(TAG,"MusicUpdateTask::onProgressUpdate 启动,  update music item data to the adapter");//在更新数据这里出了问题。
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mMusicListLength = mMusicList.size();
            Toast.makeText(getApplicationContext(), "音乐文件加载完成...找到 "+mMusicListLength+" 首歌曲", Toast.LENGTH_SHORT).show();
        }
    }

    public class MusicItemAdapter extends BaseAdapter{
        private List<MusicItem> mData; //所有的音乐文件都在mData里面
        private final LayoutInflater mInflater;
        private final int mLayoutResource; //布局文件
        private Context mContext;

        public MusicItemAdapter(Context context, int mLayoutResource, List<MusicItem> data){
//            this.mInflater = LayoutInflater.from(context);
            this.mInflater = LayoutInflater.from(MainActivity.this);
            this.mContext = context;
            this.mLayoutResource = mLayoutResource;
            this.mData = data;
        }

        @Override
        public int getCount() {
            return (mData != null)? mData.size() :0; //条件运算符
        }

        @Override
        public Object getItem(int position) {
            return (mData != null)? mData.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = mInflater.inflate(mLayoutResource, parent, false);
            }

            MusicItem item  = mData.get(position);
            ImageView thumb = (ImageView) convertView.findViewById(R.id.music_thumb);
            TextView title = (TextView) convertView.findViewById(R.id.music_title);
            TextView createTime = (TextView) convertView.findViewById(R.id.music_duration);
            TextView artist = (TextView) convertView.findViewById(R.id.music_artist);

            title.setText("歌曲名："+item.name);
            artist.setText("艺术家："+item.artist);
            String times = CustomUtils.convertMSecendToTime(item.duration);
//            Log.d(TAG,"getView::times = " + times);
//            times = String.format(mContext.getString(R.string.duration), times);
            createTime.setText("时长:"+times);

            /*专辑封面*/
            if (thumb!=null){
                if (item.thumb != null ){
                    thumb.setImageBitmap(item.thumb);
                }
                else  {
                    /*默认封面*/
                    thumb.setImageResource(R.mipmap.default_cover);
                }
            }
            return convertView;
        }
    }
}