package com.mei.baijiaproject.manager;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/6/25
 */
public class FlowLayoutManager extends RecyclerView.LayoutManager {

    // 竖直方向的偏移量 判定是否换行
    private int mVerticalOffset = 0;

    // 屏幕中第一个view的索引
    private int mFirstVisiblePos = 0;

    // 屏幕中最后一个view的索引
    private int mLastVisiblePos = 0;

    private SparseArray<Rect> mItemRectList = new SparseArray<>();

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public FlowLayoutManager() {
        setAutoMeasureEnabled(true);
    }


    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // super.onLayoutChildren(recycler, state);
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (getChildCount() == 0 && state.isPreLayout()) {
            return;
        }

        // 加入到可复用的临时缓存当中 mAttachedScrap
        detachAndScrapAttachedViews(recycler);

        // 初始化 重置数据
        mVerticalOffset = 0;
        mFirstVisiblePos = 0;
        mLastVisiblePos = getItemCount();

        // 填充数据
        fill(recycler, state);
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        fill(recycler, state, 0);
    }

    /**
     * 填充child的核心方法 应该先填充 再移动
     *
     * @param recycler
     * @param state
     * @param dy
     * @return 返回真正的dy
     */
    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        int topOffset = getPaddingTop();

        // 注意不要遍历  getItemCount()

        // 回收越界childView
        if (getChildCount() > 0) {
            for (int i = getChildCount() - 1; i <= 0; i--) {
                View child = getChildAt(i);
                if (dy > 0) {
                    if (getDecoratedBottom(child) - dy < topOffset) {
                        // 上越界 回收到recyclerPool.mScrap 新绑定数据 缓存池中
                        removeAndRecycleView(child, recycler);
                        mFirstVisiblePos++;
                        continue;
                    }
                } else if (dy < 0) {
                    // 下越界 回收到recyclerPool.mScrap 新绑定数据 缓存池中
                    if (getDecoratedTop(child) - dy > getHeight() - getPaddingBottom()) {
                        removeAndRecycleView(child, recycler);
                        mLastVisiblePos--;
                        continue;
                    }
                }
            }
        }

        int leftOffset = getPaddingLeft();
        int lineMaxHeight = 0;

        // 上移
        if (dy >= 0) {
            int minPos = mFirstVisiblePos;
            mLastVisiblePos = getItemCount() - 1;
            if (getChildCount() > 0) {
                View screenLastView = getChildAt(getChildCount() - 1);

                minPos = getPosition(screenLastView) + 1;
                topOffset = getDecoratedTop(screenLastView);
                leftOffset = getDecoratedRight(screenLastView);
                lineMaxHeight = Math.max(lineMaxHeight, getDecoratedMeasureVertical(screenLastView));

            }

            // 顺序添加 addChildView
            for (int i = minPos; i <= mLastVisiblePos; i++) {
                // 不管它是从scrap里取
                // 还是从RecyclerViewPool里取
                // 亦或是onCreateViewHolder里拿
                View child = recycler.getViewForPosition(i);
                addView(child);
                // 测量子view
                measureChildWithMargins(child, 0, 0);

                // 计算宽度 margin
                if (leftOffset + getDecoratedMeasureHorizontal(child) <= getHorizontalSpace()) {
                    // 当前行还排列的下
                    layoutDecoratedWithMargins(child,
                            leftOffset, topOffset,
                            leftOffset + getDecoratedMeasureHorizontal(child),
                            topOffset + getDecoratedMeasureVertical(child));

                    Rect rect = new Rect(leftOffset,
                            topOffset + mVerticalOffset,
                            leftOffset + getDecoratedMeasureHorizontal(child),
                            topOffset + getDecoratedMeasureVertical(child) + mVerticalOffset);
                    mItemRectList.put(i, rect);

                    // 改变 left  lineHeight
                    leftOffset += getDecoratedMeasureHorizontal(child);
                    lineMaxHeight = Math.max(lineMaxHeight, getDecoratedMeasureVertical(child));
                } else {
                    // 当前行排列不下
                    leftOffset = getPaddingLeft();
                    topOffset += lineMaxHeight;
                    lineMaxHeight = 0;

                    // 新起一行的时候要判断一下边界
                    if (topOffset - dy > getHeight() - getPaddingBottom()) {
                        // 越界了 回收
                        removeAndRecycleView(child, recycler);
                        mLastVisiblePos = i - 1;
                    } else {
                        layoutDecoratedWithMargins(child, leftOffset, topOffset, leftOffset + getDecoratedMeasureHorizontal(child), topOffset + getDecoratedMeasureVertical(child));

                        //保存Rect供逆序layout用
                        Rect rect = new Rect(leftOffset, topOffset + mVerticalOffset, leftOffset + getDecoratedMeasureHorizontal(child), topOffset + getDecoratedMeasureVertical(child) + mVerticalOffset);
                        mItemRectList.put(i, rect);

                        //改变 left  lineHeight
                        leftOffset += getDecoratedMeasureHorizontal(child);
                        lineMaxHeight = Math.max(lineMaxHeight, getDecoratedMeasureVertical(child));
                    }

                }

            }

            // 添加完后，判断是否已经没有更多的ItemView，并且此时屏幕仍有空白，则需要修正dy
            View lastChild = getChildAt(getChildCount() - 1);
            if (getPosition(lastChild) == getItemCount() - 1) {
                int gap = getHeight() - getPaddingBottom() - getDecoratedBottom(lastChild);
                if (gap > 0) {
                    dy -= gap;
                }
            }

        } else {
            // 下移

        }


        return 0;
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

}
