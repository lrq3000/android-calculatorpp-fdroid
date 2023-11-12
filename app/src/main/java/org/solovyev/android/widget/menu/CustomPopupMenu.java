package org.solovyev.android.widget.menu;

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import androidx.annotation.MenuRes;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.SubMenuBuilder;
import androidx.appcompat.widget.ForwardingListener;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.PopupMenu;

/**
 * Static library support version of the framework's {@link android.widget.PopupMenu}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation. See the framework SDK
 * documentation for a class overview.
 */
@SuppressWarnings({"unused", "RestrictedApi"})
public class CustomPopupMenu implements MenuBuilder.Callback, MenuPresenter.Callback {
    private Context mContext;
    private MenuBuilder mMenu;
    private View mAnchor;
    private CustomPopupMenuHelper mPopup;
    private PopupMenu.OnMenuItemClickListener mMenuItemClickListener;
    private PopupMenu.OnDismissListener mDismissListener;
    private View.OnTouchListener mDragListener;

    /**
     * Construct a new PopupMenu.
     *
     * @param context Context for the PopupMenu.
     * @param anchor  Anchor view for this popup. The popup will appear below the anchor if there
     *                is room, or above it if there is not.
     */
    public CustomPopupMenu(Context context, View anchor) {
        this(context, anchor, Gravity.NO_GRAVITY);
    }

    /**
     * Constructor to create a new popup menu with an anchor view and alignment
     * gravity.
     *
     * @param context Context the popup menu is running in, through which it
     *                can access the current theme, resources, etc.
     * @param anchor  Anchor view for this popup. The popup will appear below
     *                the anchor if there is room, or above it if there is not.
     * @param gravity The {@link Gravity} value for aligning the popup with its
     *                anchor.
     */
    public CustomPopupMenu(Context context, View anchor, int gravity) {
        this(context, anchor, gravity, androidx.appcompat.R.attr.popupMenuStyle, 0);
    }

    /**
     * Constructor a create a new popup menu with a specific style.
     *
     * @param context        Context the popup menu is running in, through which it
     *                       can access the current theme, resources, etc.
     * @param anchor         Anchor view for this popup. The popup will appear below
     *                       the anchor if there is room, or above it if there is not.
     * @param gravity        The {@link Gravity} value for aligning the popup with its
     *                       anchor.
     * @param popupStyleAttr An attribute in the current theme that contains a
     *                       reference to a style resource that supplies default values for
     *                       the popup window. Can be 0 to not look for defaults.
     * @param popupStyleRes  A resource identifier of a style resource that
     *                       supplies default values for the popup window, used only if
     *                       popupStyleAttr is 0 or can not be found in the theme. Can be 0
     *                       to not look for defaults.
     */
    public CustomPopupMenu(Context context, View anchor, int gravity, int popupStyleAttr,
                           int popupStyleRes) {
        mContext = context;
        mMenu = new MenuBuilder(context);
        mMenu.setCallback(this);
        mAnchor = anchor;
        mPopup = new CustomPopupMenuHelper(context, mMenu, anchor, false, popupStyleAttr, popupStyleRes);
        mPopup.setGravity(gravity);
        mPopup.setCallback(this);
    }

    /**
     * @return the gravity used to align the popup window to its anchor view
     * @see #setGravity(int)
     */
    public int getGravity() {
        return mPopup.getGravity();
    }

    /**
     * Sets the gravity used to align the popup window to its anchor view.
     * <p/>
     * If the popup is showing, calling this method will take effect only
     * the next time the popup is shown.
     *
     * @param gravity the gravity used to align the popup window
     * @see #getGravity()
     */
    public void setGravity(int gravity) {
        mPopup.setGravity(gravity);
    }

    public void setForceShowIcon(boolean forceShow) {
        mPopup.setForceShowIcon(forceShow);
    }

    /**
     * Returns an {@link android.view.View.OnTouchListener} that can be added to the anchor view
     * to implement drag-to-open behavior.
     * <p/>
     * When the listener is set on a view, touching that view and dragging
     * outside of its bounds will open the popup window. Lifting will select the
     * currently touched list item.
     * <p/>
     * Example usage:
     * <pre>
     * PopupMenu myPopup = new PopupMenu(context, myAnchor);
     * myAnchor.setOnTouchListener(myPopup.getDragToOpenListener());
     * </pre>
     *
     * @return a touch listener that controls drag-to-open behavior
     */
    public View.OnTouchListener getDragToOpenListener() {
        if (mDragListener == null) {
            mDragListener = new ForwardingListener(mAnchor) {
                @Override
                protected boolean onForwardingStarted() {
                    show();
                    return true;
                }

                @Override
                protected boolean onForwardingStopped() {
                    dismiss();
                    return true;
                }

                @Override
                public ListPopupWindow getPopup() {
                    // This will be null until show() is called.
                    return mPopup.getPopup();
                }
            };
        }

        return mDragListener;
    }

    /**
     * @return the {@link Menu} associated with this popup. Populate the returned Menu with
     * items before calling {@link #show()}.
     * @see #show()
     * @see #getMenuInflater()
     */
    public Menu getMenu() {
        return mMenu;
    }

    /**
     * @return a {@link MenuInflater} that can be used to inflate menu items from XML into the
     * menu returned by {@link #getMenu()}.
     * @see #getMenu()
     */
    public MenuInflater getMenuInflater() {
        return new SupportMenuInflater(mContext) {
            @Override
            public void inflate(int menuRes, Menu menu) {
                final int sizeBefore = menu.size();
                super.inflate(menuRes, menu);
                CustomPopupMenuHelper.tintMenuItems(mContext, menu, sizeBefore, menu.size());
            }
        };
    }

    /**
     * Inflate a menu resource into this PopupMenu. This is equivalent to calling
     * popupMenu.getMenuInflater().inflate(menuRes, popupMenu.getMenu()).
     *
     * @param menuRes Menu resource to inflate
     */
    public void inflate(@MenuRes int menuRes) {
        getMenuInflater().inflate(menuRes, mMenu);
    }

    /**
     * Show the menu popup anchored to the view specified during construction.
     *
     * @see #dismiss()
     */
    public void show() {
        mPopup.show();
    }

    /**
     * Dismiss the menu popup.
     *
     * @see #show()
     */
    public void dismiss() {
        mPopup.dismiss();
    }

    public void setKeepOnSubMenu(boolean keepOnSubMenu) {
        mPopup.setKeepOnSubMenu(keepOnSubMenu);
    }

    /**
     * @return {@code true} if the popup is currently showing, {@code false} otherwise.
     */
    public boolean isShowing() {
        return mPopup.isShowing();
    }

    /**
     * Set a listener that will be notified when the user selects an item from the menu.
     *
     * @param listener Listener to notify
     */
    public void setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener listener) {
        mMenuItemClickListener = listener;
    }

    /**
     * Set a listener that will be notified when this menu is dismissed.
     *
     * @param listener Listener to notify
     */
    public void setOnDismissListener(PopupMenu.OnDismissListener listener) {
        mDismissListener = listener;
    }

    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        if (mMenuItemClickListener != null) {
            return mMenuItemClickListener.onMenuItemClick(item);
        }
        return false;
    }

    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        if (mDismissListener != null) {
            mDismissListener.onDismiss(null);
        }
    }

    public boolean onOpenSubMenu(MenuBuilder subMenu) {
        if (subMenu == null) return false;

        if (!subMenu.hasVisibleItems()) {
            return true;
        }

        return true;
    }

    public void onCloseSubMenu(SubMenuBuilder menu) {
    }

    public void onMenuModeChange(MenuBuilder menu) {
    }
}
