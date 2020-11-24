package com.example.testanr;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomUtils {

    /*获取封面图片*/
    /*从Uir得到Thumb*/
    public static Bitmap createThumbFromUir(ContentResolver contentResolver, Uri albumUri) {
        InputStream in = null;
        Bitmap bmp = null;
        BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
        Log.d("yangzhang", "CustomUtils::createThumbFromUir 启动");
        try {
            Log.d("yangzhang", "进入try");
            Log.d("yangzhang", "contentResolver = null?" + (contentResolver == null));
            in = contentResolver.openInputStream(albumUri);
            Log.d("yangzhang", "CustomUtils:: in:" +  in);
//            BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
            bmp = BitmapFactory.decodeStream(in, null, sBitmapOptions);
            Log.d("yangzhang", "CustomUtils::createThumbFromUir:: bmp: "+bmp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null){
                    in.close();
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        return bmp;
    }

    static public String convertMSecendToTime(long time) {

        SimpleDateFormat mSDF = new SimpleDateFormat("mm:ss");

        Date date = new Date(time);
        String times= mSDF.format(date);

        return times;
    }

    public static Bitmap createAlbumArt(String filePath){
//        Log.d("yangzhang", "filePath: " + filePath);
        if (filePath == null){
            return null;
        }
        Bitmap bitmap = null;
        MediaMetadataRetriever mMediaMetadataRetriever = new MediaMetadataRetriever();
        try{
            mMediaMetadataRetriever.setDataSource(filePath);
            byte[] albumArt = mMediaMetadataRetriever.getEmbeddedPicture();
//            Log.d("yangzhang", "albumArt: " + albumArt);
            if (albumArt != null){
                bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                mMediaMetadataRetriever.release();
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
        return bitmap;
    }

//    private void initiateNotificationBarTest3() {
//        String id = "my_channel_01";
//        String name="我是渠道名字";
//        Notification notification = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
////            Toast.makeText(this, mChannel.toString(), Toast.LENGTH_SHORT).show();
//            Log.i(TAG, mChannel.toString());
//            notificationManager.createNotificationChannel(mChannel);
//            notification = new Notification.Builder(this)
//                    .setChannelId(id)
//                    .setContentTitle("5 new messages")
//                    .setContentText("hahaha")
//                    .setSmallIcon(R.mipmap.ic_launcher).build();
//        } else {
//            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
//                    .setContentTitle("5 new messages")
//                    .setContentText("hahaha")
//                    .setSmallIcon(R.mipmap.ic_launcher)
//                    .setOngoing(true);
//            notification = notificationBuilder.build();
//        }
//        notificationManager.notify(111123, notification);
//    }
//
//    @TargetApi(Build.VERSION_CODES.O)
//    private void initiateNotificationBarTest2() {
//        Notification noti = new NotificationCompat.Builder(this)
//                .setContentTitle("标题")
//                .setContentText("Hello Content.")
//                .setSmallIcon(R.mipmap.default_cover)
//                .build();
//        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//
//        //高版本需要渠道
////        if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
//        //只在Android O之上需要渠道
//        NotificationChannel notificationChannel = new NotificationChannel("channelid1","channelname",NotificationManager.IMPORTANCE_HIGH);
//        //如果这里用IMPORTANCE_NOENE就需要在系统的设置里面开启渠道，通知才能正常弹出
//        manager.createNotificationChannel(notificationChannel);
//        Log.d(TAG, "NotificationChannel on ");
////        }
//        manager.notify(1, noti);
//        Log.d(TAG, "Notification on ");
//    }
//
//
//    @TargetApi(Build.VERSION_CODES.O)
//    private void initiateNotificationBarTest() {
//        String id = "0x22222";
//        String name="我是渠道名字";
//        int importance = NotificationManager.IMPORTANCE_HIGH;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
//            Toast.makeText(this, mChannel.toString(), Toast.LENGTH_SHORT).show();
//            Log.i(TAG, mChannel.toString());
//            notificationManager.createNotificationChannel(mChannel);
//        }
//
//        Notification.Builder notification = new Notification.Builder(this, id);
//        // 设置打开该通知，该通知自动消失
//        notification.setAutoCancel(true);
//        // 设置通知的图标
//        notification.setSmallIcon(R.mipmap.default_cover);
//        // 设置通知内容的标题
//        notification.setContentTitle("还不赶紧关注公众号");
//        // 设置通知内容
//        notification.setContentText("点击查看详情！");
//        //设置使用系统默认的声音、默认震动
//        notification.setDefaults(Notification.DEFAULT_SOUND
//                | Notification.DEFAULT_VIBRATE);
//        //设置发送时间
//        notification.setWhen(System.currentTimeMillis());
//        // 创建一个启动其他Activity的Intent
////        Intent intent = new Intent(this
////                , DetailActivity.class);
////        PendingIntent pi = PendingIntent.getActivity(
////                NotificationActivity.this, 0, intent, 0);
//        //设置通知栏点击跳转
////        notification.setContentIntent(pi);
//        //发送通知
//        Notification notification1 = notification.build();
//        notificationManager.notify(1, notification.build());
//        Log.d("yangzhang", "notification.getChannelId(): "+ notification1.getChannelId());
//        Log.d(TAG, "notification on");//从0开始
//    }

}
