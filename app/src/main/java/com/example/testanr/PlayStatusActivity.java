package com.example.testanr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class PlayStatusActivity extends Activity implements View.OnClickListener {
    private ImageButton imgbtn_playMusic;
    private ImageButton imgbtn_playPrevious;
    private ImageButton imgbtn_playNext;
    private ImageButton imgbtn_back;
    private ImageButton imgbtn_playmodes;
    private SeekBar seekBar;
    private SeekBar volumeSeekBar;
//    public static int imgButtonFlag;
//    public static boolean isPaused;
//    public static int currentMusicId;
    private MediaPlayer mMediaPlayer;
    private Timer mTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_status);
        Log.d("yangzhang", "PlaySatusActivity is on!");


        imgbtn_playMusic = (ImageButton) findViewById(R.id.buttonPlayMusic);
        imgbtn_playPrevious = (ImageButton) findViewById(R.id.buttonPlayPrevious);
        imgbtn_playNext = (ImageButton) findViewById(R.id.buttonPlayNext);
        imgbtn_back = (ImageButton) findViewById(R.id.destoryPlayStatusActivity);
        imgbtn_playmodes =  (ImageButton) findViewById(R.id.buttonPlayModes);
        setPlayModeImage();
        seekBar = (SeekBar) findViewById(R.id.music_seekbar);
        volumeSeekBar = (SeekBar) findViewById(R.id.volume_seekbar);

        if (MainActivity.audioManager != null){
            Log.d("yangzhang", "MainActivity.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)="+MainActivity.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeSeekBar.setMax(MainActivity.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeSeekBar.setProgress(MainActivity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }

        imgbtn_playMusic.setOnClickListener(this);
        imgbtn_playPrevious.setOnClickListener(this);
        imgbtn_playNext.setOnClickListener(this);
        imgbtn_back.setOnClickListener(this);
        imgbtn_playmodes.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(new seekBarChangeListener());
        volumeSeekBar.setOnSeekBarChangeListener(new volumeSeekBarChangeListener());


//        imgButtonFlag = MainActivity.imgButtonFlag;
//        isPaused = MainActivity.isPaused;
//        currentMusicId = MainActivity.currentMusicId;
        mMediaPlayer = PlayMusicService.mediaPlayer;

        IntentFilter filter = new IntentFilter("ACTION_MUSIC_CHANGE");
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void setPlayModeImage() {
        switch (MainActivity.MODE) {
            case MainActivity.MODE_CYCLE:
                imgbtn_playmodes.setImageResource(R.drawable.playcycle);
                break;

            case MainActivity.MODE_ONCE:
                imgbtn_playmodes.setImageResource(R.drawable.playonce);
                break;

            case MainActivity.MODE_RANDOM:
                imgbtn_playmodes.setImageResource(R.drawable.playrandom);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPlayStatusLayout();
        //启动timer
        mTimer = new Timer();
        mTimer.schedule(timerTask, 0, 50);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimer.cancel();
    }

    private class  seekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        int onTouchFlag = 0;
        int TOUCH = 1;
        int NOTOUCH = 0;

        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (onTouchFlag == TOUCH && mMediaPlayer != null) {
                Log.d("yangzhang", "SeekBar 进度: " + progress+"getProgress();: "+ seekBar.getProgress());
                mMediaPlayer.seekTo(progress, MediaPlayer.SEEK_CLOSEST);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d("yangzhang", "按下SeekBar");
            onTouchFlag = TOUCH;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d("yangzhang", "放开SeekBar");
            onTouchFlag = NOTOUCH;
        }
    }

    private class  volumeSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        int onTouchFlag = 0;
        int TOUCH = 1;
        int NOTOUCH = 0;

        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (MainActivity.audioManager == null )
            {
                MainActivity.audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        }
            if (onTouchFlag == TOUCH ) {
                Log.d("yangzhang", "Volume SeekBar 进度: " + progress + "getProgress();: "+ seekBar.getProgress());
                MainActivity.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d("yangzhang", "按下SeekBar");
            onTouchFlag = TOUCH;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d("yangzhang", "放开SeekBar");
            onTouchFlag = NOTOUCH;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonPlayMusic:
//                Log.d("yangzhang", "buttonPlayMusic imgButtonFlag:  " + imgButtonFlag);
                switch (MainActivity.imgButtonFlag) {
                    case 0:
//                        Log.d("yangzhang", "buttonPlayMusic imgButtonFlag:" + imgButtonFlag);
                        imgbtn_playMusic.setActivated(true);
                        MainActivity.imgButtonFlag = 1;
                        initiatePlayMusicService(MainActivity.PLAY_MUSIC);
                        break;

                    case 1:
//                        Log.d("yangzhang", "buttonPlayMusic imgButtonFlag:" + imgButtonFlag);
                        imgbtn_playMusic.setActivated(false);
                        MainActivity.imgButtonFlag = 0;
                        MainActivity.isPaused = true;
                        initiatePlayMusicService(MainActivity.PAUSE_MUSIC);
                        break;
                }
                break;

            case R.id.buttonPlayNext:
                imgbtn_playMusic.setActivated(true);
                MainActivity.imgButtonFlag = 1;
                MainActivity.isPaused = false;
                initiatePlayMusicService(MainActivity.PLAY_NEXT);
                break;

            case R.id.buttonPlayPrevious:
                imgbtn_playMusic.setActivated(true);
                MainActivity.imgButtonFlag = 1;
                MainActivity.isPaused = false;
                initiatePlayMusicService(MainActivity.PLAY_PREV);
                break;

            case R.id.destoryPlayStatusActivity:
                onBackPressed();
                break;

            case R.id.buttonPlayModes:
                changePlayMode();
                break;
        }
    }

    private void changePlayMode() {
        switch (MainActivity.MODE) {
            case MainActivity.MODE_CYCLE:
                //变成ONCE
                imgbtn_playmodes.setImageResource(R.drawable.playonce);
                MainActivity.MODE = MainActivity.MODE_ONCE;
                break;

            case MainActivity.MODE_ONCE:
                //变成RANDOM
                imgbtn_playmodes.setImageResource(R.drawable.playrandom);
                MainActivity.MODE = MainActivity.MODE_RANDOM;
                break;

            case MainActivity.MODE_RANDOM:
                //变成CYCLE
                imgbtn_playmodes.setImageResource(R.drawable.playcycle);
                MainActivity.MODE = MainActivity.MODE_CYCLE;
                break;
        }
        Log.d("yangzhang", "MODE = "+ MainActivity.MODE);

    }

    public void initiatePlayMusicService(int TYPE) {
        Log.d("yangzhang", "PlayStatusActivity:: initiatePlayMusicService");
        //启动Service之前先更新参数
        Intent intent = new Intent(this, PlayMusicService.class);
        intent.putExtra("TYPE", TYPE);
        startService(intent);
        if ((TYPE != MainActivity.PAUSE_MUSIC) && (!MainActivity.isPaused)){
//            Log.d(TAG, "setBottomPlayStatus启动,  ispaused: "+ ispaused);
            setPlayStatusLayout();
        }
    }

    private void setPlayStatusLayout() {
        //打开activity调用这个方法
        MusicItem musicItem = MainActivity.mMusicList.get(MainActivity.currentMusicId);

        ImageView imageView = (ImageView) findViewById(R.id.music_thumb_big);
        TextView title = (TextView) findViewById(R.id.playMusicStatusTitle);
        TextView artist = (TextView) findViewById(R.id.playMusicStatusArtist);

        title.setText("歌曲名： " + musicItem.name);
        artist.setText("艺术家： " + musicItem.artist);

        if (musicItem.thumb != null ){
            imageView.setImageBitmap(musicItem.thumb);
        }
        else  {
            /*默认封面*/
            imageView.setImageResource(R.mipmap.default_cover);
        }

        switch (MainActivity.imgButtonFlag) {
            case 0:
                imgbtn_playMusic.setActivated(false);
                break;

            case 1:
                imgbtn_playMusic.setActivated(true);
                break;
        }

        //todo 更新seekbar状态
        TextView duration = (TextView) findViewById(R.id.music_duration_end);
//        TextView music_current = (TextView) findViewById(R.id.music_duration_current);
        seekBar.setMax((int) musicItem.duration); //设置进度条
        duration.setText(CustomUtils.convertMSecendToTime(musicItem.duration));

//        seekBar.setProgress(mMediaPlayer.getCurrentPosition());
//        music_current.setText(CustomUtils.convertMSecendToTime(mMediaPlayer.getCurrentPosition()));
//            Log.d("yangzhang", "mMediaPlayer.getDuration()"+ mMediaPlayer.getDuration());//178364
//            Log.d("yangzhang", "mMediaPlayer.getCurrentPosition()"+ mMediaPlayer.getCurrentPosition());

//        if (mMediaPlayer != null){
//            mTimer.schedule(timerTask, 0, 50);
//        }

    }

    TimerTask timerTask = new TimerTask() {

        @Override
        public void run() {
//            Log.d("yangzhang", "timerTask 运行");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayer != null){
//                        Log.d("yangzhang", "更新seekBar 播放进度状态");//会
                        seekBar.setProgress(mMediaPlayer.getCurrentPosition());
                        TextView music_current = (TextView) findViewById(R.id.music_duration_current);
                        music_current.setText(CustomUtils.convertMSecendToTime(mMediaPlayer.getCurrentPosition()));
                } else {
                        mMediaPlayer = PlayMusicService.mediaPlayer;
                    }

                    if (MainActivity.audioManager != null){
                        volumeSeekBar.setProgress(MainActivity.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                    }
                }
            });
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPlayStatusLayout();
//            setBottomPlayStatus(mMusicList.get(currentMusicId));
            String action = intent.getStringExtra("action");
            Log.d("zhangyang", "onReceive:action = " + action);


            if (action.equals("next")) {
//                setBottomPlayStatus(mMusicList.get(currentMusicId));
            } else if (action.equals("pre")) {

            } else if (action.equals("start")) {
                Log.d("zhangyang", "onReceive:start = ");
            } else if (action.equals("stop")) {

            }
        }
    };



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
//        Log.d("yangzhang", "在super.onDestory后面还会运行吗？");//会
    }

}