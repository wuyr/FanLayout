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
 * GitHub: https://github.com/wuyr/FanLayout
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

    @IntDef({MODE_AVERAGE, MODE_FIXED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LayoutMode {
    }

    public static final int MODE_AVERAGE = 0, MODE_FIXED = 1;

    @IntDef({ADD_DIRECTION_CLOCKWISE, ADD_DIRECTION_COUNTERCLOCKWISE, ADD_DIRECTION_INTERLACED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DirectionMode {
    }

    public static final int ADD_DIRECTION_CLOCKWISE = 0, ADD_DIRECTION_COUNTERCLOCKWISE = 1, ADD_DIRECTION_INTERLACED = 2;
    private int mFixingAnimationDuration = 300;
    private int mRadius;
    private int mBearingOffset;
    private int mItemOffset;
    private int mItemLayoutMode;
    private int mItemAddDirection;
    private float mItemAngleOffset;
    private int mCurrentGravity;
    private int mCurrentSelectionIndex;
    private int mPivotX, mPivotY;
    private float mStartX, mStartY;
    private boolean isItemDirectionFixed;
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
    private boolean isSmoothSelection;
    private volatile boolean isOnLayout;
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
        isItemDirectionFixed = a.getBoolean(R.styleable.FanLayout_item_direction_fixed, false);
        mCurrentBearingType = a.getInteger(R.styleable.FanLayout_bearing_type, TYPE_COLOR);
        mBearingColor = a.getColor(R.styleable.FanLayout_bearing_color, Color.BLACK);
        if (isViewType()) {
            mBearingLayoutId = a.getResourceId(R.styleable.FanLayout_bearing_layout, 0);
            if (mBearingLayoutId == 0) {
                throw new IllegalStateException("bearing layout not set!");
            } else {
                mBearingView = LayoutInflater.from(context).inflate(mBearingLayoutId, this, false);
                addView(mBearingView);
                mCurrentSelectionIndex = 1;
            }
        } else {
            mRadius = a.getDimensionPixelSize(R.styleable.FanLayout_bearing_radius, 0);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(mBearingColor);
            setWillNotDraw(false);
        }
        mItemAddDirection = a.getInteger(R.styleable.FanLayout_item_add_direction, ADD_DIRECTION_CLOCKWISE);
        if ((mItemLayoutMode = a.getInteger(R.styleable.FanLayout_item_layout_mode, MODE_AVERAGE)) == MODE_FIXED) {
            mItemAngleOffset = a.getFloat(R.styleable.FanLayout_item_angle_offset, 0);
            if (mItemAngleOffset <= 0 || mItemAngleOffset > 360) {
                throw new IllegalStateException("item_angle_offset must be between 1~360!");
            }
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
                rotation((isClockwiseScrolling = isClockwise(x, y)) ? angle : -angle);
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
        } else if (mScroller.isFinished()) {
            mLastScrollOffset = 0;
            if (isScrolled && isAutoSelect) {
                playFixingAnimation();
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

    private void playFixingAnimation() {
        int childCount = getChildCount();
        if (isBeingDragged || childCount == 0 || (childCount == 1 && isViewType())) {
            return;
        }
        int targetAngle = getTargetAngle();
        mCurrentSelectionIndex = findClosestViewPos(targetAngle);
        float rotation = getChildAt(mCurrentSelectionIndex).getRotation();
        if (Math.abs(rotation - targetAngle) > 180) {
            targetAngle = 360 - targetAngle;
        }
        float angle = Math.abs(rotation - fixRotation(targetAngle));
        startValueAnimator(rotation > fixRotation(targetAngle) ? -angle : angle);
    }

    private void startValueAnimator(float end) {
        mAnimator = ValueAnimator.ofFloat(0, end).setDuration(mFixingAnimationDuration);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue();
                if (mLastScrollOffset != 0) {
                    if (isOnLayout) {
                        mAnimator.cancel();
                        return;
                    }
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
                    mOnItemSelectedListener.onSelected(getChildAt(mCurrentSelectionIndex));
                }
            }
        });
        mAnimator.start();
    }

    private int findClosestViewPos(float targetAngle) {
        int childCount = getChildCount();
        int startIndex = isViewType() && isBearingOnBottom ? 1 : 0;
        float temp = getChildAt(startIndex).getRotation();
        if (targetAngle == 0 && temp > 180) {
            temp = 360 - temp;
        }
        float hitRotation = Math.abs(targetAngle - temp);
        int hitPos = startIndex;

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
        if (!isEnabled()) {
            return false;
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
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(size, MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ? height : size);
        if (isViewType()) {
            mRadius = Math.max(mBearingView.getMeasuredWidth(), mBearingView.getMeasuredHeight()) / 2;
        }
        updateCircleCenterPoint();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        isOnLayout = true;
        abortAnimation();
        isScrolled = false;
        boolean isHasBottomBearing = isViewType() && isBearingOnBottom;
        int startIndex = 0;
        if (isViewType()) {
            int width = mBearingView.getMeasuredWidth() / 2;
            int height = mBearingView.getMeasuredHeight() / 2;
            mBearingView.layout(mPivotX - width, mPivotY - height, mPivotX + width, mPivotY + height);
            mBearingView.setRotation(0);
            startIndex = 1;
        }
        int childCount = getChildCount();
        float angle = mItemLayoutMode == MODE_AVERAGE ? 360F / (childCount - startIndex) : mItemAngleOffset;
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            if (view == mBearingView) {
                continue;
            }
            int height = view.getMeasuredHeight() / 2;
            int width = view.getMeasuredWidth();
            if (mCurrentGravity == RIGHT || mCurrentGravity == RIGHT_TOP || mCurrentGravity == RIGHT_BOTTOM) {
                int baseLeft = mPivotX - mRadius - mItemOffset;
                view.layout(baseLeft - width, mPivotY - height, baseLeft, mPivotY + height);
                //更新旋转的中心点
                view.setPivotX(width + mRadius + mItemOffset);
            } else {
                int baseLeft = mPivotX + mRadius + mItemOffset;
                view.layout(baseLeft, mPivotY - height, baseLeft + width, mPivotY + height);
                //更新旋转的中心点
                view.setPivotX(-mRadius - mItemOffset);
            }
            view.setPivotY(height);
            int index = isHasBottomBearing ? i - 1 : i;
            float rotation;
            if (mItemAddDirection == ADD_DIRECTION_COUNTERCLOCKWISE) {
                rotation = 360F - index * angle;
            } else if (mItemAddDirection == ADD_DIRECTION_INTERLACED) {
                int hitCount = 0;
                boolean isDual = index % 2 == 0;
                for (int j = 0; j < index; j++) {
                    if (isDual) {
                        if (j % 2 == 0) {
                            hitCount++;
                        }
                    } else {
                        if (j % 2 != 0) {
                            hitCount++;
                        }
                    }
                }
                rotation = isDual ? 360F - hitCount * angle : hitCount * angle;
            } else {
                rotation = index * angle;
            }
            view.setRotation(fixRotation(rotation));
        }
        isOnLayout = false;
        if (isAutoSelect && childCount > (isViewType() ? 1 : 0)) {
            View view = getChildAt(mCurrentSelectionIndex);
            if (view == null) {
                view = getChildAt(0);
            }
            if (view != null) {
                angle = view.getRotation();
                float rotation = getTargetAngle() - angle;
                if (!isSmoothSelection) {
                    isSmoothSelection = false;
                    startValueAnimator(rotation);
                } else {
                    rotation(rotation);
                    if (mOnItemSelectedListener != null) {
                        mOnItemSelectedListener.onSelected(getChildAt(mCurrentSelectionIndex));
                    }
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isViewType() && isBearingOnBottom) {
            canvas.drawCircle(mPivotX, mPivotY, mRadius, mPaint);
        }
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        if (!isViewType() && !isBearingOnBottom) {
            canvas.drawCircle(mPivotX, mPivotY, mRadius, mPaint);
        }
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (isViewType() && !isBearingOnBottom && getChildCount() > 0 && child != mBearingView) {
            index = 0;
        }
        super.addView(child, index, params);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        mCurrentSelectionIndex = isViewType() && isBearingOnBottom ? 1 : 0;
        super.removeViewsInLayout(start, count);
    }

    @Override
    public void removeViews(int start, int count) {
        mCurrentSelectionIndex = isViewType() && isBearingOnBottom ? 1 : 0;
        super.removeViews(start, count);
    }

    @Override
    public void removeView(View view) {
        handleRemoveView(view);
        super.removeView(view);
    }

    @Override
    public void removeViewInLayout(View view) {
        handleRemoveView(view);
        super.removeViewInLayout(view);
    }

    private void handleRemoveView(View view) {
        int index = indexOfChild(view);
        if (index > -1) {
            if (index <= mCurrentSelectionIndex) {
                mCurrentSelectionIndex--;
            }
        }
    }


    @Override
    public void removeViewAt(int index) {
        if (index < 0) {
            return;
        }
        if (index <= mCurrentSelectionIndex) {
            mCurrentSelectionIndex--;
        }
        if (isViewType() && getChildAt(index) == mBearingView) {
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
        mLastScrollOffset = 0;
    }

    private void setItemChildViewRotation(boolean isUseRotation) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (!isBearingView(v) && v instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) v;
                for (int j = 0; j < viewGroup.getChildCount(); j++) {
                    View child = viewGroup.getChildAt(j);
                    child.setRotation(isUseRotation ? 0 : -viewGroup.getRotation());
                }
            }
        }
    }

    public void setScrollAvailabilityRatio(@FloatRange(from = 0.0, to = 1.0) float ratio) {
        mScrollAvailabilityRatio = ratio;
    }

    public void setAutoSelect(boolean isAutoSelect) {
        if (this.isAutoSelect != isAutoSelect) {
            this.isAutoSelect = isAutoSelect;
            mCurrentSelectionIndex = isBearingOnBottom ? 1 : 0;
            requestLayout();
        }
    }

    public void setItemDirectionFixed(boolean isFixed) {
        if (isItemDirectionFixed != isFixed) {
            isItemDirectionFixed = isFixed;
            setItemChildViewRotation(!isFixed);
        }
    }

    public boolean isItemDirectionFixed() {
        return isItemDirectionFixed;
    }

    public void setBearingCanRoll(boolean isBearingCanRoll) {
        if (this.isBearingCanRoll != isBearingCanRoll) {
            this.isBearingCanRoll = isBearingCanRoll;
            requestLayout();
        }
    }

    public void setSelection(int index, boolean isSmooth) {
        if (isAutoSelect && mCurrentSelectionIndex != index && getChildCount() > (isViewType() ? 1 : 0)) {
            if (isViewType()) {
                if (isBearingOnBottom) {
                    index++;
                } else {
                    if (index == getChildCount() - 1) {
                        index--;
                    }
                }
                if (index < 0) {
                    index = 0;
                }
                if (index >= getChildCount()) {
                    index = getChildCount() - 1;
                }
            }
            mCurrentSelectionIndex = index;
            isSmoothSelection = isSmooth;
            requestLayout();
        }
    }

    public boolean isBearingView(View view) {
        return view == mBearingView;
    }

    public void setBearingOnBottom(boolean isBearingOnBottom) {
        if (this.isBearingOnBottom != isBearingOnBottom) {
            this.isBearingOnBottom = isBearingOnBottom;
            if (isViewType()) {
                if (mBearingView != null) {
                    removeView(mBearingView);
                    addView(mBearingView, isBearingOnBottom ? 0 : -1);
                }
            } else {
                invalidate();
            }
        }
    }

    private boolean isViewType() {
        return mCurrentBearingType == TYPE_VIEW;
    }

    public void setRadius(int radius) {
        if (mRadius != radius) {
            mRadius = radius;
            if (!isViewType()) {
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
            if (!isViewType()) {
                invalidate();
            }
        }
    }

    public void rotation(float rotation) {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view == mBearingView && isViewType() && !isBearingCanRoll) {
                continue;
            }
            view.setRotation(fixRotation(view.getRotation() + rotation));
        }
        if (isItemDirectionFixed) {
            setItemChildViewRotation(false);
        }
        if (mOnItemRotateListener != null) {
            mOnItemRotateListener.onRotate(rotation);
        }
    }

    public void setBearingType(@BearingType int type) {
        if (mCurrentBearingType != type) {
            mCurrentBearingType = type;
            if (isViewType()) {
                if (mBearingLayoutId == 0) {
                    throw new IllegalStateException("bearing layout not set!");
                } else {
                    mBearingView = LayoutInflater.from(getContext()).inflate(mBearingLayoutId, this, false);
                    addView(mBearingView, isBearingOnBottom ? 0 : -1);
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

    public void setItemLayoutMode(@LayoutMode int layoutMode) {
        if (mItemLayoutMode != layoutMode) {
            mItemLayoutMode = layoutMode;
            requestLayout();
        }
    }

    public void setItemAddDirection(@DirectionMode int direction) {
        if (mItemAddDirection != direction) {
            mItemAddDirection = direction;
            requestLayout();
        }
    }

    public void setItemAngleOffset(float angle) {
        if (mItemAngleOffset != angle) {
            mItemAngleOffset = angle;
            requestLayout();
        }
    }

    public void setFixingAnimationDuration(int duration) {
        mFixingAnimationDuration = duration;
    }

    public int getBearingType() {
        return mCurrentBearingType;
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
