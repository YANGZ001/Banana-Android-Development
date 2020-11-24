package com.example.testanr;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private ImageButton imgbtn_notification_play;
    private ImageButton imgbtn_notification_playprev;
    private ImageButton imgbtn_notification_playnext;
    private ImageButton imgbtn_notification_cancel;
    private ImageView notification_album;
    private TextView notification_cancel;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        Log.d("yangzhang", "接收到广播啦");
        String action = intent.getAction();//获取action标记，用户区分点击事件

        MediaPlayer mediaPlayer = PlayMusicService.mediaPlayer;//获取全局播放控制对象，该对象已在Activity中初始化
        if (mediaPlayer != null) {
            switch (action){
                case "com.example.testanr.play":
                    Log.d("yangzhang", "广播类型： notification press play button");
                    switch (MainActivity.imgButtonFlag){
                        case MainActivity.FLAG_OFF:
                            Log.d(MainActivity.TAG, "广播 imgButtonFlag: FLAG_OFF"+ MainActivity.imgButtonFlag);
                            MainActivity.imgButtonFlag = MainActivity.FLAG_ON;
                            initiatePlayMusicService(context, MainActivity.PLAY_MUSIC);
                            break;

                        case MainActivity.FLAG_ON:
                            Log.d(MainActivity.TAG, "广播 imgButtonFlag: FLAG_ON "+ MainActivity.imgButtonFlag);
                            MainActivity.imgButtonFlag = MainActivity.FLAG_OFF;
                            MainActivity.isPaused = true;
                            initiatePlayMusicService(context, MainActivity.PAUSE_MUSIC);
                            break;
                    }
                    break;

                case "com.example.testanr.next":
                    Log.d("yangzhang", "notification press next button");
                    MainActivity.imgButtonFlag = 1;
                    MainActivity.isPaused = false;
                    initiatePlayMusicService(context, MainActivity.PLAY_NEXT);
                    break;

                case "com.example.testanr.previous":
                    Log.d("yangzhang", "notification press previous button");
                    MainActivity.imgButtonFlag = 1;
                    MainActivity.isPaused = false;
                    initiatePlayMusicService(context, MainActivity.PLAY_PREV);
                    break;

                case "com.example.testanr.cancel":
                    Log.d("yangzhang", "notification press cancel button");

                    //新建一个异步线程保存数据
                    PlayMusicService.MySaveDataTask mySaveDataTask= new PlayMusicService.MySaveDataTask(context, MainActivity.mMusicList, MainActivity.currentMusicId, PlayMusicService.mediaPlayer, MainActivity.MODE);
                    Log.d("yangzhang", "Service mySaveDataTask 新建完成");
                    mySaveDataTask.execute();
                    MainActivity.imgButtonFlag = 0;
                    MainActivity.isPaused = true;
                    initiatePlayMusicService(context, MainActivity.STOP);
                    break;

                default:
                    Log.d("yangzhang", "notification 点击了其他");
                    break;
            }

        }
    }

    public void initiatePlayMusicService(Context context, int TYPE) {
        Log.d("yangzhang", "广播:: initiatePlayMusicService");
        Intent intent = new Intent(context, PlayMusicService.class);
        intent.putExtra("TYPE", TYPE);
        context.startService(intent);
        //状态栏参数不在这里更新
    }

}
