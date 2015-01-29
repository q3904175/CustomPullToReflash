package android.jchun.com.pulltoreflash;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

/**
 * Created by JChun on 2015/1/21 0021.
 * 刷新图片基类
 */
public abstract class BaseRefreshView extends Drawable implements Animatable {
    private PullToRefreshView mRefreshLayout;

    public BaseRefreshView(PullToRefreshView layout) {
        mRefreshLayout = layout;
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public Context getContext(){
        return mRefreshLayout != null ? mRefreshLayout.getContext() : null;
    }

    /**
     * 获取刷新布局
     * @return
     */
    public PullToRefreshView getRefreshLayout(){
        return mRefreshLayout;
    }

    /**
     * 设置百分比
     * @param percent 当前百分比
     * @param invalidate //是否启用
     */
    public abstract void setPercent(float percent, boolean invalidate);

    /**
     * 设置垂直方向的偏移量
     * @param offset
     */
    public abstract void offsetTopAndBottom(int offset);

    /**
     * dp转px
     * @param dp
     * @return
     */
    public int convertDpToPixel(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
