package com.wuyr.fanlayout;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;
import android.widget.Scroller;

/**
 * Created by wuyr on 18-5-8 下午7:10.
 * GitHub: https://github.com/wuyr/ArcSlidingHelper
 */
@SuppressWarnings("unused")
public class ArcSlidingHelper {

    private int mPivotX, mPivotY;
    private float mStartX, mStartY;
    private float mLastScrollOffset;
    private float mScrollAvailabilityRatio;
    private boolean isSelfSliding;
    private boolean isInertialSlidingEnable;
    private boolean isClockwiseScrolling;
    private boolean isShouldBeGetY;
    private boolean isRecycled;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private OnSlidingListener mListener;
    private InertialSlidingHandler mHandler;

    /**
     * 创建ArcSlidingHelper对象
     *
     * @param targetView 接受滑动手势的View (圆弧滑动事件以此View的中心点为圆心)
     * @param listener 当发生圆弧滚动时的回调
     * @return ArcSlidingHelper
     */
    public static ArcSlidingHelper create(View targetView, @NonNull OnSlidingListener listener) {
        int width = targetView.getWidth();
        int height = targetView.getHeight();
        if (width <= 0) {
            width = targetView.getMeasuredWidth();
            if (width <= 0) {
                throw new IllegalStateException("view width invalid: " + width);
            }
        }
        if (height <= 0) {
            height = targetView.getMeasuredHeight();
            if (height <= 0) {
                throw new IllegalStateException("view height invalid: " + height);
            }
        }
        width /= 2;
        height /= 2;
        int x = (int) getAbsoluteX(targetView);
        int y = (int) getAbsoluteY(targetView);
        return new ArcSlidingHelper(targetView.getContext(), x + width, y + height, listener);
    }

    private ArcSlidingHelper(Context context, int pivotX, int pivotY, @NonNull OnSlidingListener listener) {
        mPivotX = pivotX;
        mPivotY = pivotY;
        mListener = listener;
        mScroller = new Scroller(context);
        mVelocityTracker = VelocityTracker.obtain();
        mScrollAvailabilityRatio = .3F;
        mHandler = new InertialSlidingHandler(this);
    }

    /**
     * 获取view在屏幕中的绝对x坐标
     */
    private static float getAbsoluteX(View view) {
        float x = view.getX();
        ViewParent parent = view.getParent();
        if (parent != null && parent instanceof View) {
            x += getAbsoluteX((View) parent);
        }
        return x;
    }

    /**
     * 获取view在屏幕中的绝对y坐标
     */
    private static float getAbsoluteY(View view) {
        float y = view.getY();
        ViewParent parent = view.getParent();
        if (parent != null && parent instanceof View) {
            y += getAbsoluteY((View) parent);
        }
        return y;
    }

    /**
     * 设置自身滑动
     *
     * @param isSelfSliding 是否view自身滑动
     */
    public void setSelfSliding(boolean isSelfSliding) {
        checkIsRecycled();
        this.isSelfSliding = isSelfSliding;
    }

    /**
     * 设置惯性滑动
     *
     * @param enable 是否开启
     */
    public void enableInertialSliding(boolean enable) {
        checkIsRecycled();
        isInertialSlidingEnable = enable;
    }

    /**
     * 处理触摸事件
     */
    public void handleMovement(MotionEvent event) {
        checkIsRecycled();
        float x, y;
        if (isSelfSliding) {
            x = event.getRawX();
            y = event.getRawY();
        } else {
            x = event.getX();
            y = event.getY();
        }
        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                if (isInertialSlidingEnable) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    mScroller.fling(0, 0, (int) mVelocityTracker.getXVelocity(), (int) mVelocityTracker.getYVelocity(),
                            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    startFling();
                }
                break;
            default:
                break;
        }
        mStartX = x;
        mStartY = y;
    }

    /**
     * 处理惯性滚动
     */
    private void computeInertialSliding() {
        checkIsRecycled();
        if (mScroller.computeScrollOffset()) {
            float y = ((isShouldBeGetY ? mScroller.getCurrY() : mScroller.getCurrX()) * mScrollAvailabilityRatio);
            if (mLastScrollOffset != 0) {
                float offset = fixAngle(Math.abs(y - mLastScrollOffset));
                mListener.onSliding(isClockwiseScrolling ? offset : -offset);
            }
            mLastScrollOffset = y;
            startFling();
        } else if (mScroller.isFinished()) {
            mLastScrollOffset = 0;
        }
    }

    /**
     * 开始惯性滚动
     */
    private void startFling() {
        mHandler.sendEmptyMessage(0);
    }

    /**
     * 打断动画
     */
    public void abortAnimation() {
        checkIsRecycled();
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        checkIsRecycled();
        mScroller = null;
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mListener = null;
        mHandler = null;
        isRecycled = true;
    }

    /**
     * 更新当前手指触摸的坐标，在ViewGroup的onInterceptTouchEvent中使用
     */
    public void updateMovement(MotionEvent event) {
        checkIsRecycled();
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            if (isSelfSliding) {
                mStartX = event.getRawX();
                mStartY = event.getRawY();
            } else {
                mStartX = event.getX();
                mStartY = event.getY();
            }
        }
    }

    /**
     * 更新圆心x坐标
     *
     * @param pivotX 新的x坐标
     */

    public void updatePivotX(int pivotX) {
        checkIsRecycled();
        mPivotX = pivotX;
    }

    /**
     * 更新圆心y坐标
     *
     * @param pivotY 新的y坐标
     */
    public void updatePivotY(int pivotY) {
        checkIsRecycled();
        mPivotY = pivotY;
    }

    /**
     * 计算滑动的角度
     */
    private void handleActionMove(float x, float y) {
        float l, t, r, b;
        if (mStartX > x) {
            r = mStartX;
            l = x;
        } else {
            r = x;
            l = mStartX;
        }
        if (mStartY > y) {
            b = mStartY;
            t = y;
        } else {
            b = y;
            t = mStartY;
        }
        float pA1 = Math.abs(mStartX - mPivotX);
        float pA2 = Math.abs(mStartY - mPivotY);
        float pB1 = Math.abs(x - mPivotX);
        float pB2 = Math.abs(y - mPivotY);
        float hypotenuse = (float) Math.sqrt(Math.pow(r - l, 2) + Math.pow(b - t, 2));
        float lineA = (float) Math.sqrt(Math.pow(pA1, 2) + Math.pow(pA2, 2));
        float lineB = (float) Math.sqrt(Math.pow(pB1, 2) + Math.pow(pB2, 2));
        if (hypotenuse > 0 && lineA > 0 && lineB > 0) {
            float angle = fixAngle((float) Math.toDegrees(Math.acos((Math.pow(lineA, 2) + Math.pow(lineB, 2) - Math.pow(hypotenuse, 2)) / (2 * lineA * lineB))));
            if (!Float.isNaN(angle)) {
                mListener.onSliding((isClockwiseScrolling = isClockwise(x, y)) ? angle : -angle);
            }
        }
    }

    /**
     * 设置惯性滑动的利用率
     */
    public void setScrollAvailabilityRatio(@FloatRange(from = 0.0, to = 1.0) float ratio) {
        checkIsRecycled();
        mScrollAvailabilityRatio = ratio;
    }

    /**
     * 检查资源释放已经释放
     */
    private void checkIsRecycled() {
        if (isRecycled) {
            throw new IllegalStateException("ArcSlidingHelper is recycled!");
        }
    }

    /**
     * 调整角度，使其在360之间
     *
     * @param rotation 当前角度
     * @return 调整后的角度
     */
    private float fixAngle(float rotation) {
        float angle = 360F;
        if (rotation < 0) {
            rotation += angle;
        }
        if (rotation > angle) {
            rotation = rotation % angle;
        }
        return rotation;
    }

    /**
     * 检测手指是否顺时针滑动
     *
     * @param x 当前手指的x坐标
     * @param y 当前手指的y坐标
     * @return 是否顺时针
     */
    private boolean isClockwise(float x, float y) {
        return (isShouldBeGetY = Math.abs(y - mStartY) > Math.abs(x - mStartX)) ?
                x < mPivotX != y > mStartY : y < mPivotY == x > mStartX;
    }

    /**
     * 开始弧形滑动
     */
    public interface OnSlidingListener {
        /**
         * @param angle 本次滑动的角度
         */
        void onSliding(float angle);
    }

    /**
     * 主线程回调惯性滚动
     */
    private static class InertialSlidingHandler extends Handler {

        ArcSlidingHelper mHelper;

        InertialSlidingHandler(ArcSlidingHelper helper) {
            mHelper = helper;
        }

        @Override
        public void handleMessage(Message msg) {
            mHelper.computeInertialSliding();
        }
    }
}
