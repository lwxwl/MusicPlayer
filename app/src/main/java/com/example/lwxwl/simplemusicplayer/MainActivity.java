package com.example.lwxwl.simplemusicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.lwxwl.simplemusicplayer.service.MusicService;
import com.example.lwxwl.simplemusicplayer.utils.DisplayUtil;
import com.example.lwxwl.simplemusicplayer.utils.FastBlurUtil;
import com.example.lwxwl.simplemusicplayer.widget.BackgroundRelativeLayout;
import com.example.lwxwl.simplemusicplayer.widget.DiscView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.example.lwxwl.simplemusicplayer.R;


public class MainActivity extends AppCompatActivity implements DiscView.IPlayInfo, View.OnClickListener{

    public static final int MUSIC_MESSAGE = 0;
    public static final String PARAM_MUSIC_LIST = "PARAM_MUSIC_LIST";
    private static final long DURATION_NEEDLE_ANIMATOR = 500;

    private DiscView discView;
    private Toolbar toolbar;
    private SeekBar seekBar;
    private ImageView imgPlayOrPause, imgNext, imgLast;
    private TextView txvCurrentTime, txvTotalTime;
    private BackgroundRelativeLayout rootLayout;

    private MusicReceiver musicReceiver = new MusicReceiver();
    private List<MusicData> musicDatas = new ArrayList<>();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            seekBar.setProgress(seekBar.getProgress() + 1000);
            txvCurrentTime.setText(durationTime(seekBar.getProgress()));
            startUpdateSeekBarProgress();
            }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initMusicDatas();
        initMusicReceiver();
        makeStatusBarTransparent();
    }

    // 初始化界面
    private void initView() {
        discView = (DiscView) findViewById(R.id.disc_view);
        imgNext = (ImageView) findViewById(R.id.img_next);
        imgLast = (ImageView) findViewById(R.id.img_last);
        imgPlayOrPause = (ImageView) findViewById(R.id.img_play_or_pause);
        txvCurrentTime = (TextView) findViewById(R.id.txv_current_time);
        txvTotalTime = (TextView) findViewById(R.id.txv_total_time);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        rootLayout = (BackgroundRelativeLayout) findViewById(R.id.rootLayout);
        discView.setPlayInfoListener(this);
        imgNext.setOnClickListener(this);
        imgLast.setOnClickListener(this);
        imgPlayOrPause.setOnClickListener(this);
        txvCurrentTime.setText(durationTime(0));
        txvTotalTime.setText(durationTime(0));
        discView.setMusicDataList(musicDatas);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                txvCurrentTime.setText(durationTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopUpdateSeekBarProgress();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seek(seekBar.getProgress());
                startUpdateSeekBarProgress();
            }
        });

    }
    private void stopUpdateSeekBarProgress() {
        handler.removeMessages(MUSIC_MESSAGE);
    }


    private void initMusicDatas() {
        MusicData musicData1 = new MusicData(R.raw.music1, R.raw.ic_music1, "Counting Stars", "OneRepublic");
        MusicData musicData2 = new MusicData(R.raw.music2, R.raw.ic_music2, "Jar Of Love", "曲婉婷");
        MusicData musicData3 = new MusicData(R.raw.music3, R.raw.ic_music3, "The Cure", "Lady Gaga");

        musicDatas.add(musicData1);
        musicDatas.add(musicData2);
        musicDatas.add(musicData3);

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(PARAM_MUSIC_LIST, (Serializable) musicDatas);
        startService(intent);
    }

    private void initMusicReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.STATUS_PLAY);
        intentFilter.addAction(MusicService.STATUS_PAUSE);
        intentFilter.addAction(MusicService.STATUS_DURATION);
        intentFilter.addAction(MusicService.STATUS_COMPLETE);
        // 注册本地广播
        LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver,intentFilter);
    }

    private void makeStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private void tryTwiceUpdateMusicPicBackground(final int musicPicRes) {
        if (rootLayout.isNeedUpdateTwice(musicPicRes)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Drawable foregroundDrawable = getForegroundDrawable(musicPicRes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            rootLayout.setForeground(foregroundDrawable);
                            rootLayout.beginAnimation();
                        }
                    });
                }
            }).start();
        }
    }

    private Drawable getForegroundDrawable(int musicPicRes) {
        // 屏幕的宽高比，日常英语 aspect ratio
        final float aspectRatio = (float) (DisplayUtil.getScreenWidth(MainActivity.this) * 1.0 /
                DisplayUtil.getScreenHeight(MainActivity.this) * 1.0);
        Bitmap bitmap = getForegroundBitmap(musicPicRes);
        int cropBitmapWidth = (int) (aspectRatio * bitmap.getHeight());
        int cropBitmapWidthX = (int) ((bitmap.getWidth() - cropBitmapWidth) / 2.0);
        // 切割图片
        Bitmap cropBitmap = Bitmap.createBitmap(bitmap, cropBitmapWidthX, 0, cropBitmapWidth, bitmap.getHeight());
        // 缩小图片
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(cropBitmap, bitmap.getWidth() / 50, bitmap.getHeight() / 50, false);
        // 模糊化处理
        final Bitmap blurBitmap = FastBlurUtil.doBlur(scaledBitmap, 8, true);
        final Drawable foregroundDrawable = new BitmapDrawable(blurBitmap);
        // 加入灰色遮罩层，避免图片过亮影响其他控件
        foregroundDrawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);   // 取两图层交集部分叠加后颜色
        return foregroundDrawable;
    }
    // 颜色渲染部分参考CSDN上的系列文章
    // 网址http://blog.csdn.net/t12x3456/article/category/1648991

    private Bitmap getForegroundBitmap(int musicPicRes){
        int screenWidth = DisplayUtil.getScreenWidth(this);
        int screenHeight = DisplayUtil.getScreenHeight(this);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), musicPicRes, options);
        int imgWidth = options.outWidth;
        int imgHeight = options.outHeight;
        if (imgWidth < screenWidth && imgHeight < screenHeight) {
            return BitmapFactory.decodeResource(getResources(), musicPicRes);
        }
        int sample = 2;
        int sampleX = imgWidth / DisplayUtil.getScreenHeight(this);
        int sampleY = imgHeight / DisplayUtil.getScreenHeight(this);

        if (sampleX > sampleY && sampleY > 1) {
            sample = sampleX;
        } else if (sampleX < sampleY && sampleX > 1) {
            sample = sampleY;
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeResource(getResources(), musicPicRes, options);
    }

    @Override
    public void onMusicInfoChanged(String song, String singer) {
        getSupportActionBar().setTitle(song);
        getSupportActionBar().setSubtitle(singer);
    }

    @Override
    public void onMusicPicChanged(int musicPicRes) {
        tryTwiceUpdateMusicPicBackground(musicPicRes);
    }

    @Override
    public void onMusicChanged(DiscView.MusicChangedStatus musicChangedStatus) {
        switch (musicChangedStatus) {
            case PLAY:
                play();
                break;
            case PAUSE:
                pause();
                break;
            case NEXT:
                next();
                break;
            case LAST:
                last();
                break;
            case STOP:
                stop();
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view == imgNext) {
            discView.next();
        } else if (view == imgLast) {
            discView.last();
        } else if (view == imgPlayOrPause) {
            discView.playOrPause();
        }
    }

    class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicService.STATUS_PLAY)) {
                imgPlayOrPause.setImageResource(R.drawable.ic_pause);
                int currentPosition = intent.getIntExtra(MusicService.PARAM_CURRENT_POSITION, 0);
                seekBar.setProgress(currentPosition);
                if (!discView.isPlaying()) {
                    discView.playOrPause();
                }
            } else if (action.equals(MusicService.STATUS_PAUSE)) {
                imgPlayOrPause.setImageResource(R.drawable.ic_play);
                if (discView.isPlaying()) {
                    discView.playOrPause();
                }
            } else if (action.equals(MusicService.STATUS_DURATION)) {
                int duration = intent.getIntExtra(MusicService.PARAM_DURATION, 0);
                updateDurationInfo(duration);
            } else if (action.equals(MusicService.STATUS_COMPLETE)) {
                boolean isOver = intent.getBooleanExtra(MusicService.PARAM_COMPLETE, true);
            }
        }
    }

    private void play() {
        optMusic(MusicService.ACTION_PLAY);
        startUpdateSeekBarProgress();
    }

    private void pause() {
        optMusic(MusicService.ACTION_PAUSE);
        stopUpdateSeekBarProgress();
    }

    private void stop() {
        stopUpdateSeekBarProgress();
        imgPlayOrPause.setImageResource(R.drawable.ic_play);
        txvCurrentTime.setText(durationTime(0));
        txvTotalTime.setText(durationTime(0));
        seekBar.setProgress(0);
    }

    private void next() {
        rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                optMusic(MusicService.ACTION_NEXT);
            }
        }, DURATION_NEEDLE_ANIMATOR);
        stopUpdateSeekBarProgress();
        txvCurrentTime.setText(durationTime(0));
        txvTotalTime.setText(durationTime(0));
    }

    private void last() {
        rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                optMusic(MusicService.ACTION_LAST);
            }
        }, DURATION_NEEDLE_ANIMATOR);
        stopUpdateSeekBarProgress();
        txvCurrentTime.setText(durationTime(0));
        txvTotalTime.setText(durationTime(0));
    }

    private void complete(boolean isOver) {
        if (isOver) {
            discView.stop();
        } else {
            discView.next();
        }
    }

    private void optMusic(final String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
    }

    private void seek(int position) {
        Intent intent = new Intent(MusicService.ACTION_SEEK);
        intent.putExtra(MusicService.PARAM_SEEK, position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startUpdateSeekBarProgress() {
        // 避免重复发送
        stopUpdateSeekBarProgress();
        handler.sendEmptyMessageDelayed(0, 1000);
    }

    private String durationTime(int duration) {
        int m = duration / 1000 / 60;
        int s = duration / 1000 % 60;
        return (m < 10 ? "0" + m : m + "") + ":" + (s < 10 ? "0" + s : s + "");
    }

    private void updateDurationInfo(int totalDuration) {
        seekBar.setProgress(0);
        seekBar.setMax(totalDuration);
        txvCurrentTime.setText(durationTime(0));
        txvTotalTime.setText(durationTime(totalDuration));
        startUpdateSeekBarProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicReceiver);
    }
}




