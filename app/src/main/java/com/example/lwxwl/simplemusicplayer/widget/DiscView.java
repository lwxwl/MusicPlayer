package com.example.lwxwl.simplemusicplayer.widget;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.lwxwl.simplemusicplayer.utils.DisplayUtil;
import com.example.lwxwl.simplemusicplayer.MusicData;
import com.example.lwxwl.simplemusicplayer.R;

import java.util.ArrayList;
import java.util.List;


public class DiscView extends RelativeLayout {

    private ImageView imgNeedle;
    private ViewPager vpContain;
    private ViewPagerAdapter vpAdapter;
    private ObjectAnimator needleAnimator;

    private List<View> discLayouts = new ArrayList<>();

    private List<MusicData> musicDatas = new ArrayList<>();
    private List<ObjectAnimator> discAnimators = new ArrayList<>();
    // 标记ViewPager是否处于偏移的状态
    private boolean vpIsOffset = false;

    // 标记唱针复位后，是否需要重新偏移到唱片处
    private boolean isNeedTwicePlayAnimator = false;
    private MusicStatus musicStatus = MusicStatus.STOP;

    public static final int DURATION_NEEDLE_ANIMATOR = 500;
    private NeedleAnimatorStatus needleAnimatorStatus = NeedleAnimatorStatus.IN_FAR_END;

    private IPlayInfo info;

    private int screenWidth, screenHeight;

    // 唱针当前所处的状态
    private enum NeedleAnimatorStatus {
        // 从唱盘往远处移动
        TO_FAR_END,
        // 从远处往唱盘移动
        TO_NEAR_END,
        // 离开唱盘
        IN_FAR_END,
        // 贴近唱盘
        IN_NEAR_END
    }

    public enum MusicStatus {
        PLAY, PAUSE, STOP
    }

    public enum MusicChangedStatus {
        PLAY, PAUSE, NEXT, LAST, STOP
    }

    public interface IPlayInfo {
        // 更新标题栏的变化
        public void onMusicInfoChanged(String song, String singer);
        // 更新背景图片
        public void onMusicPicChanged(int musicPicRes);
        // 更新音乐播放状态
        public void onMusicChanged(MusicChangedStatus musicChangedStatus);
    }

    public DiscView(Context context) {
        this(context, null);
    }

    public DiscView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DiscView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        screenWidth = DisplayUtil.getScreenWidth(context);
        screenHeight = DisplayUtil.getScreenHeight(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initDiscBackground();
        initViewPager();
        initNeedle();
        initObjectAnimator();
    }

    private void initDiscBackground() {
        ImageView discBackground = (ImageView) findViewById(R.id.img_disc_background);
        discBackground.setImageDrawable(getDiscBackgroundDrawable());

        int marginTop = (int) (DisplayUtil.SCALE_DISC_MARGIN_TOP * screenHeight);
        RelativeLayout.LayoutParams layoutParams = (LayoutParams) discBackground
                .getLayoutParams();
        layoutParams.setMargins(0, marginTop, 0, 0);

        discBackground.setLayoutParams(layoutParams);
    }

    private void initViewPager() {
        vpAdapter = new ViewPagerAdapter();
        vpContain = (ViewPager) findViewById(R.id.vp_disc_contain);
        vpContain.setOverScrollMode(View.OVER_SCROLL_NEVER);
        vpContain.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            int lastPositionOffsetPixels = 0;
            int currentItem = 0;
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //左滑
                if (lastPositionOffsetPixels > positionOffsetPixels) {
                    if (positionOffset < 0.5) {
                        notifyMusicInfoChanged(position);
                    } else {
                        notifyMusicInfoChanged(vpContain.getCurrentItem());
                    }
                }
                //右滑
                else if (lastPositionOffsetPixels < positionOffsetPixels) {
                    if (positionOffset > 0.5) {
                        notifyMusicInfoChanged(position + 1);
                    } else {
                        notifyMusicInfoChanged(position);
                    }
                }
                lastPositionOffsetPixels = positionOffsetPixels;
            }

            @Override
            public void onPageSelected(int position) {
                resetOtherDiscAnimation(position);
                notifyMusicPicChanged(position);
                if (position > currentItem) {
                    notifyMusicStatusChanged(MusicChangedStatus.NEXT);
                } else {
                    notifyMusicStatusChanged(MusicChangedStatus.LAST);
                }
                currentItem = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                doWithAnimatorOnPageScroll(state);
            }
        });
        vpContain.setAdapter(vpAdapter);

        RelativeLayout.LayoutParams layoutParams = (LayoutParams) vpContain.getLayoutParams();
        int marginTop = (int) (DisplayUtil.SCALE_DISC_MARGIN_TOP * screenHeight);
        layoutParams.setMargins(0, marginTop, 0, 0);
        vpContain.setLayoutParams(layoutParams);
    }

    private void resetOtherDiscAnimation(int position) {
        for (int i = 0; i < discLayouts.size(); i++) {
            if (position == i) continue;
            discAnimators.get(position).cancel();
            ImageView imageView = (ImageView) discLayouts.get(i).findViewById(R.id.img_disc);
            imageView.setRotation(0);
        }
    }

    private void doWithAnimatorOnPageScroll(int state) {
        switch (state) {
            case ViewPager.SCROLL_STATE_IDLE:
            case ViewPager.SCROLL_STATE_SETTLING: {
                vpIsOffset = false;
                if (musicStatus == MusicStatus.PLAY) {
                    playAnimator();
                }
                break;
            }
            case ViewPager.SCROLL_STATE_DRAGGING: {
                vpIsOffset = true;
                pauseAnimator();
                break;
            }
        }
    }

    private void initNeedle() {
        imgNeedle = (ImageView) findViewById(R.id.img_needle);

        int needleWidth = (int) (DisplayUtil.SCALE_NEEDLE_WIDTH * screenWidth);
        int needleHeight = (int) (DisplayUtil.SCALE_NEEDLE_HEIGHT * screenHeight);

        int marginTop = (int) (DisplayUtil.SCALE_NEEDLE_MARGIN_TOP * screenHeight) * -1;
        int marginLeft = (int) (DisplayUtil.SCALE_NEEDLE_MARGIN_LEFT * screenWidth);

        Bitmap originBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_needle);
        Bitmap bitmap = Bitmap.createScaledBitmap(originBitmap, needleWidth, needleHeight, false);

        RelativeLayout.LayoutParams layoutParams = (LayoutParams) imgNeedle.getLayoutParams();
        layoutParams.setMargins(marginLeft, marginTop, 0, 0);

        int pivotX = (int) (DisplayUtil.SCALE_NEEDLE_PIVOT_X * screenWidth);
        int pivotY = (int) (DisplayUtil.SCALE_NEEDLE_PIVOT_Y * screenWidth);

        imgNeedle.setPivotX(pivotX);
        imgNeedle.setPivotY(pivotY);
        imgNeedle.setRotation(DisplayUtil.ROTATION_INIT_NEEDLE);
        imgNeedle.setImageBitmap(bitmap);
        imgNeedle.setLayoutParams(layoutParams);
    }

    private void initObjectAnimator() {
        needleAnimator = ObjectAnimator.ofFloat(imgNeedle, View.ROTATION, DisplayUtil.ROTATION_INIT_NEEDLE, 0);
        needleAnimator.setDuration(DURATION_NEEDLE_ANIMATOR);
        needleAnimator.setInterpolator(new AccelerateInterpolator());
        needleAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (needleAnimatorStatus == NeedleAnimatorStatus.IN_FAR_END) {
                    needleAnimatorStatus = NeedleAnimatorStatus.TO_NEAR_END;
                } else if (needleAnimatorStatus == NeedleAnimatorStatus.IN_NEAR_END) {
                    needleAnimatorStatus = NeedleAnimatorStatus.TO_FAR_END;
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {

                if (needleAnimatorStatus == NeedleAnimatorStatus.TO_NEAR_END) {
                    needleAnimatorStatus = NeedleAnimatorStatus.IN_NEAR_END;
                    int index = vpContain.getCurrentItem();
                    playDiscAnimator(index);
                    musicStatus = MusicStatus.PLAY;
                } else if (needleAnimatorStatus == NeedleAnimatorStatus.TO_FAR_END) {
                    needleAnimatorStatus = NeedleAnimatorStatus.IN_FAR_END;
                    if (musicStatus == MusicStatus.STOP) {
                        isNeedTwicePlayAnimator = true;
                    }
                }

                if (isNeedTwicePlayAnimator) {
                    isNeedTwicePlayAnimator = false;
                    if (!vpIsOffset) {
                        DiscView.this.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                playAnimator();
                            }
                        }, 50);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    public void setPlayInfoListener(IPlayInfo listener) {
        this.info = listener;
    }

    // 唱盘背后半透明的圆形背景
    private Drawable getDiscBackgroundDrawable() {
        int discSize = (int) (screenWidth * DisplayUtil.SCALE_DISC_SIZE);
        Bitmap bitmapDisc = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_disc_blackground), discSize, discSize, false);
        RoundedBitmapDrawable roundDiscDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmapDisc);
        return roundDiscDrawable;
    }


     // 得到唱盘图片
    private Drawable getDiscDrawable(int musicPicRes) {
        int discSize = (int) (screenWidth * DisplayUtil.SCALE_DISC_SIZE);
        int musicPicSize = (int) (screenWidth * DisplayUtil.SCALE_MUSIC_PIC_SIZE);

        Bitmap bitmapDisc = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_disc), discSize, discSize, false);
        Bitmap bitmapMusicPic = getMusicPicBitmap(musicPicSize,musicPicRes);
        BitmapDrawable discDrawable = new BitmapDrawable(bitmapDisc);
        RoundedBitmapDrawable roundMusicDrawable = RoundedBitmapDrawableFactory.create
                (getResources(), bitmapMusicPic);
        discDrawable.setAntiAlias(true);
        roundMusicDrawable.setAntiAlias(true);

        Drawable[] drawables = new Drawable[2];
        drawables[0] = roundMusicDrawable;
        drawables[1] = discDrawable;

        LayerDrawable layerDrawable = new LayerDrawable(drawables);
        int musicPicMargin = (int) ((DisplayUtil.SCALE_DISC_SIZE - DisplayUtil.SCALE_MUSIC_PIC_SIZE) * screenWidth / 2);
        layerDrawable.setLayerInset(0, musicPicMargin, musicPicMargin, musicPicMargin, musicPicMargin);

        return layerDrawable;
    }

    private Bitmap getMusicPicBitmap(int musicPicSize, int musicPicRes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(getResources(),musicPicRes,options);
        int imageWidth = options.outWidth;

        int sample = imageWidth / musicPicSize;
        int dstSample = 1;
        if (sample > dstSample) {
            dstSample = sample;
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = dstSample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                musicPicRes, options), musicPicSize, musicPicSize, true);
    }

    public void setMusicDataList(List<MusicData> musicDataList) {
        if (musicDataList.isEmpty()) return;

        discLayouts.clear();
        musicDatas.clear();
        discAnimators.clear();
        musicDatas.addAll(musicDataList);

        int i = 0;
        for (MusicData musicData : musicDatas) {
            View discLayout = LayoutInflater.from(getContext()).inflate(R.layout.disc_layout, vpContain, false);

            ImageView disc = (ImageView) discLayout.findViewById(R.id.img_disc);
            disc.setImageDrawable(getDiscDrawable(musicData.getMusicPicRes()));

            discAnimators.add(getDiscObjectAnimator(disc, i++));
            discLayouts.add(discLayout);
        }
        vpAdapter.notifyDataSetChanged();

        MusicData musicData = musicDatas.get(0);
        if (info != null) {
            info.onMusicInfoChanged(musicData.getSong(), musicData.getSinger());
            info.onMusicPicChanged(musicData.getMusicPicRes());
        }
    }

    private ObjectAnimator getDiscObjectAnimator(ImageView disc, final int i) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(disc, View.ROTATION, 0, 360);
        objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objectAnimator.setDuration(20 * 1000);
        objectAnimator.setInterpolator(new LinearInterpolator());

        return objectAnimator;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void playAnimator() {
        // 唱针处于远端时，直接播放动画
        if (needleAnimatorStatus == NeedleAnimatorStatus.IN_FAR_END) {
            needleAnimator.start();
        }
        // 唱针处于往远端移动时，设置标记，等动画结束后再播放动画
        else if (needleAnimatorStatus == NeedleAnimatorStatus.TO_FAR_END) {
            isNeedTwicePlayAnimator = true;
        }
    }

    private void pauseAnimator() {
        // 播放时暂停动画
        if (needleAnimatorStatus == NeedleAnimatorStatus.IN_NEAR_END) {
            int index = vpContain.getCurrentItem();
            pauseDiscAnimatior(index);
        }
        // 唱针往唱盘移动时暂停动画
        else if (needleAnimatorStatus == NeedleAnimatorStatus.TO_NEAR_END) {
            needleAnimator.reverse();
            needleAnimatorStatus = NeedleAnimatorStatus.TO_FAR_END;
        }
        if (musicStatus == MusicStatus.STOP) {
            notifyMusicStatusChanged(MusicChangedStatus.STOP);
        }else if (musicStatus == MusicStatus.PAUSE) {
            notifyMusicStatusChanged(MusicChangedStatus.PAUSE);
        }
    }

    // 播放唱盘动画
    @TargetApi(19)
    private void playDiscAnimator(int index) {
        ObjectAnimator objectAnimator = discAnimators.get(index);
        if (objectAnimator.isPaused()) {
            objectAnimator.resume();
        } else {
            objectAnimator.start();
        }
        if (musicStatus != MusicStatus.PLAY) {
            notifyMusicStatusChanged(MusicChangedStatus.PLAY);
        }
    }

    // 暂停唱盘动画
    @TargetApi(19)
    private void pauseDiscAnimatior(int index) {
        ObjectAnimator objectAnimator = discAnimators.get(index);
        objectAnimator.pause();
        needleAnimator.reverse();
    }

    public void notifyMusicInfoChanged(int position) {
        if (info != null) {
            MusicData musicData = musicDatas.get(position);
            info.onMusicInfoChanged(musicData.getSong(), musicData.getSinger());
        }
    }

    public void notifyMusicPicChanged(int position) {
        if (info != null) {
            MusicData musicData = musicDatas.get(position);
            info.onMusicPicChanged(musicData.getMusicPicRes());
        }
    }

    public void notifyMusicStatusChanged(MusicChangedStatus musicChangedStatus) {
        if (info != null) {
            info.onMusicChanged(musicChangedStatus);
        }
    }

    private void play() {
        playAnimator();
    }

    private void pause() {
        musicStatus = MusicStatus.PAUSE;
        pauseAnimator();
    }

    public void stop() {
        musicStatus = MusicStatus.STOP;
        pauseAnimator();
    }

    public void playOrPause() {
        if (musicStatus == MusicStatus.PLAY) {
            pause();
        } else {
            play();
        }
    }

    public void next() {
        int currentItem = vpContain.getCurrentItem();
        if (currentItem == musicDatas.size() - 1) {
            Toast.makeText(getContext(), "已经到达最后一首", Toast.LENGTH_SHORT).show();
        } else {
            selectMusicWithButton();
            vpContain.setCurrentItem(currentItem + 1, true);
        }
    }

    public void last() {
        int currentItem = vpContain.getCurrentItem();
        if (currentItem == 0) {
            Toast.makeText(getContext(), "已经到达第一首", Toast.LENGTH_SHORT).show();
        } else {
            selectMusicWithButton();
            vpContain.setCurrentItem(currentItem - 1, true);
        }
    }

    public boolean isPlaying() {
        return musicStatus == MusicStatus.PLAY;
    }

    private void selectMusicWithButton() {
        if (musicStatus == MusicStatus.PLAY) {
            isNeedTwicePlayAnimator = true;
            pauseAnimator();
        } else if (musicStatus == MusicStatus.PAUSE) {
            play();
        }
    }

    class ViewPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View discLayout = discLayouts.get(position);
            container.addView(discLayout);
            return discLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(discLayouts.get(position));
        }

        @Override
        public int getCount() {
            return discLayouts.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}

