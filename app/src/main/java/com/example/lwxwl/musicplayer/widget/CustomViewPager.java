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
    PointF currPoint = new PointF();     // 触摸时当前的位置
    OnSingleTouchListener onSingleTouchListener;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        // onTouch拦截在此控件，进而执行此控件的onTouchEvent
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // 每次onTouch事件都记录当前的坐标
        currPoint.x = motionEvent.getX();
        currPoint.y = motionEvent.getY();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
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
                if ((currPoint.x - downPoint.x <= (float) 5.0 && currPoint.x - downPoint.x >= - (float) 5.0)
                        && (currPoint.y - downPoint.y <= (float) 5.0 && currPoint.y - downPoint.y >= - (float) 5.0)) {
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

    // 创建点击事件接口
    public interface onSingleTouchListener {
        public void onSingleTouch(View view);
    }

    public void setOnSingleTouchListener(OnSingleTouchListener onSingleTouchListener) {
        this.onSingleTouchListener = onSingleTouchListener;
    }
}






