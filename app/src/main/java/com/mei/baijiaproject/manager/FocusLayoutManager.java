package com.mei.baijiaproject.manager;

import android.animation.ValueAnimator;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/6/25
 */
public class FocusLayoutManager extends RecyclerView.LayoutManager {

    // 堆叠view之间的间距
    private float layerPadding;

    // 堆叠数量
    private int maxLayerCount = 4;

    // 堆叠方向
    private int focusOrientation = 1;

    // 普通view之间的间距
    private float normalViewGap;

    // 普通view 滚动到 焦点view 为一次完整的聚焦滑动所需要移动的距离
    private float onceCompleteScrollLength = -1;

    // 累计水平偏移量
    private int mHorizontalOffset;

    private int mLastVisiPos;

    private int mFirstVisiPos;

    // 是否自动选中
    private boolean isAutoSelect;

    private List<TransitionListener> transitionListeners;

    private View.OnFocusChangeListener onFocusChangeListener;

    private int focusPosition = -1;

    /**
     * 自动选中动画
     */
    private ValueAnimator selectAnimator;
    private long autoSelectMinDuration;
    private long autoSelectMaxDuration;

    // 在普通view移动了一个onceCompleteScrollLength 堆叠View只移动了一个layerPadding
    public FocusLayoutManager() {
        this(new Builder());
    }

    public FocusLayoutManager(Builder builder) {
        this.maxLayerCount = builder.maxLayerCount;
        this.focusOrientation = builder.focusOrientation;
        this.layerPadding = builder.layerPadding;
        this.transitionListeners = builder.trasitionListeners;
        this.normalViewGap = builder.normalViewGap;
        this.isAutoSelect = builder.isAutoSelect;
        this.onFocusChangeListener = builder.onFocusChangeListener;
        this.autoSelectMinDuration = builder.autoSelectMinDuration;
        this.autoSelectMaxDuration = builder.autoSelectMaxDuration;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //  super.onLayoutChildren(recycler, state);
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }

        onceCompleteScrollLength = -1;

        //分离全部已有的view 放入临时缓存
        detachAndScrapAttachedViews(recycler);

        fill(recycler, state, 0);

    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 手指从右向左滑动  dx > 0
        // 从左向右滑动 dx < 0

        // 位移0、没有子View 直接返回 0
        if (dx == 0 || getChildCount() == 0) {
            return 0;
        }

        // 累加实际滑动距离
        mHorizontalOffset += dx;

        // dx 实际消费的距离
        dx = fill(recycler, state, dx);

        return dx;
    }

    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        int resultDelta = dx;

        resultDelta = fillHorizontalLeft(recycler, state, dx);

        return resultDelta;
    }

    private int fillHorizontalLeft(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {

        // -------------------1、边界检测-----------------------
        if (dx < 0) {
            // 从左向右滑动 已到达左边界 mHorizontalOffset < 0
            if (mHorizontalOffset < 0) {
                mHorizontalOffset = dx = 0;
            }
        }

        if (dx > 0) {

            // 滑动到只剩下堆叠view 没有普通view了 说明已经到达右边界了
            if (mLastVisiPos - mFirstVisiPos <= maxLayerCount - 1) {

                // 因为 scrollHorizontallyBy 里加了一次 dx 现在减回去
                mHorizontalOffset -= dx;
                dx = 0;
            }
        }

        // -------------------2、分离全部view 放入临时缓存 mAttachedScrap 中-----------------------
        detachAndScrapAttachedViews(recycler);

        // -------------------3、初始化布局数据-----------------------
        float startX = getPaddingLeft() - layerPadding;
        View tempView = null;
        int tempPosition = -1;

        if (onceCompleteScrollLength == -1) {
            // 因为mFirstVisiPos在下面可能会被改变 所以用tempPosition暂存一下
            tempPosition = mFirstVisiPos;
            tempView = recycler.getViewForPosition(tempPosition);
            measureChildWithMargins(tempView, 0, 0);

            onceCompleteScrollLength = getDecoratedMeasureHorizontal(tempView) + normalViewGap;
        }

        // 当前 一次完成的聚焦滑动 所在的百分比 百分比增加方向为向着堆叠移动的方向（即如果为FOCUS_LEFT 从右向左移动fraction将从0%到100%）
        float fraction = (Math.abs(mHorizontalOffset) % onceCompleteScrollLength) / (onceCompleteScrollLength * 1.0F);

        // 堆叠区域view偏移量 在一次完整的聚焦滑动期间 其总偏移量是一个layerCompletePadding的距离
        float layerViewOffset = layerPadding * fraction;

        // 普通区域view偏移量 在一次完成的聚焦滑动期间 其总位移量是一个onceCompleteScrollLength
        float normalViewOffset = onceCompleteScrollLength * fraction;

        boolean isLayerViewOffsetSetted = false;
        boolean isNormalViewOffsetSetted = false;

        // 修正第一个可见的view对应的mFirstVisiPos 已经滑动了多少个完整的onceCompleteScrollLength就代码滑动了多少个item
        mFirstVisiPos = (int) Math.floor(Math.abs(mHorizontalOffset / onceCompleteScrollLength)); // 向下取整
        // 临时将 mLastVisiPos赋值为 getItemCount-1 下面遍历时候会判定view是否溢出屏幕 并及时修正该值并结束布局
        mLastVisiPos = getItemCount() - 1;


        // -------------------4、开始布局-----------------------
        for (int i = mFirstVisiPos; i <= mLastVisiPos; i++) {

            // 属于堆叠区域
            if (i - mFirstVisiPos < maxLayerCount) {
                View item;
                if (i == tempPosition && tempView != null) {
                    // 如果初始化数据时候已经取了一个临时view 可别浪费了
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

                int l, t, r, b;
                l = (int) startX;
                t = getPaddingTop();
                r = (int) (startX + getDecoratedMeasureHorizontal(item));
                b = getPaddingTop() + getDecoratedMeasureVertical(item);
                layoutDecoratedWithMargins(item, l, t, r, b);

            } else {
                // 属于普通区域
                View item = recycler.getViewForPosition(i);
                addView(item);
                measureChildWithMargins(item, 0, 0);

                startX += onceCompleteScrollLength;

                if (!isNormalViewOffsetSetted) {
                    startX += layerViewOffset;
                    startX -= normalViewOffset;
                    isNormalViewOffsetSetted = true;
                }

                int l, t, r, b;
                l = (int) startX;
                t = getPaddingTop();
                r = (int) (startX + getDecoratedMeasureHorizontal(item));
                b = getPaddingTop() + getDecoratedMeasureVertical(item);
                layoutDecoratedWithMargins(item, l, t, r, b);

                // 判定下一个view的布局位置是不是已经超出了屏幕 若超出 修正 mLastVisiPos 并跳出遍历
                if (startX + onceCompleteScrollLength > getWidth() - getPaddingRight()) {
                    mLastVisiPos = i;
                    break;
                }

            }

        }

        return dx;
    }

    private void recycleChildren(RecyclerView.Recycler recycler) {
        List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        for (int i = 0; i < scrapList.size(); i++) {
            RecyclerView.ViewHolder viewHolder = scrapList.get(i);
            removeAndRecycleView(viewHolder.itemView, recycler);
        }

    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }


    /**
     * 获取某个child在水平方向所占的空间
     *
     * @param view
     * @return
     */
    public int getDecoratedMeasureHorizontal(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin;
    }

    /**
     * 获取某个child在垂直方向所占的空间
     *
     * @param view
     * @return
     */
    public int getDecoratedMeasureVertical(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin;
    }

    /**
     * 获取垂直方向的空间
     *
     * @return
     */
    public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }


    /**
     * 滚动过程中view的变换监听接口。属于高级定制，暴露了很多关键布局数据。若定制要求不高，考虑使用{@link SimpleTransitionListener}
     */
    public interface TransitionListener {

        /**
         * @param focusLayoutManager
         * @param view               view对象。请仅在方法体范围内对view做操作，不要外部强引用它，view是要被回收复用的
         * @param viewLayer          当前层级，0表示底层，maxLayerCount-1表示顶层
         * @param maxLayerCount      最大层级
         * @param position           item所在的position
         * @param fraction           "一次完整的聚焦滑动"所在的进度百分比.百分比增加方向为向着堆叠移动的方向（即如果为FOCUS_LEFT ，从右向左移动fraction将从0%到100%）
         * @param offset             当次滑动偏移量
         */
        void handleLayerView(FocusLayoutManager focusLayoutManager, View view, int viewLayer
                , int maxLayerCount, int position, float fraction, float offset);

        /**
         * 处理正聚焦的那个View（即正处在从普通位置滚向聚焦位置时的那个view,即堆叠顶层view）
         *
         * @param focusLayoutManager
         * @param view               view对象。请仅在方法体范围内对view做操作，不要外部强引用它，view是要被回收复用的
         * @param position           item所在的position
         * @param fraction           "一次完整的聚焦滑动"所在的进度百分比.百分比增加方向为向着堆叠移动的方向（即如果为FOCUS_LEFT
         * @param offset，            从右向左移动fraction将从0%到100%）
         */
        void handlerFocusingView(FocusLayoutManager focusLayoutManager, View view, int position,
                                 float fraction, float offset);

        /**
         * 处理不在堆叠里的普通view（正在聚焦的那个view除外）
         *
         * @param focusLayoutManager view对象。请仅在方法体范围内对view做操作，不要外部强引用它，view是要被回收复用的
         * @param view               item所在的position
         * @param position           "一次完整的聚焦滑动"所在的进度百分比.百分比增加方向为向着堆叠移动的方向（即如果为FOCUS_LEFT
         * @param fraction           ，从右向左移动fraction将从0%到100%）
         * @param offset             当次滑动偏移量
         */
        void handleNormalView(FocusLayoutManager focusLayoutManager, View view, int position,
                              float fraction, float offset);
    }


    public static abstract class SimpleTransitionListener {

        /**
         * 返回堆叠view最大透明度
         *
         * @param maxLayerCount 最大层级
         * @return
         */
        @FloatRange(from = 0.0f, to = 1.0f)
        public float getLayerViewMaxAlpha(int maxLayerCount) {
            return getFocusingViewMaxAlpha();
        }

        /**
         * 返回堆叠view最小透明度
         *
         * @param maxLayerCount 最大层级
         * @return
         */
        @FloatRange(from = 0.0f, to = 1.0f)
        public float getLayerViewMinAlpha(int maxLayerCount) {
            return 0;
        }

        @FloatRange(from = 0.0f, to = 1.0f)
        private float getFocusingViewMaxAlpha() {
            return 1f;
        }


        /**
         * 返回堆叠view最大缩放比例
         *
         * @param maxLayerCount 最大层级
         * @return
         */
        public float getLayerViewMaxScale(int maxLayerCount) {
            return getFocusingViewMaxScale();
        }

        /**
         * 返回堆叠view最小缩放比例
         *
         * @param maxLayerCount 最大层级
         * @return
         */
        public float getLayerViewMinScale(int maxLayerCount) {
            return 0.7f;
        }

        /**
         * 返回聚焦view的最大缩放比例
         *
         * @return
         */
        public float getFocusingViewMaxScale() {
            return 1.2f;
        }

        /**
         * 返回一个百分比值，相对于"一次完整的聚焦滑动"期间，在该百分比值内view就完成缩放、透明度的渐变变化。
         * 例：若返回值为1，说明在"一次完整的聚焦滑动"期间view将线性均匀完成缩放、透明度变化；
         * 例：若返回值为0.5，说明在"一次完整的聚焦滑动"的一半路程内（具体从什么时候开始变由实际逻辑自己决定），view将完成的缩放、透明度变化
         *
         * @return
         */
        @FloatRange(from = 0.0f, to = 1.0f)
        public float getLayerChangeRangePercent() {
            return 0.35f;
        }

        /**
         * 返回聚焦view的最小缩放比例
         *
         * @return
         */
        public float getFocusingViewMinScale() {
            return getNormalViewScale();
        }

        /**
         * 返回普通view的缩放比例
         *
         * @return
         */
        public float getNormalViewScale() {
            return 1.0f;
        }

        /**
         * 返回值意义参考{@link #getLayerChangeRangePercent()}
         *
         * @return
         */
        @FloatRange(from = 0.0f, to = 1.0f)
        public float getFocusingViewChangeRangePercent() {
            return 0.5f;
        }

        /**
         * 返回聚焦view的最小透明度
         *
         * @return
         */
        @FloatRange(from = 0.0f, to = 1.0f)
        public float getFocusingViewMinAlpha() {
            return getNormalViewAlpha();
        }

        /**
         * 返回普通view的透明度
         *
         * @return
         */
        @FloatRange(from = 0.0f, to = 1.0f)
        public float getNormalViewAlpha() {
            return 1.0f;
        }

    }


    public static class TransitionListenerConvert implements TransitionListener {

        SimpleTransitionListener stl;

        public TransitionListenerConvert(SimpleTransitionListener stl) {
            this.stl = stl;
        }

        @Override
        public void handleLayerView(FocusLayoutManager focusLayoutManager, View view, int viewLayer, int maxLayerCount, int position, float fraction, float offset) {

            /**
             * 在0~0.35f 之间
             * view均匀完成渐变 之后一直保持不变
             *
             */
            float realFraction;

            if (fraction <= stl.getLayerChangeRangePercent()) {
                realFraction = fraction / stl.getLayerChangeRangePercent();
            } else {
                realFraction = 1.0f;
            }

            float minScale = stl.getLayerViewMinScale(maxLayerCount);
            float maxScale = stl.getLayerViewMaxScale(maxLayerCount);

            float scaleDelta = maxScale - minScale;

            float currentLayerMaxScale = minScale + scaleDelta * (viewLayer + 1) / (maxLayerCount * 1.0f);
            float currentLayerMinScale = minScale + scaleDelta * viewLayer / (maxLayerCount * 1.0f);
            float realScale =
                    currentLayerMaxScale - (currentLayerMaxScale - currentLayerMinScale) * realFraction;

            float minAlpha = stl.getLayerViewMinAlpha(maxLayerCount);
            float maxAlpha = stl.getLayerViewMaxAlpha(maxLayerCount);
            float alphaDelta = maxAlpha - minAlpha; //总透明度差
            float currentLayerMaxAlpha =
                    minAlpha + alphaDelta * (viewLayer + 1) / (maxLayerCount * 1.0f);
            float currentLayerMinAlpha = minAlpha + alphaDelta * viewLayer / (maxLayerCount * 1.0f);
            float realAlpha =
                    currentLayerMaxAlpha - (currentLayerMaxAlpha - currentLayerMinAlpha) * realFraction;

            view.setScaleX(realScale);
            view.setScaleY(realScale);
            view.setAlpha(realAlpha);
        }

        @Override
        public void handlerFocusingView(FocusLayoutManager focusLayoutManager, View view, int position, float fraction, float offset) {
            /**
             * 期望效果：从0%开始到{@link SimpleTrasitionListener#getFocusingViewChangeRangePercent()} 期间
             * view均匀完成渐变，之后一直保持不变
             */
            //转换为真实的渐变百分比
            float realFraction;
            if (fraction <= stl.getFocusingViewChangeRangePercent()) {
                realFraction = fraction / stl.getFocusingViewChangeRangePercent();
            } else {
                realFraction = 1.0f;
            }

            float realScale =
                    stl.getFocusingViewMinScale() + (stl.getFocusingViewMaxScale() - stl.getFocusingViewMinScale()) * realFraction;
            float realAlpha =
                    stl.getFocusingViewMinAlpha() + (stl.getFocusingViewMaxAlpha() - stl.getFocusingViewMinAlpha()) * realFraction;

            view.setScaleX(realScale);
            view.setScaleY(realScale);
            view.setAlpha(realAlpha);
        }

        @Override
        public void handleNormalView(FocusLayoutManager focusLayoutManager, View view, int position, float fraction, float offset) {
            /**
             * 期望效果：直接完成变换
             */

            view.setScaleX(stl.getNormalViewScale());
            view.setScaleY(stl.getNormalViewScale());
            view.setAlpha(stl.getNormalViewAlpha());
        }
    }

    public static class Builder {


        int maxLayerCount;
        private int focusOrientation;
        private float layerPadding;
        private float normalViewGap;
        private boolean isAutoSelect;
        private List<TransitionListener> trasitionListeners;
        private View.OnFocusChangeListener onFocusChangeListener;
        private long autoSelectMinDuration;
        private long autoSelectMaxDuration;
        private TransitionListener defaultTrasitionListener;


        public Builder() {
            maxLayerCount = 3;
            focusOrientation = 1;
            layerPadding = 60;
            normalViewGap = 60;
            isAutoSelect = true;
            trasitionListeners = new ArrayList<>();
            defaultTrasitionListener = new TransitionListenerConvert(new SimpleTransitionListener() {
            });
            trasitionListeners.add(defaultTrasitionListener);
            onFocusChangeListener = null;
            autoSelectMinDuration = 100;
            autoSelectMaxDuration = 300;
        }

        /**
         * 最大可堆叠层级
         */
        public Builder maxLayerCount(int maxLayerCount) {
            if (maxLayerCount <= 0) {
                throw new RuntimeException("maxLayerCount不能小于0");
            }
            this.maxLayerCount = maxLayerCount;
            return this;
        }


        /**
         * 堆叠的方向。
         * 滚动方向为水平时
         * 滚动方向为垂直时
         *
         * @param focusOrientation
         * @return
         */
        public Builder focusOrientation(int focusOrientation) {
            this.focusOrientation = focusOrientation;
            return this;
        }

        /**
         * 堆叠view之间的偏移量
         *
         * @param layerPadding
         * @return
         */
        public Builder layerPadding(float layerPadding) {
            if (layerPadding < 0) {
                layerPadding = 0;
            }
            this.layerPadding = layerPadding;
            return this;
        }

        /**
         * 普通view之间的margin
         */
        public Builder normalViewGap(float normalViewGap) {
            this.normalViewGap = normalViewGap;
            return this;
        }

        /**
         * 是否自动选中
         */
        public Builder isAutoSelect(boolean isAutoSelect) {
            this.isAutoSelect = isAutoSelect;
            return this;
        }

        public Builder autoSelectDuration(@IntRange(from = 0) long minDuration, @IntRange(from =
                0) long maxDuration) {
            if (minDuration < 0 || maxDuration < 0 || maxDuration < minDuration) {
                throw new RuntimeException("autoSelectDuration入参不合法");
            }
            this.autoSelectMinDuration = minDuration;
            this.autoSelectMaxDuration = maxDuration;
            return this;
        }

        /**
         * 高级定制 添加滚动过程中view的变换监听接口
         *
         * @param trasitionListener
         * @return
         */
        public Builder addTrasitionListener(TransitionListener trasitionListener) {
            if (trasitionListener != null) {
                this.trasitionListeners.add(trasitionListener);
            }
            return this;
        }

        /**
         * 简化版 滚动过程中view的变换监听接口。实际会被转换为{@link TransitionListener}
         *
         * @param simpleTrasitionListener if null,remove current
         * @return
         */
        public Builder setSimpleTrasitionListener(SimpleTransitionListener simpleTrasitionListener) {
            this.trasitionListeners.remove(defaultTrasitionListener);
            defaultTrasitionListener = null;
            if (simpleTrasitionListener != null) {
                defaultTrasitionListener = new TransitionListenerConvert(simpleTrasitionListener);
                this.trasitionListeners.add(defaultTrasitionListener);
            }
            return this;
        }

        public Builder setOnFocusChangeListener(View.OnFocusChangeListener onFocusChangeListener) {
            this.onFocusChangeListener = onFocusChangeListener;
            return this;
        }

        public FocusLayoutManager build() {
            return new FocusLayoutManager(this);
        }
    }

}
