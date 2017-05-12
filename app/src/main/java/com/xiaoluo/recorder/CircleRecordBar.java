package com.xiaoluo.recorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import static android.graphics.Paint.Style.STROKE;

/**
 * 环形录制进度条
 *
 * author: xiaoluo
 * date: 2017/5/11 15:11
 */
public class CircleRecordBar extends View implements Handler.Callback {
    private final static String TAG = CircleRecordBar.class.getSimpleName();
    private static final int MSG_STOP = 110;    // 停止
    private static final int MSG_CANCEL = 120;  // 取消
    private static final int MSG_BEGIN = 130;   // 开始

    private Context mContext;

    private Paint mPaint;              // 画笔
    private int mCircleColor;          // 圆环初始颜色
    private int mProgressColor;        // 进度条颜色
    private float mCircleWidth;        // 圆环宽度
    private int mTimeColor;            // 中间时间文字颜色
    private float mTimeSize;           // 文字大小
    private boolean isShowTime;        // 是否显示时间
    private int mMaxTime = 10 * 1000;  // 录制最长时间(ms)
    private int mMinTime = 1 * 1000;   // 录制最短时间(ms)
    private int mCurrentTime = 0;      // 当前时间

    private boolean isTiming = false;  // 是否计时中
    private int mRefreshTime = 200;    //进度刷新间隔
    private TimeThread mTimeThread;    // 计时线程
    private Handler mHandler = new Handler(this);
    private OnClickEvent mListener;

    public CircleRecordBar(Context context) {
        this(context, null);
    }

    public CircleRecordBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleRecordBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        mPaint = new Paint();

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTiming) {
                    mHandler.sendEmptyMessage(MSG_STOP);
                } else {
                    mHandler.sendEmptyMessage(MSG_BEGIN);
                }
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centreX = getWidth() / 2;                     // 圆心X坐标
        int radius = (int) (centreX - mCircleWidth / 2);  // 圆环半径

        // 大圆环
        mPaint.setColor(mCircleColor);          // 设置圆环颜色
        mPaint.setStyle(STROKE);   // 设置空心
        mPaint.setStrokeWidth(mCircleWidth);    // 设置圆环宽度
        mPaint.setAntiAlias(true);              // 消除锯齿
        canvas.drawCircle(centreX, centreX, radius, mPaint);

        // 时间
        float textWidth = mPaint.measureText(mCurrentTime + "ms");
        mPaint.setStrokeWidth(0);
        mPaint.setColor(mTimeColor);
        mPaint.setTextSize(mTimeSize);
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(mCurrentTime + "ms", centreX - textWidth / 2, centreX + mTimeSize / 2, mPaint);


        // 进度
        mPaint.setStrokeWidth(mCircleWidth);
        mPaint.setColor(mProgressColor);
        RectF oval = new RectF(centreX - radius, centreX - radius, centreX + radius, centreX + radius); // 定义边界
        mPaint.setStyle(STROKE);
        canvas.drawArc(oval, 180, 360 * mCurrentTime / mMaxTime, false, mPaint);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_BEGIN:
                mCurrentTime = 0;
                isTiming = true;
                // 初始化计时线程
                mTimeThread = new TimeThread();
                if (mListener != null) {
                    mListener.onBegin();
                }

                break;
            case MSG_STOP:
                if (!isTiming) {
                    return true;
                }
                isTiming = false;
                stopTime();
                if (mListener != null) {
                    mListener.onStop();
                }
                break;
        }
        return true;
    }

    /**
     * 刷新进度
     */
    public synchronized void refreshProgress(int time) {
        if (time < 0) {
            return;
        }

        // 到达最大时间时,发送停止
        if (time >= mMaxTime) {
            mCurrentTime = mMaxTime;
            mHandler.sendEmptyMessage(MSG_STOP);
        } else {
            mCurrentTime = time;
            postInvalidate();
        }
    }

    /**
     * 计时线程
     */
    public class TimeThread implements Runnable {
        Thread thread;

        @Override
        public void run() {
            while (isTiming && mCurrentTime < mMaxTime) {
                try {
                    Thread.sleep(mRefreshTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mCurrentTime += mRefreshTime;
                refreshProgress(mCurrentTime);
            }
        }

        /**
         * 开始线程
         */
        public void startThread() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }
    }


    /**
     * 开始计时
     */
    public void startTime() {
        mTimeThread.startThread();
    }

    /**
     * 停止计时
     */
    public void stopTime() {
        isTiming = false;
    }

    /**
     * 设置点击回调
     */
    public void setOnClickEvent(OnClickEvent listener) {
        mListener = listener;
    }

    public int getCircleColor() {
        return mCircleColor;
    }

    public void setCircleColor(int circleColor) {
        mCircleColor = circleColor;
    }

    public int getProgressColor() {
        return mProgressColor;
    }

    public void setProgressColor(int progressColor) {
        mProgressColor = progressColor;
    }

    public float getCircleWidth() {
        return mCircleWidth;
    }

    public void setCircleWidth(float circleWidth) {
        mCircleWidth = circleWidth;
    }

    public int getTimeColor() {
        return mTimeColor;
    }

    public void setTimeColor(int timeColor) {
        mTimeColor = timeColor;
    }

    public float getTimeSize() {
        return mTimeSize;
    }

    public void setTimeSize(float timeSize) {
        mTimeSize = timeSize;
    }

    public boolean isShowTime() {
        return isShowTime;
    }

    public void setShowTime(boolean showTime) {
        isShowTime = showTime;
    }

    public int getMaxTime() {
        return mMaxTime;
    }

    public void setMaxTime(int maxTime) {
        mMaxTime = maxTime;
    }

    public int getMinTime() {
        return mMinTime;
    }

    public void setMinTime(int minTime) {
        mMinTime = minTime;
    }

    public int getSleepTime() {
        return mRefreshTime;
    }

    public void setSleepTime(int sleepTime) {
        mRefreshTime = sleepTime;
    }

    /**
     * 触摸事件回调
     */
    public interface OnClickEvent {
        void onBegin();

        void onStop();
    }
}
