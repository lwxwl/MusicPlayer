package com.example.lwxwl.simplemusicplayer;

import java.io.Serializable;

public class MusicData implements Serializable {

    private int musicRes;

    private int musicPicRes;

    private String song;

    private String singer;

    public MusicData(int musicRes, int musicPicRes, String song, String singer) {
        this.musicRes = musicRes;
        this.musicPicRes = musicPicRes;
        this.song = song;
        this.singer = singer;
    }

    public int getMusicRes() {
        return musicRes;
    }

    public int getMusicPicRes() {
        return musicPicRes;
    }

    public String getSong() {
        return song;
    }

    public String getSinger() {
        return singer;
    }
}

