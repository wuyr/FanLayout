package com.wuyr.fanlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
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
    private int mRadius;
    private int mCenterOffset;
    private int mItemOffset;
    private int mCurrentGravity;
    private int mPivotX, mPivotY;
    private float mStartX, mStartY;
    private Paint mPaint;
    private Scroller mScroller;//平滑滚动辅助
    private VelocityTracker mVelocityTracker;//手指滑动速率搜集
    private boolean isClockwiseScrolling;
    private boolean isShouldBeGetY;
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

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FanLayout, defStyleAttr, 0);
        mRadius = a.getDimensionPixelSize(R.styleable.FanLayout_bearing_radius, 0);
        int paintColor = a.getColor(R.styleable.FanLayout_primary_color, Color.BLACK);
        mCenterOffset = a.getDimensionPixelSize(R.styleable.FanLayout_center_offset, 0);
        mItemOffset = a.getDimensionPixelSize(R.styleable.FanLayout_item_offset, 0);
        mCurrentGravity = a.getInteger(R.styleable.FanLayout_center_gravity, LEFT);
        a.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(paintColor);
        setWillNotDraw(false);

        mScroller = new Scroller(getContext());
        mVelocityTracker = VelocityTracker.obtain();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        mVelocityTracker.addMovement(event);
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
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
//                    LogUtil.printf("lineA = %s lineB = %s hypotenuse = %s", lineA, lineB, hypotenuse);
//                    LogUtil.printf("lineA^2 + lineB^2 + hypotenuse^2 = %s + %s + %s = %s", Math.pow(lineA, 2), Math.pow(lineB, 2), Math.pow(hypotenuse, 2), (Math.pow(lineA, 2) + Math.pow(lineB, 2) - Math.pow(hypotenuse, 2)));
//                    LogUtil.printf(" / 2 * lineA * lineB = 2 * %s * %s = %s", lineA, lineB, 2 * lineA * lineB);
//                    LogUtil.printf(" lineA^2 + lineB^2 + hypotenuse^2/ 2 * lineA * lineB =%s / %s = %s", (Math.pow(lineA, 2) + Math.pow(lineB, 2) - Math.pow(hypotenuse, 2)), 2 * lineA * lineB, ((Math.pow(lineA, 2) + Math.pow(lineB, 2) - Math.pow(hypotenuse, 2)) / (2 * lineA * lineB)));
//                    LogUtil.printf("cos = %s", Math.cos(((Math.pow(lineA, 2) + Math.pow(lineB, 2) - Math.pow(hypotenuse, 2)) / (2 * lineA * lineB))));
//                    LogUtil.printf("arcos = %s", Math.acos(((Math.pow(lineA, 2) + Math.pow(lineB, 2) - Math.pow(hypotenuse, 2)) / (2 * lineA * lineB))));
//                    LogUtil.print(angle);
                    if (!Float.isNaN(angle)) {
                        isClockwiseScrolling = isClockwise(x, y);
                        rotation(isClockwiseScrolling ? angle : -angle);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
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

    private float mLastScrollOffset;
    private boolean isFixingPosition;

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            float y = ((isShouldBeGetY ? mScroller.getCurrY() : mScroller.getCurrX()) * .3F);
            if (mLastScrollOffset != 0) {
                float offset = Math.abs(y - mLastScrollOffset);
                rotation(isClockwiseScrolling ? offset : -offset);
            }
            mLastScrollOffset = y;
            invalidate();
        } else if (mScroller.isFinished()) {
            mLastScrollOffset = 0;
            int childCount = getChildCount();
            if (childCount == 0) {
                return;
            }
            int targetAngle;
            switch (mCurrentGravity) {
                case RIGHT:
                    targetAngle = 180;
                    break;
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
                    targetAngle = 315;
                    break;
                case RIGHT_TOP:
                    targetAngle = 135;
                    break;
                case RIGHT_BOTTOM:
                    targetAngle = 225;
                    break;
                case LEFT:
                default:
                    targetAngle = 0;
                    break;
            }
            final int hitPos = findClosestViewPos(targetAngle);
            float rotation = getChildAt(hitPos).getRotation();
            if (Math.abs(rotation - targetAngle) > 180) {
                targetAngle = 360 - targetAngle;
            }
            float angle = Math.abs(rotation - fixRotation(targetAngle));
            ValueAnimator animator = ValueAnimator.ofFloat(0, rotation > fixRotation(targetAngle) ? -angle : angle).setDuration(250);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float currentValue = (float) animation.getAnimatedValue();
                    if (mLastScrollOffset != 0) {
                        rotation(currentValue - mLastScrollOffset);
                    }
                    mLastScrollOffset = currentValue;
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLastScrollOffset = 0;
                    if (mOnItemSelectedListener != null) {
                        mOnItemSelectedListener.onSelected(getChildAt(hitPos));
                    }
                }
            });
            animator.start();
        }
    }

    private int findClosestViewPos(float targetAngle) {
        int childCount = getChildCount();
        float temp = getChildAt(0).getRotation();
        if (targetAngle == 0 && temp > 180) {
            temp = 360 - temp;
        }
        float hitRotation = Math.abs(targetAngle - temp);
        int hitPos = 0;

        for (int i = 1; i < childCount; i++) {
            temp = getChildAt(i).getRotation();
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
            view.setRotation(fixRotation(view.getRotation() + rotation));
//            LogUtil.print(view.getRotation());
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    private void updateCircleCenterPoint() {
        int cx = 0, cy = 0;
        int totalWidth = getWidth();
        int totalHeight = getHeight();
        switch (mCurrentGravity) {
            case RIGHT:
                cx = totalWidth;
                cy = totalHeight / 2;
                cx -= mCenterOffset;
                break;
            case LEFT:
                cy = totalHeight / 2;
                cx += mCenterOffset;
                break;
            case BOTTOM:
                cy = totalHeight;
                cx = totalWidth / 2;
                cy -= mCenterOffset;
                break;
            case TOP:
                cx = totalWidth / 2;
                cy += mCenterOffset;
                break;
            case RIGHT_BOTTOM:
                cx = totalWidth;
                cy = totalHeight;
                cx -= mCenterOffset;
                cy -= mCenterOffset;
                break;
            case LEFT_BOTTOM:
                cy = totalHeight;
                cx += mCenterOffset;
                cy -= mCenterOffset;
                break;
            case RIGHT_TOP:
                cx = totalWidth;
                cx -= mCenterOffset;
                cy += mCenterOffset;
                break;
            case LEFT_TOP:
                cx = cy = mCenterOffset;
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
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int baseLeft = mPivotX + mRadius + mItemOffset;
        int childCount = getChildCount();
        float angle = 360F / childCount;
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            int height = view.getMeasuredHeight() / 2;
            //更新旋转的中心点
            view.setPivotX(-mRadius - mItemOffset);
            view.setPivotY(height);
            view.layout(baseLeft, mPivotY - height, baseLeft + view.getMeasuredWidth(), mPivotY + height);
            view.setRotation(i * angle);
        }

    }

    @Override
    public void onDrawForeground(Canvas canvas) {
//        mPaint.setColor(Color.BLUE);
//        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(mPivotX, mPivotY, mRadius, mPaint);
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
    public void requestLayout() {
        updateCircleCenterPoint();
        super.requestLayout();
    }

    public void setRadius(int radius) {
        if (mRadius != radius) {
            mRadius = radius;
            requestLayout();
        }
    }

    public void setCenterOffset(int centerOffset) {
        if (mCenterOffset != centerOffset) {
            mCenterOffset = centerOffset;
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

    public static void main(String[] args) {
        System.out.println(Math.sin(310));
        System.out.println(Math.sin(50));
        System.out.println(186 % 90);
    }
}
