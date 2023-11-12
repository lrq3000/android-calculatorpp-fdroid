package org.solovyev.android.calculator.view;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PagerViewFabBehavior extends FloatingActionButton.Behavior {

    @NonNull
    private final FloatingActionButton.OnVisibilityChangedListener
            visibilityListener = new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    // by default, FloatingActionButton#hide causes FAB to be GONE which blocks any
                    // consequent scroll updates in CoordinatorLayout#onNestedScroll. Let's make the
                    // FAB invisible instead
                    fab.setVisibility(View.INVISIBLE);
                }
            };

    public PagerViewFabBehavior() {
        super();
    }

    public PagerViewFabBehavior(Context context, AttributeSet attributeSet) {
        super();
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                       FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        if (!child.isClickable()) {
            return false;
        }
        switch (nestedScrollAxes) {
            case ViewCompat.SCROLL_AXIS_HORIZONTAL:
                return target instanceof ViewPager;
            case ViewCompat.SCROLL_AXIS_VERTICAL:
                return target instanceof RecyclerView;
            default:
                return false;
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
            View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        onScroll(child, dxConsumed, dyConsumed);
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
            View target, float velocityX, float velocityY, boolean consumed) {
        return onScroll(child, velocityX, velocityY);
    }

    private boolean onScroll(FloatingActionButton child, float scrollX, float scrollY) {
        if (scrollY > 0 && child.getVisibility() == View.VISIBLE) {
            child.hide(visibilityListener);
            return true;
        } else if (scrollY < 0 && child.getVisibility() != View.VISIBLE) {
            child.show(visibilityListener);
            return true;
        } else if (scrollX != 0 && child.getVisibility() != View.VISIBLE) {
            child.show(visibilityListener);
            return true;
        }
        return false;
    }
}
