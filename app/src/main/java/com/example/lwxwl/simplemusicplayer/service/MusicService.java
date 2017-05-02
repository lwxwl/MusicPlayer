package com.example.lwxwl.simplemusicplayer.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.example.lwxwl.simplemusicplayer.MainActivity;
import com.example.lwxwl.simplemusicplayer.MusicData;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by lwxwl on 2017/5/2.
 */

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {
    // 操作指令
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_LAST = "ACTION_LAST";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_SEEK = "ACTION_SEEK";

    // 状态指令
    public static final String STATUS_PLAY = "STATUS_PLAY";
    public static final String STATUS_PAUSE = "STATUS_PAUSE";
    public static final String STATUS_COMPLETE = "STATUS_COMPLETE";
    public static final String STATUS_DURATION = "STATUS_DURATION";

    public static final String PARAM_SEEK = "PARAM_SEEK";
    public static final String PARAM_COMPLETE = "PARAM_COMPLETE";
    public static final String PARAM_DURATION = "PARAM_DURATION";
    public static final String PARAM_CURRENT_POSITION = "PARAM_CURRENT_POSITION";

    private int currentMusicIndex = 0;
    private boolean isPause = false;
    private List<MusicData> musicDatas = new ArrayList<>();
    private MusicReceiver musicReceiver = new MusicReceiver();
    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initMusicDatas(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initBoardCastReceiver();
    }

    private void initMusicDatas(Intent intent) {
        if (intent == null) {
            return;
        }
        List<MusicData> musicDatas = (List<MusicData>) intent.getSerializableExtra(MainActivity.PARAM_MUSIC_LIST);
        musicDatas.addAll(musicDatas);
    }

    private void initBoardCastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_LAST);
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_SEEK);
        LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        mediaPlayer = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicReceiver);
    }

    private void play(final int index) {
        if (index >= musicDatas.size()) {
            return;
        }
        if (currentMusicIndex == index && isPause) {
            mediaPlayer.start();
        } else {
            mediaPlayer.stop();
            mediaPlayer = null;
            mediaPlayer = MediaPlayer.create(getApplicationContext(), musicDatas.get(index).getMusicRes());
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(this);
            currentMusicIndex = index;
            isPause = false;
            int duration = mediaPlayer.getDuration();
            sendDurationBroadCast(duration);
        }
        sendStatusBroadCast(STATUS_PLAY);
    }

    private void pause() {
        mediaPlayer.pause();
        isPause = true;
        sendStatusBroadCast(STATUS_PAUSE);
    }

    private void stop() {
        mediaPlayer.stop();
    }

    private void next() {
        if (currentMusicIndex + 1 < musicDatas.size()) {
            play(currentMusicIndex + 1);
        } else {
            stop();
        }
    }

    private void last() {
        if (currentMusicIndex != 0) {
            play(currentMusicIndex - 1);
        }
    }

    private void seek(Intent intent) {
        if (mediaPlayer.isPlaying()) {
            int position = intent.getIntExtra(PARAM_SEEK, 0);
            mediaPlayer.seekTo(position);
        }
    }

    public void onCompletion(MediaPlayer mediaPlayer) {
        sendCompleteBroadCast();
    }

    private void sendDurationBroadCast(int duration) {
        Intent intent = new Intent(STATUS_DURATION);
        intent.putExtra(PARAM_DURATION, duration);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCompleteBroadCast() {
        Intent intent = new Intent(STATUS_COMPLETE);
        intent.putExtra(PARAM_COMPLETE, (currentMusicIndex == musicDatas.size() - 1));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendStatusBroadCast(String action) {
        Intent intent = new Intent(action);
        if (action.equals(STATUS_PLAY)) {
            intent.putExtra(PARAM_CURRENT_POSITION, mediaPlayer.getCurrentPosition());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_PLAY)) {
                play(currentMusicIndex);
            } else if (action.equals(ACTION_PAUSE)) {
                pause();
            } else if (action.equals(ACTION_NEXT)) {
                next();
            } else if (action.equals(ACTION_LAST)) {
                last();
            } else if (action.equals(ACTION_SEEK)) {
                seek(intent);
            }
        }
    }
}


