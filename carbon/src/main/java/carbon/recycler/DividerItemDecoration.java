package carbon.recycler;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import carbon.widget.RecyclerView;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    public interface DrawRules {
        boolean drawAfter(int position);
    }

    private Drawable drawable;
    private int height;
    private DrawRules drawRules;

    public DividerItemDecoration(Drawable drawable, int height) {
        this.drawable = drawable;
        this.height = height;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, android.support.v7.widget.RecyclerView parent, android.support.v7.widget.RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (drawable == null)
            return;
        if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1)
            return;

        if (drawRules == null || !drawRules.drawAfter(parent.getChildAdapterPosition(view)))
            return;
        if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
            outRect.top = height;
        } else {
            outRect.left = height;
        }
    }

    @Override
    public void onDrawOver(Canvas c, android.support.v7.widget.RecyclerView parent, android.support.v7.widget.RecyclerView.State state) {
        if (drawable == null) {
            super.onDrawOver(c, parent, state);
            return;
        }

        // Initialization needed to avoid compiler warning
        int left = 0, right = 0, top = 0, bottom = 0;
        int orientation = getOrientation(parent);
        int childCount = parent.getChildCount();

        if (orientation == LinearLayoutManager.VERTICAL) {
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
        } else { //horizontal
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
        }

        for (int i = 0; i < childCount - 1; i++) {
            if (drawRules == null || !drawRules.drawAfter(i))
                continue;

            View child = parent.getChildAt(i);

            if (orientation == LinearLayoutManager.VERTICAL) {
                bottom = (int) (child.getBottom() + child.getTranslationY());
                top = bottom - height;
            } else { //horizontal
                right = (int) (child.getRight() + child.getTranslationX());
                left = right - height;
            }
            c.save(Canvas.CLIP_SAVE_FLAG);
            c.clipRect(left, top, right, bottom);
            drawable.setAlpha((int) (child.getAlpha() * 255));
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(c);
            c.restore();
        }
    }

    private int getOrientation(android.support.v7.widget.RecyclerView parent) {
        if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
            return layoutManager.getOrientation();
        } else {
            throw new IllegalStateException(
                    "DividerItemDecoration can only be used with a LinearLayoutManager.");
        }
    }

    public void setDrawRules(DrawRules drawRules) {
        this.drawRules = drawRules;
    }

}