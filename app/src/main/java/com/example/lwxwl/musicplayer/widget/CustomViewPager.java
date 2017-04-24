package com.example.lwxwl.musicplayer.widget;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;



public class CustomViewPager extends ViewPager {

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    PointF downPoint = new PointF();     // 触摸时的位置
    OnSingleTouchListener onSingleTouchListener;


    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录按下时的坐标
                downPoint.x = motionEvent.getX();
                downPoint.y = motionEvent.getY();
                if (this.getChildCount() > 1) {
                    // 通知父ViewPager现在进行的是本控件的操作，防止对操作进行干扰
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (this.getChildCount() > 1) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                // 判断按下和松手的点坐标是否相同
                if (PointF.length(motionEvent.getX() - downPoint.x, motionEvent.getY()
                        - downPoint.y) < (float) 5.0) {
                    onSingleTouch(this);
                    return true;
                }
                break;
        }
        return super.onTouchEvent(motionEvent);
    }

    public void onSingleTouch(View view) {
        if (onSingleTouchListener != null) {
            onSingleTouchListener.onSingleTouch(view);
        }
    }

    public interface OnSingleTouchListener {
        public void onSingleTouch(View view);
    }

    public void setOnSingleTouchListener(OnSingleTouchListener onSingleTouchListener) {
        this.onSingleTouchListener = onSingleTouchListener;
    }
}






