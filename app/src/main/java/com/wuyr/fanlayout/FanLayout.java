package com.wuyr.fanlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by wuyr on 18-5-5 下午6:15.
 */
public class FanLayout extends ViewGroup {

    @IntDef({LEFT, RIGHT, TOP, BOTTOM, LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Gravity {
    }

    public static final int LEFT = 0, RIGHT = 1, TOP = 2, BOTTOM = 3,
            LEFT_TOP = 4, LEFT_BOTTOM = 5, RIGHT_TOP = 6, RIGHT_BOTTOM = 7;

    @IntDef({TYPE_COLOR, TYPE_VIEW})
    @Retention(RetentionPolicy.SOURCE)
    private @interface BearingType {
    }

    public static final int TYPE_COLOR = 0, TYPE_VIEW = 1;
    private int mRadius;
    private int mBearingOffset;
    private int mItemOffset;
    private int mCurrentGravity;
    private int mPivotX, mPivotY;
    private float mStartX, mStartY;
    private boolean isAutoSelect;
    private boolean isBearingCanRoll;
    private boolean isBearingOnBottom;
    private int mCurrentBearingType;
    private int mBearingColor;
    private int mBearingLayoutId;
    private View mBearingView;
    private Paint mPaint;
    private Scroller mScroller;//平滑滚动辅助
    private VelocityTracker mVelocityTracker;//手指滑动速率搜集
    private boolean isClockwiseScrolling;
    private boolean isShouldBeGetY;
    private int mTouchSlop;//触发滑动的最小距离
    private boolean isBeingDragged;//已经开始了拖动
    private float mScrollAvailabilityRatio;//滑动的利用率
    private ValueAnimator mAnimator;
    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemRotateListener mOnItemRotateListener;

    public FanLayout(Context context) {
        this(context, null);
    }

    public FanLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FanLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initAttrs(context, attrs, defStyleAttr);

        mScrollAvailabilityRatio = .3F;
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mScroller = new Scroller(getContext());
        mVelocityTracker = VelocityTracker.obtain();
    }

    private void initAttrs(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FanLayout, defStyleAttr, 0);
        isBearingCanRoll = a.getBoolean(R.styleable.FanLayout_bearing_can_roll, false);
        isBearingOnBottom = a.getBoolean(R.styleable.FanLayout_bearing_on_bottom, false);
        isAutoSelect = a.getBoolean(R.styleable.FanLayout_auto_select, false);
        mCurrentBearingType = a.getInteger(R.styleable.FanLayout_bearing_type, TYPE_COLOR);
        if (mCurrentBearingType == TYPE_VIEW) {
            mBearingLayoutId = a.getResourceId(R.styleable.FanLayout_bearing_layout, 0);
            if (mBearingLayoutId == 0) {
                throw new IllegalStateException("bearing layout not set!");
            } else {
                mBearingView = LayoutInflater.from(context).inflate(mBearingLayoutId, this, false);
                addView(mBearingView);
            }
        } else {
            mRadius = a.getDimensionPixelSize(R.styleable.FanLayout_bearing_radius, 0);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(mBearingColor = a.getColor(R.styleable.FanLayout_bearing_color, Color.BLACK));
            setWillNotDraw(false);
        }
        mBearingOffset = a.getDimensionPixelSize(R.styleable.FanLayout_bearing_offset, 0);
        mItemOffset = a.getDimensionPixelSize(R.styleable.FanLayout_item_offset, 0);
        mCurrentGravity = a.getInteger(R.styleable.FanLayout_bearing_gravity, LEFT);
        a.recycle();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        abortAnimation();
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                isBeingDragged = false;
                isScrolled = true;
                mVelocityTracker.computeCurrentVelocity(1000);
                mScroller.fling(0, 0, (int) mVelocityTracker.getXVelocity(), (int) mVelocityTracker.getYVelocity(),
                        Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
                invalidate();
                break;
            default:
                break;
        }
        mStartX = x;
        mStartY = y;
        return true;
    }

    private void handleActionMove(float x, float y) {
        float l, t, r, b;
        //先调整下长宽数据
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
            float angle = (float) Math.toDegrees(Math.acos((Math.pow(lineA, 2) + Math.pow(lineB, 2) - Math.pow(hypotenuse, 2)) / (2 * lineA * lineB)));
            if (!Float.isNaN(angle)) {
                isClockwiseScrolling = isClockwise(x, y);
                rotation(isClockwiseScrolling ? angle : -angle);
            }
        }
    }

    private float mLastScrollOffset;
    private boolean isScrolled;

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            float y = ((isShouldBeGetY ? mScroller.getCurrY() : mScroller.getCurrX()) * mScrollAvailabilityRatio);
            if (mLastScrollOffset != 0) {
                float offset = Math.abs(y - mLastScrollOffset);
                rotation(isClockwiseScrolling ? offset : -offset);
            }
            mLastScrollOffset = y;
            invalidate();
        } else if (mScroller.isFinished() && isAutoSelect) {
            mLastScrollOffset = 0;
            int childCount = getChildCount();
            if (childCount == 0 || (childCount == 1 && mCurrentBearingType == TYPE_VIEW)) {
                return;
            }
            if (isScrolled) {
                startFixingAnimation();
                isScrolled = false;
            }
        }
    }

    private int getTargetAngle() {
        int targetAngle;
        switch (mCurrentGravity) {
            case TOP:
                targetAngle = 90;
                break;
            case BOTTOM:
                targetAngle = 270;
                break;
            case LEFT_TOP:
                targetAngle = 45;
                break;
            case LEFT_BOTTOM:
            case RIGHT_TOP:
                targetAngle = 315;
                break;
            case RIGHT_BOTTOM:
                targetAngle = 45;
                break;
            case LEFT:
            case RIGHT:
            default:
                targetAngle = 0;
                break;
        }
        return targetAngle;
    }

    private void startFixingAnimation() {
        if (isBeingDragged) {
            return;
        }
        int targetAngle = getTargetAngle();
        final int hitPos = findClosestViewPos(targetAngle);
        float rotation = getChildAt(hitPos).getRotation();
        if (Math.abs(rotation - targetAngle) > 180) {
            targetAngle = 360 - targetAngle;
        }
        float angle = Math.abs(rotation - fixRotation(targetAngle));
        mAnimator = ValueAnimator.ofFloat(0, rotation > fixRotation(targetAngle) ? -angle : angle).setDuration(250);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue();
                if (mLastScrollOffset != 0) {
                    rotation(currentValue - mLastScrollOffset);
                }
                mLastScrollOffset = currentValue;
            }
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mLastScrollOffset = 0;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLastScrollOffset = 0;
                if (mOnItemSelectedListener != null) {
                    mOnItemSelectedListener.onSelected(getChildAt(hitPos));
                }
            }
        });
        LogUtil.print("start");
        mAnimator.start();
    }

    private int findClosestViewPos(float targetAngle) {
        int childCount = getChildCount();
        int startIndex = mCurrentBearingType == TYPE_VIEW && isBearingOnBottom ? 1 : 0;
        float temp = getChildAt(startIndex).getRotation();
        startIndex++;
        if (targetAngle == 0 && temp > 180) {
            temp = 360 - temp;
        }
        float hitRotation = Math.abs(targetAngle - temp);
        int hitPos = 0;

        for (int i = startIndex; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView == mBearingView) {
                continue;
            }
            temp = childView.getRotation();
            if (targetAngle == 0 && temp > 180) {
                temp = 360 - temp;
            }
            float rotation = Math.abs(targetAngle - temp);
            if (rotation < hitRotation) {
                hitPos = i;
                hitRotation = rotation;
            }
        }
        return hitPos;
    }

    /**
     * 检测手指是否顺时针滑动
     *
     * @param x 当前手指的x坐标
     * @param y 当前手指的y坐标
     * @return 是否顺时针
     */
    private boolean isClockwise(float x, float y) {
//        return Math.abs(y - mStartY) > Math.abs(x - mStartX) ? x < mPivotX != y > mStartY : y < mPivotY == x > mStartX;
        boolean isClockwise;
        //手势向下
        boolean isGestureDownward = y > mStartY;
        //手势向右
        boolean isGestureRightward = x > mStartX;
        //垂直滑动  上下滑动的幅度 > 左右滑动的幅度，则认为是垂直滑动，反之
        boolean isVerticalScroll = isShouldBeGetY = Math.abs(y - mStartY) > Math.abs(x - mStartX);

        if (isVerticalScroll) {
            //如果手指滑动的地方是在圆心左边的话：向下滑动就是逆时针，向上滑动则顺时针。反之，如果在圆心右边，向下滑动是顺时针，向上则逆时针。
            isClockwise = x < mPivotX != isGestureDownward;
        } else {
            //逻辑同上：手指滑动在圆心的上方：向右滑动就是顺时针，向左就是逆时针。反之，如果在圆心的下方，向左滑动是顺时针，向右是逆时针。
            isClockwise = y < mPivotY == isGestureRightward;
        }
        return isClockwise;
    }

    private void rotation(float rotation) {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view == mBearingView && mCurrentBearingType == TYPE_VIEW && !isBearingCanRoll) {
                continue;
            }
            view.setRotation(fixRotation(view.getRotation() + rotation));
        }
        if (mOnItemRotateListener != null) {
            mOnItemRotateListener.onRotate(rotation);
        }
    }

    private float fixRotation(float rotation) {
        //周角
        float angle = 360F;
        if (rotation < 0) {
            //将负的角度变成正的, 比如：-1 --> 359，在视觉上是一样的，这样我们内部处理起来会比较轻松
            rotation += angle;
        }
        //避免大于360度，即：362 --> 2
        if (rotation > angle) {
            rotation = rotation % angle;
        }
        return rotation;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if ((event.getAction() == MotionEvent.ACTION_MOVE && isBeingDragged) || super.onInterceptTouchEvent(event)) {
            return true;
        }
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                abortAnimation();
                mStartX = x;
                mStartY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float offsetX = x - mStartX;
                float offsetY = y - mStartY;
                //判断是否触发拖动事件
                if (Math.abs(offsetX) > mTouchSlop || Math.abs(offsetY) > mTouchSlop) {
                    mStartX = x;
                    mStartY = y;
                    isBeingDragged = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                isBeingDragged = false;
                break;
        }
        return isBeingDragged;
    }

    private void updateCircleCenterPoint() {
        int cx = 0, cy = 0;
        int totalWidth = getMeasuredWidth();
        int totalHeight = getMeasuredHeight();
        switch (mCurrentGravity) {
            case RIGHT:
                cx = totalWidth;
                cy = totalHeight / 2;
                cx -= mBearingOffset;
                break;
            case LEFT:
                cy = totalHeight / 2;
                cx += mBearingOffset;
                break;
            case BOTTOM:
                cy = totalHeight;
                cx = totalWidth / 2;
                cy -= mBearingOffset;
                break;
            case TOP:
                cx = totalWidth / 2;
                cy += mBearingOffset;
                break;
            case RIGHT_BOTTOM:
                cx = totalWidth;
                cy = totalHeight;
                cx -= mBearingOffset;
                cy -= mBearingOffset;
                break;
            case LEFT_BOTTOM:
                cy = totalHeight;
                cx += mBearingOffset;
                cy -= mBearingOffset;
                break;
            case RIGHT_TOP:
                cx = totalWidth;
                cx -= mBearingOffset;
                cy += mBearingOffset;
                break;
            case LEFT_TOP:
                cx = cy = mBearingOffset;
                break;
            default:
                break;
        }
        mPivotX = cx;
        mPivotY = cy;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int size;
        if (specMode == MeasureSpec.EXACTLY) {
            size = specSize;
        } else {
            int childMaxWidth = 0;
            for (int i = 0; i < getChildCount(); i++) {
                childMaxWidth = Math.max(childMaxWidth, getChildAt(i).getMeasuredWidth());
            }
            size = 2 * mRadius + mItemOffset + childMaxWidth;
        }
        setMeasuredDimension(size, size);
        if (mCurrentBearingType == TYPE_VIEW) {
            mRadius = Math.max(mBearingView.getMeasuredWidth(), mBearingView.getMeasuredHeight()) / 2;
        }
        updateCircleCenterPoint();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        abortAnimation();
        isScrolled = false;
        int startIndex = 0;
        if (mCurrentBearingType == TYPE_VIEW) {
            int width = mBearingView.getMeasuredWidth() / 2;
            int height = mBearingView.getMeasuredHeight() / 2;
            mBearingView.layout(mPivotX - width, mPivotY - height, mPivotX + width, mPivotY + height);
            mBearingView.setRotation(isBearingCanRoll ? mBearingView.getRotation() : 0);
            startIndex = 1;
        }
        int childCount = getChildCount();
        float angle = 360F / (childCount - startIndex);
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            if (view != mBearingView) {
                int height = view.getMeasuredHeight() / 2;
                int width = view.getMeasuredWidth();
                if (mCurrentGravity == RIGHT || mCurrentGravity == RIGHT_TOP || mCurrentGravity == RIGHT_BOTTOM) {
                    int baseLeft = mPivotX - mRadius - mItemOffset;
                    view.layout(baseLeft - width, mPivotY - height, baseLeft, mPivotY + height);
                    //更新旋转的中心点
                    view.setPivotX(width + mRadius + mItemOffset);
                    view.setPivotY(height);
                    view.setRotation(view.getRotation() + i * angle);
                } else {
                    int baseLeft = mPivotX + mRadius + mItemOffset;
                    view.layout(baseLeft, mPivotY - height, baseLeft + width, mPivotY + height);
                    //更新旋转的中心点
                    view.setPivotX(-mRadius - mItemOffset);
                    view.setPivotY(height);
                    view.setRotation(view.getRotation() + i * angle);
                }
            }
        }
        startFixingAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCurrentBearingType == TYPE_COLOR && isBearingOnBottom) {
            canvas.drawCircle(mPivotX, mPivotY, mRadius, mPaint);
        }
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
//        mPaint.setColor(Color.BLUE);
//        mPaint.setStyle(Paint.Style.FILL);
        if (mCurrentBearingType == TYPE_COLOR && !isBearingOnBottom) {
            canvas.drawCircle(mPivotX, mPivotY, mRadius, mPaint);
        }
//        canvas.drawLine(mPivotX, mPivotY, getWidth(), mPivotY, mPaint);
//        canvas.drawPoint(mPivotX, mPivotY, mPaint);

//        mPaint.setColor(Color.DKGRAY);
//        mPaint.setStyle(Paint.Style.STROKE);
//        mPaint.setStrokeWidth(10);
//        canvas.drawLine(mStartX, mStartY, mEndX, mEndY, mPaint);
//        canvas.drawLine(mPivotX, mPivotY, mEndX, mEndY, mPaint);
//        canvas.drawLine(mStartX, mStartY, mPivotX, mPivotY, mPaint);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (mCurrentBearingType == TYPE_VIEW && !isBearingOnBottom && getChildCount() > 0) {
            index = 0;
        }
        super.addView(child, index, params);
    }

    @Override
    public void removeViewAt(int index) {
        if (mCurrentBearingType == TYPE_VIEW && getChildAt(index) == mBearingView) {
            if (isBearingOnBottom) {
                return;
            } else {
                index--;
                if (getChildCount() - 1 < 1) {
                    return;
                }
            }
        }
        super.removeViewAt(index);
    }

    private void abortAnimation() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
    }

    public void setScrollAvailabilityRatio(@FloatRange(from = 0.0, to = 1.0) float ratio) {
        mScrollAvailabilityRatio = ratio;
    }

    public void setAutoSelect(boolean isAutoSelect) {
        if (this.isAutoSelect != isAutoSelect) {
            this.isAutoSelect = isAutoSelect;
            requestLayout();
        }
    }

    public void setBearingCanRoll(boolean isBearingCanRoll) {
        if (this.isBearingCanRoll != isBearingCanRoll) {
            this.isBearingCanRoll = isBearingCanRoll;
            requestLayout();
        }
    }

    public void setBearingOnBottom(boolean isBearingOnBottom) {
        if (this.isBearingOnBottom != isBearingOnBottom) {
            this.isBearingOnBottom = isBearingOnBottom;
            requestLayout();
        }
    }

    public void setRadius(int radius) {
        if (mRadius != radius) {
            mRadius = radius;
            if (mCurrentBearingType == TYPE_COLOR) {
                requestLayout();
            }
        }
    }

    public void setBearingLayoutId(@LayoutRes int layoutId) {
        mBearingLayoutId = layoutId;
    }

    public void setBearingOffset(int centerOffset) {
        if (mBearingOffset != centerOffset) {
            mBearingOffset = centerOffset;
            requestLayout();
        }
    }

    public void setItemOffset(int itemOffset) {
        if (mItemOffset != itemOffset) {
            mItemOffset = itemOffset;
            requestLayout();
        }
    }

    public void setGravity(@Gravity int gravity) {
        if (mCurrentGravity != gravity) {
            mCurrentGravity = gravity;
            requestLayout();
        }
    }

    public void setBearingColor(@ColorInt int color) {
        if (mPaint != null) {
            mPaint.setColor(mBearingColor = color);
            if (mCurrentBearingType == TYPE_COLOR) {
                invalidate();
            }
        }
    }

    public void setBearingType(@BearingType int type) {
        if (mCurrentBearingType != type) {
            mCurrentBearingType = type;
            if (mCurrentBearingType == TYPE_VIEW) {
                if (mBearingLayoutId == 0) {
                    throw new IllegalStateException("bearing layout not set!");
                } else {
                    mBearingView = LayoutInflater.from(getContext()).inflate(mBearingLayoutId, this, false);
                    addView(mBearingView);
                }
                setWillNotDraw(true);
            } else {
                if (mBearingView != null) {
                    removeView(mBearingView);
                    mBearingView = null;
                }
                if (mPaint == null) {
                    mPaint = new Paint();
                    mPaint.setAntiAlias(true);
                    mPaint.setColor(mBearingColor);
                }
                setWillNotDraw(false);
            }
            requestLayout();
        }
    }

    public boolean isBearingView(View view) {
        return view == mBearingView;
    }

    public int getGravity() {
        return mCurrentGravity;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public interface OnItemSelectedListener {
        void onSelected(View item);
    }

    public void setOnItemRotateListener(OnItemRotateListener listener) {
        mOnItemRotateListener = listener;
    }

    public interface OnItemRotateListener {
        void onRotate(float rotation);
    }
}
