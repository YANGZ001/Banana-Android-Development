package com.example.testanr;

import android.graphics.Bitmap;
import android.net.Uri;

public class MusicItem {

    String name; //--存储音乐的名字
    Uri songUri; //--存储音乐的Uri地址
    Uri albumUri;//--存储音乐封面的Uri地址
    Bitmap thumb;//--存储封面图片
    long duration;//--存储音乐的播放时长，单位是毫秒
    String artist;//歌手
    int currentPosition;
    boolean hasAlbumArt = false;
    int currentItem;

    MusicItem(Uri songUri, Uri albumUri, String strName, String artist,long duration, Bitmap thumb) {
        this.name = strName;
        this.songUri = songUri;
        this.duration = duration;
        this.albumUri = albumUri;
        this.thumb = thumb;
        this.artist = artist;
        if (thumb != null){
            this.hasAlbumArt = true;
        }
    }

    MusicItem(Uri songUri, Uri albumUri, String strName, int currentPosition,String artist,long duration, Bitmap thumb) {
        this.name = strName;
        this.songUri = songUri;
        this.duration = duration;
        this.albumUri = albumUri;
        this.thumb = thumb;
        this.artist = artist;
        this.currentPosition = currentPosition;
        if (thumb != null){
            this.hasAlbumArt = true;
        }
    }

    //if needed, implement getXXX() and setXXX() methods.
}
