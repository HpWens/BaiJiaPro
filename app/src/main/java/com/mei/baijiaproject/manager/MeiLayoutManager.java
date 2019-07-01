package com.mei.baijiaproject.manager;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/6/26
 */
public class MeiLayoutManager extends RecyclerView.LayoutManager {

    public static final String TAG = "MeiLayoutManager";

    /**
     * 堆叠方向在左
     *
     * @return
     */
    public static final int FOCUS_LEFT = 1;

    /**
     * 堆叠方向在右
     *
     * @return
     */
    public static final int FOCUS_RIGHT = 2;

    public static final int FOCUS_TOP = 3;

    public static final int FOCUS_BOTTOM = 4;

    /**
     * 最大可堆叠层级
     */
    private int maxLayerCount = 3;

    /**
     * 堆叠方向
     */
    @FocusOrientation
    private int focusOrientation = FOCUS_LEFT;

    /**
     * 堆叠view之间的偏移量
     */
    private float layerPadding = 30;

    /**
     * 普通view之间的margin
     */
    private float normalViewGap = 30;

    /**
     * 是否自动选中
     */
    private boolean isAutoSelect = true;

    /**
     * 水平方向累计偏移量
     */
    private long mHorizontalOffset;

    /**
     * 垂直方向累计偏移量
     */
    private long mVerticalOffset;

    /**
     * 屏幕可见第一个view的position
     */
    private int mFirstVisiPos;

    /**
     * 屏幕可见的最后一个view的position
     */
    private int mLastVisiPos;

    /**
     * 一次完整的聚焦滑动所需要的移动距离
     */
    private float onceCompleteScrollLength = -1;

    /**
     * 焦点view的position
     */
    private int focusdPosition = -1;

    /**
     * 自动选中动画
     */
    private ValueAnimator selectAnimator;
    private long autoSelectMinDuration = 100;
    private long autoSelectMaxDuration = 300;

    @IntDef({FOCUS_LEFT, FOCUS_RIGHT, FOCUS_TOP, FOCUS_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusOrientation {
    }

    public static float dp2px(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    public MeiLayoutManager(Context context) {
        layerPadding = normalViewGap = dp2px(context, 16);
    }

    @Override
    public void onMeasure(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state, int widthSpec, int heightSpec) {
        // 测量布局
        super.onMeasure(recycler, state, widthSpec, heightSpec);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // super.onLayoutChildren(recycler, state);
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }

        onceCompleteScrollLength = -1;

        // 分离全部已有的view 放入临时缓存  mAttachedScrap 集合中
        detachAndScrapAttachedViews(recycler);

        fill(recycler, state, 0);
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 手指从右向左滑动，dx > 0; 手指从左向右滑动，dx < 0;
        // 位移0、没有子View 当然不移动
        if (dx == 0 || getChildCount() == 0) {
            return 0;
        }

        mHorizontalOffset += dx;

        dx = fill(recycler, state, dx);

        return dx;
    }

    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        // dx 表示消费的偏移量
        int resultDelta = dx;
        resultDelta = fillHorizontalLeft(recycler, state, dx);
        recycleChildren(recycler);
        return resultDelta;
    }

    /**
     * @param recycler
     * @param state
     * @param dx       偏移量。手指从右向左滑动，dx > 0; 手指从左向右滑动，dx < 0;
     * @return
     */
    private int fillHorizontalLeft(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        //----------------1、边界检测-----------------
        if (dx < 0) {
            // 已到达左边界
            if (mHorizontalOffset < 0) {
                mHorizontalOffset = dx = 0;
            }
        }

        if (dx > 0) {
            // 判定到达右边界 滑动到只剩下堆叠view，没有普通view了，说明已经到达右边界了
            if (mLastVisiPos - mFirstVisiPos <= maxLayerCount - 1) {
                // 因为在因为scrollHorizontallyBy里加了一次dx，现在减回去
                mHorizontalOffset -= dx;
                dx = 0;
            }
        }

        // 分离全部的view，加入到临时缓存
        detachAndScrapAttachedViews(recycler);

        //----------------2、初始化布局数据-----------------
        float startX = getPaddingLeft() - layerPadding;

        View tempView = null;
        int tempPosition = -1;

        if (onceCompleteScrollLength == -1) {
            // 因为mFirstVisiPos在下面可能被改变，所以用tempPosition暂存一下
            tempPosition = mFirstVisiPos;
            tempView = recycler.getViewForPosition(tempPosition);
            measureChildWithMargins(tempView, 0, 0);
            onceCompleteScrollLength = getDecoratedMeasurementHorizontal(tempView) + normalViewGap;
        }

        // 当前"一次完整的聚焦滑动"所在的进度百分比.百分比增加方向为向着堆叠移动的方向（即如果为FOCUS_LEFT，从右向左移动fraction将从0%到100%）
        float fraction = (Math.abs(mHorizontalOffset) % onceCompleteScrollLength) / (onceCompleteScrollLength * 1.0f);

        // 堆叠区域的偏移量 在一次完整的聚焦滑动期间，其总偏移量是一个layerPadding的距离
        float layerViewOffset = layerPadding * fraction;

        // 普通区域view的偏移量 在一次完整的聚焦滑动期间，其总位移量是一个onceCompleteScrollLength
        float normalViewOffset = onceCompleteScrollLength * fraction;

        boolean isLayerViewOffsetSetted = false;
        boolean isNormalViewOffsetSetted = false;

        // 修正第一个可见view mFirstVisiPos 已经滑动了多少个完整的onceCompleteScrollLength就代表滑动了多少个item
        mFirstVisiPos = (int) Math.floor(Math.abs(mHorizontalOffset) / onceCompleteScrollLength);
        // 临时将mLastVisiPos赋值为getItemCount() - 1，放心，下面遍历时会判断view是否已溢出屏幕，并及时修正该值并结束布局
        mLastVisiPos = getItemCount() - 1;


        //----------------3、开始布局-----------------
        for (int i = mFirstVisiPos; i <= mLastVisiPos; i++) {
            // 属于堆叠区域
            if (i - mFirstVisiPos < maxLayerCount) {
                View item;
                if (i == tempPosition && tempView != null) {
                    // 如果初始化数据时已经取了一个临时view
                    item = tempView;
                } else {
                    item = recycler.getViewForPosition(i);
                }

                addView(item);
                measureChildWithMargins(item, 0, 0);

                startX += layerPadding;

                if (!isLayerViewOffsetSetted) {
                    startX -= layerViewOffset;
                    isLayerViewOffsetSetted = true;
                }


                // 堆叠view缩放与透明度处理
                handleLayerView(this, item, i - mFirstVisiPos, maxLayerCount, i, fraction, dx);

                int l, t, r, b;
                l = (int) startX;
                t = getPaddingTop();
                r = l + getDecoratedMeasurementHorizontal(item);
                b = t + getDecoratedMeasurementVertical(item);

                layoutDecoratedWithMargins(item, l, t, r, b);
            } else {
                // 属于普通view区域
                View item = recycler.getViewForPosition(i);
                addView(item);
                measureChildWithMargins(item, 0, 0);

                startX += onceCompleteScrollLength;

                if (!isNormalViewOffsetSetted) {
                    startX += layerViewOffset;
                    startX -= normalViewOffset;
                    isNormalViewOffsetSetted = true;
                }

                // 普通view缩放与透明度处理
                if (i - mFirstVisiPos == maxLayerCount) {
                    handleFocusingView(this, item, i, fraction, dx);
                } else {
                    handleNormalView(this, item, i, fraction, dx);
                }

                int l, t, r, b;
                l = (int) startX;
                t = getPaddingTop();
                r = l + getDecoratedMeasurementHorizontal(item);
                b = t + getDecoratedMeasurementVertical(item);

                layoutDecoratedWithMargins(item, l, t, r, b);

                // 判断下一个view的布局位置是不是已经超出屏幕了，若超出，修正mLastVisiPos并跳出遍历
                if (startX + onceCompleteScrollLength > getWidth() - getPaddingRight()) {
                    mLastVisiPos = i;
                    break;
                }
            }
        }

        return dx;
    }

    private void handleNormalView(MeiLayoutManager meiLayoutManager, View view, int position, float fraction, int dx) {
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(1.0f);
    }

    private void handleFocusingView(MeiLayoutManager meiLayoutManager, View view, int position, float fraction, int dx) {
        float focusingChangeRangePercent = 0.5f;
        float minScale = 1.0f;
        float maxScale = 1.2f;

        float minAlpha = 1.0f;
        float maxAlpha = 1.0f;

        float realFraction;
        if (fraction <= focusingChangeRangePercent) {
            realFraction = fraction / focusingChangeRangePercent;
        } else {
            realFraction = 1.0f;
        }

        float realScale = minScale + (maxScale - minScale) * realFraction;
        float realAlpha = minAlpha + (maxAlpha - minAlpha) * realFraction;

        view.setScaleX(realScale);
        view.setScaleY(realScale);
        view.setAlpha(realAlpha);
    }

    private void handleLayerView(MeiLayoutManager meiLayoutManager, View view, int viewLayer, int maxLayerCount, int position, float fraction, int dx) {
        float changeRangePercent = 0.35f;
        float minScale = 0.7f;
        float maxScale = 1.2f;

        float realFraction;
        if (fraction <= changeRangePercent) {
            realFraction = fraction / changeRangePercent;
        } else {
            realFraction = 1.0f;
        }

        float scaleDelta = maxScale - minScale;

        float currentLayerMaxScale = minScale + scaleDelta * (viewLayer + 1) / (maxLayerCount * 1.0f);
        float currentLayerMinScale = minScale + scaleDelta * viewLayer / (maxLayerCount * 1.0f);

        float realScale = currentLayerMaxScale - (currentLayerMaxScale - currentLayerMinScale) * realFraction;

        view.setScaleX(realScale);
        view.setScaleY(realScale);

        float minAlpha = 0f;
        float maxAlpha = 1.0f;
        float alphaDelta = maxAlpha - minAlpha; // 总透明度差
        float currentLayerMaxAlpha =
                minAlpha + alphaDelta * (viewLayer + 1) / (maxLayerCount * 1.0f);
        float currentLayerMinAlpha = minAlpha + alphaDelta * viewLayer / (maxLayerCount * 1.0f);
        float realAlpha =
                currentLayerMaxAlpha - (currentLayerMaxAlpha - currentLayerMinAlpha) * realFraction;

        view.setAlpha(realAlpha);
    }

    /**
     * 回收需回收的item
     */
    private void recycleChildren(RecyclerView.Recycler recycler) {
        List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        for (int i = 0; i < scrapList.size(); i++) {
            RecyclerView.ViewHolder holder = scrapList.get(i);
            removeAndRecycleView(holder.itemView, recycler);
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        switch (state) {
            case RecyclerView.SCROLL_STATE_DRAGGING:
                //当手指按下时，停止当前正在播放的动画
                cancelAnimator();
                break;
            case RecyclerView.SCROLL_STATE_IDLE:
                //当列表滚动停止后，判断一下自动选中是否打开
                if (isAutoSelect) {
                    //找到离目标落点最近的item索引
                    smoothScrollToPosition(findShouldSelectPosition());
                }
                break;
        }
    }

    /**
     * 返回当前选中的position
     *
     * @param position
     */
    private void smoothScrollToPosition(int position) {
        if (position > -1 && position < getItemCount()) {
            startValueAnimator(position);
        }
    }

    private void startValueAnimator(int position) {
        cancelAnimator();

        final float distance = position * onceCompleteScrollLength - mHorizontalOffset;

        long minDuration = autoSelectMinDuration;
        long maxDuration = autoSelectMaxDuration;

        long duration;

        float distanceFraction = (Math.abs(distance) / (onceCompleteScrollLength));

        if (distance <= onceCompleteScrollLength) {
            duration = (long) (minDuration + (maxDuration - minDuration) * distanceFraction);
        } else {
            duration = (long) (maxDuration * distanceFraction);
        }

        selectAnimator = ValueAnimator.ofFloat(0.0f, distance).setDuration(duration);
        selectAnimator.setInterpolator(new LinearInterpolator());
        final float startedOffset = mHorizontalOffset;
        selectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                if (mHorizontalOffset < 0) {
                    mHorizontalOffset =
                            (long) Math.floor(startedOffset + value);
                } else {
                    mHorizontalOffset =
                            (long) Math.ceil(startedOffset + value);
                }
                requestLayout();
            }
        });
        selectAnimator.start();
    }

    /**
     * 取消动画
     */
    public void cancelAnimator() {
        if (selectAnimator != null && (selectAnimator.isStarted() || selectAnimator.isRunning())) {
            selectAnimator.cancel();
        }
    }

    private int findShouldSelectPosition() {
        if (onceCompleteScrollLength == -1 || mFirstVisiPos == -1) {
            return -1;
        }

        int remainder = -1;

        if (focusOrientation == FOCUS_LEFT) {
            remainder = (int) (Math.abs(mHorizontalOffset) % onceCompleteScrollLength);
        }

        // 超过一半，应当选中下一项
        if (remainder >= onceCompleteScrollLength / 2.0f) {
            if (mFirstVisiPos + 1 <= getItemCount() - 1) {
                return mFirstVisiPos + 1;
            }
        }

        return mFirstVisiPos;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }


    /**
     * 获取某个childView在水平方向所占的空间，将margin考虑进去
     *
     * @param view
     * @return
     */
    public int getDecoratedMeasurementHorizontal(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
    }

    /**
     * 获取某个childView在竖直方向所占的空间,将margin考虑进去
     *
     * @param view
     * @return
     */
    public int getDecoratedMeasurementVertical(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin;
    }

    public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}
