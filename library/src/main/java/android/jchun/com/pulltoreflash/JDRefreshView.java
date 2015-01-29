package android.jchun.com.pulltoreflash;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

/**
 * Created by JChun on 2015/1/28 0028.
 */

public class JDRefreshView extends BaseRefreshView {
    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Matrix mGoodsMatrix;
    private Animation mAnimation;
    private boolean isRefreshing = false;
    private Bitmap mJDGoods;
    private Bitmap mJDPeople_0;
    private Bitmap mJDPeople_1;
    private Bitmap mJDPeople_2;
    private Bitmap mJDPeople_3;
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final int ANIMATION_DURATION = 180;
    private float mRun = 0.0f;
    private float mPercent = 0.0f;
    private int mScreenWidth;
    private int offsetWidth = 20;


    public JDRefreshView(Context layout, PullToRefreshView parent) {
        super(parent);
        mParent = parent;
        mMatrix = new Matrix();
        mGoodsMatrix  = new Matrix();
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        offsetWidth = convertDpToPixel(offsetWidth);
        createBitmaps();
        setupAnimations();
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
        if (invalidate) setRun(percent);
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    @Override
    public void offsetTopAndBottom(int offset) {

    }

    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    public void createBitmaps() {
        mJDGoods = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.app_refresh_goods_0);
        mJDGoods = Bitmap.createScaledBitmap(mJDGoods, mJDGoods.getWidth(), mJDGoods.getHeight(), true);
        mJDPeople_0 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.app_refresh_people_0);
        mJDPeople_0 = Bitmap.createScaledBitmap(mJDPeople_0, mJDPeople_0.getWidth(), mJDPeople_0.getHeight(), true);
        mJDPeople_1 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.app_refresh_people_1);
        mJDPeople_1 = Bitmap.createScaledBitmap(mJDPeople_1, mJDPeople_1.getWidth(), mJDPeople_1.getHeight(), true);
        mJDPeople_2 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.app_refresh_people_2);
        mJDPeople_2 = Bitmap.createScaledBitmap(mJDPeople_2, mJDPeople_2.getWidth(), mJDPeople_2.getHeight(), true);
        mJDPeople_3 = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.app_refresh_people_3);
        mJDPeople_3 = Bitmap.createScaledBitmap(mJDPeople_3, mJDPeople_3.getWidth(), mJDPeople_3.getHeight(), true);
    }

    @Override
    public void draw(Canvas canvas) {
        drawJDPeople(canvas);
    }

    private void drawJDPeople(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();
        Matrix goodsMatrix = mGoodsMatrix;
        goodsMatrix.reset();
        matrix.postTranslate(mScreenWidth*1/3-mJDPeople_0.getWidth(), 0);
        float peopleScale = Math.min(1f, Math.abs(mPercent));
        if (!isRefreshing) {
            matrix.postScale(peopleScale, peopleScale,mScreenWidth*1/3-mJDPeople_0.getWidth()-offsetWidth, 0);
            goodsMatrix.postTranslate(mScreenWidth*1/3-offsetWidth, mParent.getTotalDragDistance()/2);
            goodsMatrix.postScale(peopleScale, peopleScale,mScreenWidth*1/3-offsetWidth, mParent.getTotalDragDistance()/2);
            canvas.drawBitmap(mJDPeople_0, matrix, null);
            canvas.drawBitmap(mJDGoods, goodsMatrix, null);
        } else {
            if (mRun < 0.333333) {
                canvas.drawBitmap(mJDPeople_1, matrix, null);
            } else if (mRun < 0.666666) {
                canvas.drawBitmap(mJDPeople_2, matrix, null);
            } else {
                canvas.drawBitmap(mJDPeople_3, matrix, null);
            }
        }
    }

    public void setRun(float run) {
        mRun = run;
        invalidateSelf();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setRun(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);//无限循环
        mAnimation.setRepeatMode(Animation.RESTART);//循环模式:重新开始
        mAnimation.setInterpolator(LINEAR_INTERPOLATOR);//线性速率
        mAnimation.setDuration(ANIMATION_DURATION);//动画持续时间
    }
}
