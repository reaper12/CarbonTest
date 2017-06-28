package carbon.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import carbon.Carbon;
import carbon.R;
import carbon.animation.AnimatedColorStateList;
import carbon.drawable.DefaultPrimaryColorStateList;
import carbon.drawable.EdgeEffect;
import carbon.drawable.ripple.RippleDrawable;
import carbon.drawable.ripple.RippleView;
import carbon.internal.ElevationComparator;
import carbon.recycler.DividerItemDecoration;
import carbon.shadow.ShadowView;

public class RecyclerView extends android.support.v7.widget.RecyclerView implements TintedView, VisibleView {

    public interface OnItemClickedListener<Type> {
        void onItemClicked(View view, Type type, int position);
    }

    private EdgeEffect leftGlow;
    private EdgeEffect rightGlow;
    private int mTouchSlop;
    EdgeEffect topGlow;
    EdgeEffect bottomGlow;

    private float edgeEffectOffsetTop;
    private float edgeEffectOffsetBottom;

    private boolean drag = true;
    private float prevY;
    private int overscrollMode;
    private boolean clipToPadding;
    long prevScroll = 0;
    private boolean childDrawingOrderCallbackSet = false;

    public RecyclerView(Context context) {
        super(context, null, R.attr.carbon_recyclerViewStyle);
        initRecycler(null, R.attr.carbon_recyclerViewStyle);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        super(Carbon.getThemedContext(context, attrs, R.styleable.RecyclerView, R.attr.carbon_recyclerViewStyle, R.styleable.RecyclerView_carbon_theme), attrs, R.attr.carbon_recyclerViewStyle);
        initRecycler(attrs, R.attr.carbon_recyclerViewStyle);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(Carbon.getThemedContext(context, attrs, R.styleable.RecyclerView, defStyleAttr, R.styleable.RecyclerView_carbon_theme), attrs, defStyleAttr);
        initRecycler(attrs, defStyleAttr);
    }

    private static int[] tintIds = new int[]{
            R.styleable.RecyclerView_carbon_tint,
            R.styleable.RecyclerView_carbon_tintMode,
            R.styleable.RecyclerView_carbon_backgroundTint,
            R.styleable.RecyclerView_carbon_backgroundTintMode,
            R.styleable.RecyclerView_carbon_animateColorChanges
    };

    private void initRecycler(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RecyclerView, defStyleAttr, R.style.carbon_RecyclerView);

        for (int i = 0; i < a.getIndexCount(); i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.RecyclerView_carbon_overScroll) {
                setOverScrollMode(a.getInt(attr, ViewCompat.OVER_SCROLL_ALWAYS));
            } else if (attr == R.styleable.RecyclerView_carbon_headerTint) {
                setHeaderTint(a.getColor(attr, 0));
            } else if (attr == R.styleable.RecyclerView_carbon_headerMinHeight) {
                setHeaderMinHeight((int) a.getDimension(attr, 0.0f));
            } else if (attr == R.styleable.RecyclerView_carbon_headerParallax) {
                setHeaderParallax(a.getFloat(attr, 0.0f));
            } else if (attr == R.styleable.RecyclerView_android_divider) {
                Drawable drawable = a.getDrawable(attr);
                float height = a.getDimension(R.styleable.RecyclerView_android_dividerHeight, 0);
                if (drawable != null && height > 0) {
                    setDivider(drawable, (int) height);
                }
            } else if (attr == R.styleable.RecyclerView_edgeEffectOffsetTop) {
                setEdgeEffectOffsetTop(a.getDimension(attr, 0));
            } else if (attr == R.styleable.RecyclerView_edgeEffectOffsetBottom) {
                setEdgeEffectOffsetBottom(a.getDimension(attr, 0));
            }
        }

        Carbon.initTint(this, a, tintIds);

        a.recycle();

        setClipToPadding(false);
        setWillNotDraw(false);
    }

    public void setDivider(Drawable divider, int height) {
        addItemDecoration(new DividerItemDecoration(divider, height));
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        super.setClipToPadding(clipToPadding);
        this.clipToPadding = clipToPadding;
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        if (header != null && (getChildCount() == 0 || getChildAt(0).getTop() + getScrollY() > ev.getY()))
            if (header.dispatchTouchEvent(ev))
                return true;

        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float deltaY = prevY - ev.getY();

                if (!drag && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    drag = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (drag) {
                    final int oldY = computeVerticalScrollOffset();
                    int range = computeVerticalScrollRange() - getHeight();
                    if (header != null)
                        range += header.getHeight();
                    boolean canOverscroll = overscrollMode == ViewCompat.OVER_SCROLL_ALWAYS ||
                            (overscrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    if (canOverscroll) {
                        float pulledToY = oldY + deltaY;
                        if (pulledToY < 0) {
                            topGlow.onPull(deltaY / getHeight(), ev.getX() / getWidth());
                            if (!bottomGlow.isFinished())
                                bottomGlow.onRelease();
                        } else if (pulledToY > range) {
                            bottomGlow.onPull(deltaY / getHeight(), 1.f - ev.getX() / getWidth());
                            if (!topGlow.isFinished())
                                topGlow.onRelease();
                        }
                        if (topGlow != null && (!topGlow.isFinished() || !bottomGlow.isFinished()))
                            postInvalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (drag) {
                    drag = false;

                    if (topGlow != null) {
                        topGlow.onRelease();
                        bottomGlow.onRelease();
                    }
                }
                break;
        }
        prevY = ev.getY();

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        if (drag || topGlow == null)
            return;
        int range = computeVerticalScrollRange() - getHeight();
        if (header != null)
            range += header.getHeight();
        boolean canOverscroll = overscrollMode == ViewCompat.OVER_SCROLL_ALWAYS ||
                (overscrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

        if (canOverscroll) {
            long t = System.currentTimeMillis();
            /*int velx = (int) (dx * 1000.0f / (t - prevScroll));
            if (computeHorizontalScrollOffset() == 0 && dx < 0) {
                leftGlow.onAbsorb(-velx);
            } else if (computeHorizontalScrollOffset() == computeHorizontalScrollRange() - getWidth() && dx > 0) {
                rightGlow.onAbsorb(velx);
            }*/
            int vely = (int) (dy * 1000.0f / (t - prevScroll));
            if (computeVerticalScrollOffset() == 0 && dy < 0) {
                topGlow.onAbsorb(-vely);
            } else if (computeVerticalScrollOffset() == range && dy > 0) {
                bottomGlow.onAbsorb(vely);
            }
            prevScroll = t;
        }
    }

    public void setEdgeEffectOffsetTop(float edgeEffectOffsetTop) {
        this.edgeEffectOffsetTop = edgeEffectOffsetTop;
    }

    public void setEdgeEffectOffsetBottom(float edgeEffectOffsetBottom) {
        this.edgeEffectOffsetBottom = edgeEffectOffsetBottom;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTint();
    }


    List<View> views = new ArrayList<>();

    public List<View> getViews() {
        views.clear();
        for (int i = 0; i < getChildCount(); i++)
            views.add(getChildAt(i));
        return views;
    }

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        Collections.sort(getViews(), new ElevationComparator());

        dispatchDrawWithHeader(canvas);
    }

    @Override
    public void setOverScrollMode(int mode) {
        if (mode != OVER_SCROLL_NEVER) {
            if (topGlow == null) {
                Context context = getContext();
                topGlow = new EdgeEffect(context);
                bottomGlow = new EdgeEffect(context);
                updateTint();
            }
        } else {
            topGlow = null;
            bottomGlow = null;
        }
        super.setOverScrollMode(OVER_SCROLL_NEVER);
        this.overscrollMode = mode;
    }

    @Override
    public boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
        // TODO: why isShown() returns false after being reattached?
        if (child instanceof ShadowView && (!Carbon.IS_LOLLIPOP || ((RenderingModeView) child).getRenderingMode() == RenderingMode.Software || ((ShadowView) child).getElevationShadowColor() != null)) {
            ShadowView shadowView = (ShadowView) child;
            shadowView.drawShadow(canvas);
        }

        if (child instanceof RippleView) {
            RippleView rippleView = (RippleView) child;
            RippleDrawable rippleDrawable = rippleView.getRippleDrawable();
            if (rippleDrawable != null && rippleDrawable.getStyle() == RippleDrawable.Style.Borderless) {
                int saveCount = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(child.getLeft(), child.getTop());
                canvas.concat(child.getMatrix());
                rippleDrawable.draw(canvas);
                canvas.restoreToCount(saveCount);
            }
        }

        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        getViews();
        return super.gatherTransparentRegion(region);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int child) {
        if (childDrawingOrderCallbackSet)
            return super.getChildDrawingOrder(childCount, child);
        return views.size() > child ? indexOfChild(views.get(child)) : child;
    }

    @Override
    public void setChildDrawingOrderCallback(ChildDrawingOrderCallback childDrawingOrderCallback) {
        super.setChildDrawingOrderCallback(childDrawingOrderCallback);
        childDrawingOrderCallbackSet = childDrawingOrderCallback != null;
    }


    // -------------------------------
    // tint
    // -------------------------------

    ColorStateList tint;
    PorterDuff.Mode tintMode;
    ColorStateList backgroundTint;
    PorterDuff.Mode backgroundTintMode;
    boolean animateColorChanges;
    ValueAnimator.AnimatorUpdateListener tintAnimatorListener = animation -> {
        updateTint();
        ViewCompat.postInvalidateOnAnimation(RecyclerView.this);
    };
    ValueAnimator.AnimatorUpdateListener backgroundTintAnimatorListener = animation -> {
        updateBackgroundTint();
        ViewCompat.postInvalidateOnAnimation(RecyclerView.this);
    };

    @Override
    public void setTint(ColorStateList list) {
        this.tint = animateColorChanges && !(list instanceof AnimatedColorStateList) ? AnimatedColorStateList.fromList(list, tintAnimatorListener) : list;
        updateTint();
    }

    @Override
    public void setTint(int color) {
        if (color == 0) {
            setTint(new DefaultPrimaryColorStateList(getContext()));
        } else {
            setTint(ColorStateList.valueOf(color));
        }
    }

    @Override
    public ColorStateList getTint() {
        return tint;
    }

    private void updateTint() {
        if (tint == null)
            return;
        int color = tint.getColorForState(getDrawableState(), tint.getDefaultColor());
        if (leftGlow != null)
            leftGlow.setColor(color);
        if (rightGlow != null)
            rightGlow.setColor(color);
        if (topGlow != null)
            topGlow.setColor(color);
        if (bottomGlow != null)
            bottomGlow.setColor(color);
    }

    @Override
    public void setTintMode(@NonNull PorterDuff.Mode mode) {
        this.tintMode = mode;
        updateTint();
    }

    @Override
    public PorterDuff.Mode getTintMode() {
        return tintMode;
    }

    @Override
    public void setBackgroundTint(ColorStateList list) {
        this.backgroundTint = animateColorChanges && !(list instanceof AnimatedColorStateList) ? AnimatedColorStateList.fromList(list, backgroundTintAnimatorListener) : list;
        updateBackgroundTint();
    }

    @Override
    public void setBackgroundTint(int color) {
        if (color == 0) {
            setBackgroundTint(new DefaultPrimaryColorStateList(getContext()));
        } else {
            setBackgroundTint(ColorStateList.valueOf(color));
        }
    }

    @Override
    public ColorStateList getBackgroundTint() {
        return backgroundTint;
    }

    private void updateBackgroundTint() {
        if (getBackground() == null)
            return;
        if (backgroundTint != null && backgroundTintMode != null) {
            int color = backgroundTint.getColorForState(getDrawableState(), backgroundTint.getDefaultColor());
            getBackground().setColorFilter(new PorterDuffColorFilter(color, tintMode));
        } else {
            getBackground().setColorFilter(null);
        }
    }

    @Override
    public void setBackgroundTintMode(@NonNull PorterDuff.Mode mode) {
        this.backgroundTintMode = mode;
        updateBackgroundTint();
    }

    @Override
    public PorterDuff.Mode getBackgroundTintMode() {
        return backgroundTintMode;
    }

    public boolean isAnimateColorChangesEnabled() {
        return animateColorChanges;
    }

    public void setAnimateColorChangesEnabled(boolean animateColorChanges) {
        this.animateColorChanges = animateColorChanges;
        if (tint != null && !(tint instanceof AnimatedColorStateList))
            setTint(AnimatedColorStateList.fromList(tint, tintAnimatorListener));
        if (backgroundTint != null && !(backgroundTint instanceof AnimatedColorStateList))
            setBackgroundTint(AnimatedColorStateList.fromList(backgroundTint, backgroundTintAnimatorListener));
    }


    // -------------------------------
    // scroll bars
    // -------------------------------

    protected void onDrawHorizontalScrollBar(Canvas canvas, Drawable scrollBar, int l, int t, int r, int b) {
        scrollBar.setColorFilter(tint != null ? tint.getColorForState(getDrawableState(), tint.getDefaultColor()) : Color.WHITE, tintMode);
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }

    protected void onDrawVerticalScrollBar(Canvas canvas, Drawable scrollBar, int l, int t, int r, int b) {
        scrollBar.setColorFilter(tint != null ? tint.getColorForState(getDrawableState(), tint.getDefaultColor()) : Color.WHITE, tintMode);
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }


    // -------------------------------
    // header (do not copy)
    // -------------------------------

    View header;
    private float parallax = 0.5f;
    private int headerPadding = 0;
    private int headerTint = 0;
    private int minHeader = 0;

    protected void dispatchDrawWithHeader(Canvas canvas) {
        if (header != null) {
            int saveCount = canvas.save(Canvas.CLIP_SAVE_FLAG | Canvas.MATRIX_SAVE_FLAG);
            int headerHeight = header.getMeasuredHeight();
            float scroll = computeVerticalScrollOffset();
            canvas.clipRect(0, 0, getWidth(), Math.max(minHeader, headerHeight - scroll));
            canvas.translate(0, -scroll * parallax);
            header.draw(canvas);

            if (headerTint != 0) {
                paint.setColor(headerTint);
                paint.setAlpha((int) (Color.alpha(headerTint) * Math.min(headerHeight - minHeader, scroll) / (headerHeight - minHeader)));
                canvas.drawRect(0, 0, getWidth(), Math.max(minHeader + scroll, headerHeight), paint);
            }
            canvas.restoreToCount(saveCount);

            saveCount = canvas.save(Canvas.CLIP_SAVE_FLAG);
            canvas.clipRect(0, Math.max(minHeader, headerHeight - scroll), getWidth(), Integer.MAX_VALUE);
            super.dispatchDraw(canvas);
            canvas.restoreToCount(saveCount);
        } else {
            super.dispatchDraw(canvas);
        }
        if (topGlow != null) {
            final int scrollY = computeVerticalScrollOffset();
            if (!topGlow.isFinished()) {
                final int restoreCount = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                final int width = getWidth() - getPaddingLeft() - getPaddingRight();

                canvas.translate(getPaddingLeft(), edgeEffectOffsetTop + Math.min(0, scrollY));
                topGlow.setSize(width, getHeight());
                if (topGlow.draw(canvas))
                    invalidate();
                canvas.restoreToCount(restoreCount);
            }
            if (!bottomGlow.isFinished()) {
                final int restoreCount = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                final int width = getWidth() - getPaddingLeft() - getPaddingRight();
                final int height = getHeight();

                canvas.translate(-width + getPaddingLeft(), -edgeEffectOffsetBottom + height);
                canvas.rotate(180, width, 0);
                bottomGlow.setSize(width, height);
                if (bottomGlow.draw(canvas))
                    invalidate();
                canvas.restoreToCount(restoreCount);
            }
        }
    }

    public View getHeader() {
        return header;
    }

    public void setHeader(View view) {
        header = view;
        view.setLayoutParams(generateDefaultLayoutParams());
        requestLayout();
    }

    public void setHeader(int resId) {
        header = LayoutInflater.from(getContext()).inflate(resId, this, false);
        requestLayout();
    }

    /**
     * @return parallax amount to the header applied when scrolling
     */
    public float getHeaderParallax() {
        return parallax;
    }

    /**
     * @param amount parallax amount to apply to the header
     */
    public void setHeaderParallax(float amount) {
        parallax = amount;
    }

    public int getHeaderTint() {
        return headerTint;
    }

    public void setHeaderTint(int color) {
        headerTint = color;
    }

    /**
     * @return min header height
     */
    public int getHeaderMinHeight() {
        return minHeader;
    }

    /**
     * @param height min header height
     */
    public void setHeaderMinHeight(int height) {
        minHeader = height;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int paddingTop = getPaddingTop() - headerPadding;
        if (header != null) {
            measureChildWithMargins(header, widthMeasureSpec, 0, heightMeasureSpec, 0);
            headerPadding = header.getMeasuredHeight();
        } else {
            headerPadding = 0;
        }
        setPadding(getPaddingLeft(), paddingTop + headerPadding, getPaddingRight(), getPaddingBottom());
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getLayoutManager() == null)
            setLayoutManager(new LinearLayoutManager(getContext()));
        super.onLayout(changed, l, t, r, b);
        if (header != null)
            header.layout(0, 0, getWidth(), header.getMeasuredHeight());
    }
}
