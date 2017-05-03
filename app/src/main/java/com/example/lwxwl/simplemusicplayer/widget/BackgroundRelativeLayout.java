package com.example.lwxwl.simplemusicplayer.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.widget.RelativeLayout;
import com.example.lwxwl.simplemusicplayer.R;

public class BackgroundRelativeLayout extends RelativeLayout {

    private final int INDEX_BACKGROUND = 0;
    private final int INDEX_FOREGROUND = 0;
    private final int DURATION_ANIMATION = 0;
    private int musicPicRes = -1;

    private LayerDrawable layerDrawable;
    private ObjectAnimator objectAnimator;

    public BackgroundRelativeLayout(Context context) {
        this(context, null);
    }

    public BackgroundRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BackgroundRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayerDrawable();
        initObjectAnimator();
    }

    @TargetApi(21)
    private void initLayerDrawable() {
        Drawable backgroundDrawable = getContext().getDrawable(R.drawable.ic_background);
        Drawable[] drawables = new Drawable[2];
        drawables[INDEX_BACKGROUND] = backgroundDrawable;
        drawables[INDEX_FOREGROUND] = backgroundDrawable;
        layerDrawable = new LayerDrawable(drawables);
    }

    @TargetApi(16)
    private void initObjectAnimator() {
        objectAnimator = ObjectAnimator.ofFloat(this,"numbers", 0f, 1.0f);
        objectAnimator.setDuration(DURATION_ANIMATION);
        objectAnimator.setInterpolator(new AccelerateInterpolator());
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int foregroundAlpha = (int) ((float) valueAnimator.getAnimatedValue() * 255);
                layerDrawable.getDrawable(INDEX_FOREGROUND).setAlpha(foregroundAlpha);
                BackgroundRelativeLayout.this.setBackground(layerDrawable);
            }
        });
        objectAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @TargetApi(23)
            @Override
            public void onAnimationEnd(Animator animator) {
                layerDrawable.setDrawable(INDEX_BACKGROUND, layerDrawable.getDrawable(INDEX_FOREGROUND));
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    @TargetApi(23)
    public void setForeground(Drawable drawable) {
        layerDrawable.setDrawable(INDEX_FOREGROUND, drawable);
    }

    public void beginAnimation() {
        objectAnimator.start();
    }

    public boolean isNeedUpdateTwice(int musicPicRes) {
        if (this.musicPicRes == -1) {
            return true;
        }
        return false;
    }
}

