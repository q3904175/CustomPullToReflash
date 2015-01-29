package android.jchun.com.pulltoreflash;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ImageView;

import java.security.InvalidParameterException;

/**
 * Created by JChun on 2015/1/20 0020.
 */
public class PullToRefreshView extends ViewGroup {
    private static int DRAG_MAX_DISTANCE = 100;//拖拽最大高度,单位dp
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private Interpolator mDecelerateInterpolator;//动画变化速率
    private int mTouchSlop; //用来判断为滑动的距离
    private int mTotalDragDistance;
    public static final int STYLE_SUN = 0; //太阳样式
    public static final int STYLE_JET = 1;
    public static final int STYLE_JD = 2;//京东样式
    private ImageView mRefreshView;//下拉显示的image
    private boolean mRefreshing; //是否刷新
    private boolean mNotify;//是否通知回调
    private View mTarget;//内容布局，如ListView
    private int mFrom;//动画起始位置
    private BaseRefreshView mBaseRefreshView;//刷新的图片drawable
    private int mCurrentOffsetTop;//当前位置
    private float mFromDragPercent;//动画起始百分比
    private float mCurrentDragPercent;//当前拖拽百分比
    public static final int MAX_OFFSET_ANIMATION_DURATION = 700;//最大动画持续时间
    private OnRefreshListener mListener;//刷新回调监听
    private int mActivePointerId;//当前触摸点id
    private boolean mIsBeingDragged;//是否开始拖拽
    private float mInitialMotionY;//当前初始触摸的Y坐标
    private static final int INVALID_POINTER = -1;//触摸取消
    private static final float DRAG_RATE = .5f;//拖拽比。手指移动2px，下拉1px

    public PullToRefreshView(Context context) {
        this(context, null);
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //获取下拉刷新的样式。
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefreshView);
        final int type = a.getInteger(R.styleable.RefreshView_type, STYLE_JD);
        a.recycle();
        //初始化加速曲线
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        //获取滑动的距离
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        //下拉总高度
        mTotalDragDistance = convertDpToPixel(DRAG_MAX_DISTANCE);
        //创建imageView
        mRefreshView = new ImageView(context);
        //设置刷新的图片类型
        setRefreshStyle(type);
        //添加刷新图片
        addView(mRefreshView);
        //使用自定义onDraw方法
        setWillNotDraw(false);
        //启用子元素排序
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        ensureTarget();//设置内容布局
        if (mTarget == null)
            return;

        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getPaddingRight();
        int bottom = getPaddingBottom();
        //设置内容布局位置
        mTarget.layout(left, top + mCurrentOffsetTop, left + width - right, top + height - bottom + mCurrentOffsetTop);
        mRefreshView.layout(left, top, left + width - right, top + height - bottom);
    }

    /**
     * dp转px
     * @param dp
     * @return
     */
    public int convertDpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    /**
     * 初始化并设置刷新图片的样式。
     * @param type
     */
    public void setRefreshStyle(int type) {
        //设置不是刷新
        setRefreshing(false);
        switch (type) {
            case STYLE_SUN:
                mBaseRefreshView = new SunRefreshView(getContext(), this);
                break;
            case STYLE_JET:
                // TODO
                break;
            case STYLE_JD:
                mBaseRefreshView = new JDRefreshView(getContext(), this);
                break;
            default:
                throw new InvalidParameterException("Type does not exist");
        }
        mRefreshView.setImageDrawable(mBaseRefreshView);//设置图片
    }

    /**
     * 设置刷新
     * @param refreshing
     */
    public void setRefreshing(boolean refreshing) {
        if (mRefreshing != refreshing) {
            setRefreshing(refreshing, false);
        }
    }

    /**
     * 设置刷新
     * @param refreshing
     * @param notify
     */
    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            //开始刷新
            if (mRefreshing) {
                //设置百分比
                mBaseRefreshView.setPercent(1f, true);
                //启动下拉动画
                animateOffsetToCorrectPosition();
            } else {
                //重置到起始位置
                animateOffsetToStartPosition();
            }
        }
    }

    /**
     * 设置内容布局
     */
    private void ensureTarget() {
        if (mTarget != null)
            return;
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                //不是刷新图片
                if (child != mRefreshView)
                    mTarget = child;
            }
        }
    }

    /**
     * 重置到起始位置
     */
    private void animateOffsetToStartPosition() {
        mFrom = mCurrentOffsetTop;//获取起始位置
        mFromDragPercent = mCurrentDragPercent;//获取起始百分比
        long animationDuration = Math.abs((long) (MAX_OFFSET_ANIMATION_DURATION * mFromDragPercent));//动画持续时间
        mAnimateToStartPosition.reset();//重置动画
        mAnimateToStartPosition.setDuration(animationDuration);//设置动画持续时间
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);//设置动画变化速率
        mAnimateToStartPosition.setAnimationListener(mToStartListener);//设置动画监听
        mRefreshView.clearAnimation();//清空动画
        mRefreshView.startAnimation(mAnimateToStartPosition);//设置动画
    }

    /**
     * 下拉动画
     */
    private void animateOffsetToCorrectPosition() {
        mFrom = mCurrentOffsetTop;//获取起始位置
        mFromDragPercent = mCurrentDragPercent;//获取起始百分比
        mAnimateToCorrectPosition.reset();//重置动画
        mAnimateToCorrectPosition.setDuration(MAX_OFFSET_ANIMATION_DURATION);//设置动画持续时间
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);//设置动画变化速率
        mRefreshView.clearAnimation();//清空动画
        mRefreshView.startAnimation(mAnimateToCorrectPosition);//设置动画
        //正在刷新
        if (mRefreshing) {
            mBaseRefreshView.start();//启动刷新动画
            if (mNotify) {
                if (mListener != null) {
                    mListener.onRefresh();//回调刷新监听
                }
            }
        } else {
            mBaseRefreshView.stop();//停止刷新
            animateOffsetToStartPosition();//重置到起始位置
        }
        mCurrentOffsetTop = mTarget.getTop();//设置当前位置
    }

    /**
     * 重置到起始位置动画
     */
    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };
    /**
     * 下拉动画
     */
    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop;
            int endTarget = mTotalDragDistance;
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            //设置偏移量
            int offset = targetTop - mTarget.getTop();
            //设置百分比
            mCurrentDragPercent = mFromDragPercent - (mFromDragPercent - 1.0f) * interpolatedTime;

            setTargetOffsetTop(offset, false);
        }
    };
    /**
     * 重置动画监听
     */
    private Animation.AnimationListener mToStartListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            //停止刷新动画
            mBaseRefreshView.stop();
            //重置当前位置
            mCurrentOffsetTop = mTarget.getTop();
        }
    };

    /**
     * 移动到起始位置
     * @param interpolatedTime 动画开始时为0，动画结束时为1，用于计算剩余高度和剩余百分比
     */
    private void moveToStart(float interpolatedTime) {
        int targetTop = mFrom - (int) (mFrom * interpolatedTime);//计算剩余高度
        float targetPercent = mFromDragPercent * (1.0f - interpolatedTime);//计算剩余百分比
        int offset = targetTop - mTarget.getTop();//计算已经完成的高度

        mCurrentDragPercent = targetPercent;//获取当前百分比
        mBaseRefreshView.setPercent(mCurrentDragPercent, true);//设置当前百分比
        setTargetOffsetTop(offset, false);//设置内容布局的偏移距离
    }

    /**
     * 设置内容布局的偏移距离
     * @param offset
     * @param requiresUpdate
     */
    private void setTargetOffsetTop(int offset, boolean requiresUpdate) {
        mTarget.offsetTopAndBottom(offset);//设置当前偏移量
        mBaseRefreshView.offsetTopAndBottom(offset);//设置刷新图片偏移量
        mCurrentOffsetTop = mTarget.getTop();//获取当前偏移量
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();//重绘
        }
    }

    /**
     * 获取总拖拽高度
     * @return
     */
    public int getTotalDragDistance() {
        return mTotalDragDistance;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ensureTarget();//设置内容布局
        if (mTarget == null)
            return;

        widthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingRight() - getPaddingLeft(), MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
        mTarget.measure(widthMeasureSpec, heightMeasureSpec);
        mRefreshView.measure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * onInterceptTouchEvent是在ViewGroup里面定义的。
     * Android中的layout布局类一般都是继承此类的。
     * onInterceptTouchEvent是用于拦截手势事件的，每个手势事件都会先调用onInterceptTouchEvent
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!isEnabled() || canChildScrollUp() || mRefreshing) {
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //重置到初始位置
                setTargetOffsetTop(0, true);
                //获取当前触摸点id
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                //重置为不可拖拽
                mIsBeingDragged = false;
                //获取触摸的Y坐标
                final float initialMotionY = getMotionEventY(ev, mActivePointerId);
                if (initialMotionY == -1) {
                    return false;
                }
                //设置初始Y坐标
                mInitialMotionY = initialMotionY;
                break;
            case MotionEvent.ACTION_MOVE:
                //触摸取消
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                //获取当前触摸的y坐标
                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                //获取拖拽距离
                final float yDiff = y - mInitialMotionY;
                //如果可以拖拽
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //取消拖拽
                mIsBeingDragged = false;
                //设置不可拖拽
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                //当前手指松开，则重新选择触摸id
                onSecondaryPointerUp(ev);
                break;
        }

        return mIsBeingDragged;
    }

    /**
     * onTouchEvent同样也是在view中定义的一个方法。处理传递到view 的手势事件。
     * 手势事件类型包括ACTION_DOWN,ACTION_MOVE,ACTION_UP,ACTION_CANCEL等事件。
     * 其中Layout里的onInterceptTouchEvent默认返回值是false,这样touch事件会传递到View控件，
     * Layout里的onTouch默认返回值是false, View里的onTouch默认返回值是true,当我们手指点击屏幕时候，
     * 先调用ACTION_DOWN事件,当onTouch里返回值是true的时候，onTouch回继续调用ACTION_UP事件，如果onTouch里返回值是false，
     * 那么onTouch只会调用ACTION_DOWN而不调用ACTION_UP.
     * @param ev
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (!mIsBeingDragged) {
            return super.onTouchEvent(ev);
        }

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                //获取当前手指触摸index
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                //获取当前触摸y坐标
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                //获取偏移量
                final float yDiff = y - mInitialMotionY;
                //获取下拉高度
                final float scrollTop = yDiff * DRAG_RATE;
                //获取当前拖拽百分比
                mCurrentDragPercent = scrollTop / mTotalDragDistance;
                if (mCurrentDragPercent < 0) {
                    return false;
                }
                float boundedDragPercent = Math.min(1f, Math.abs(mCurrentDragPercent));
                float extraOS = Math.abs(scrollTop) - mTotalDragDistance;
                float slingshotDist = mTotalDragDistance;
                float tensionSlingshotPercent = Math.max(0,
                        Math.min(extraOS, slingshotDist * 2) / slingshotDist);
                float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                        (tensionSlingshotPercent / 4), 2)) * 2f;
                float extraMove = (slingshotDist) * tensionPercent / 2;
                int targetY = (int) ((slingshotDist * boundedDragPercent) + extraMove);
                //设置百分比
                mBaseRefreshView.setPercent(mCurrentDragPercent, true);
                //设置偏移高度
                setTargetOffsetTop(targetY - mCurrentOffsetTop, true);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN:
                //新手指触摸，重置当前手指触摸id
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                //当前手指松开，则重新选择触摸id
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                //获取当前手指触摸index
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                //获取当前触摸y坐标
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                //获取拖拽的高度
                final float overScrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                //如果拖拽的距离大于总拖拽距离
                if (overScrollTop > mTotalDragDistance) {
                    //启动刷新
                    setRefreshing(true, true);
                } else {
                    //重置初始刷新
                    mRefreshing = false;
                    //重置到起始位置
                    animateOffsetToStartPosition();
                }
                //设置手指无效
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    /**
     * 是否可以垂直滚动
     * @return
     */
    private boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    /**
     * 获取Y坐标
     * @param ev
     * @param activePointerId
     * @return
     */
    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    /**
     * 重新设置当前手指触摸id
     * @param ev
     */
    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }
    public static interface OnRefreshListener {
        public void onRefresh();
    }

}
