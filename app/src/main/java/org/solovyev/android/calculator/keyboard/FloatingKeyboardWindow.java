package org.solovyev.android.calculator.keyboard;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import org.solovyev.android.Check;
import org.solovyev.android.calculator.App;
import org.solovyev.android.calculator.R;

public class FloatingKeyboardWindow {

    @javax.annotation.Nullable
    private final PopupWindow.OnDismissListener dismissListener;

    @Nullable
    private PopupWindow window;
    @Nullable
    private Dialog dialog;
    private boolean tablet;

    public FloatingKeyboardWindow(@javax.annotation.Nullable PopupWindow.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public void hide() {
        if (!isShown()) {
            return;
        }
        Check.isNotNull(window);
        window.dismiss();
        onDismissed();
    }

    private void onDismissed() {
        if (!tablet) {
            moveDialog(Gravity.CENTER);
        }
        window = null;
        dialog = null;
    }

    public void show(@NonNull FloatingKeyboard keyboard, @Nullable Dialog dialog) {
        final EditText editor = keyboard.getUser().getEditor();
        if (isShown()) {
            App.hideIme(editor);
            return;
        }
        this.dialog = dialog;
        final Context context = editor.getContext();
        this.tablet = App.isTablet(context);
        if (!tablet) {
            moveDialog(Gravity.TOP);
        }
        App.hideIme(editor);
        final LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);

        final Resources resources = context.getResources();
        final int buttonSize = resources.getDimensionPixelSize(R.dimen.cpp_clickable_area_size);
        final boolean landscape = resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        final int keyboardWidth = keyboard.getColumnsCount(landscape) * buttonSize;
        final int keyboardHeight = keyboard.getRowsCount(landscape) * buttonSize;

        window = new PopupWindow(view, keyboardWidth, keyboardHeight);
        window.setClippingEnabled(false);
        window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                onDismissed();
                if (dismissListener != null) {
                    dismissListener.onDismiss();
                }
            }
        });
        // see http://stackoverflow.com/a/4713487/720489
        editor.post(new Runnable() {
            @Override
            public void run() {
                if (window == null) {
                    return;
                }
                if (editor.getWindowToken() != null) {
                    App.hideIme(editor);
                    final int inputWidth = editor.getWidth();
                    final int xOff = (inputWidth - keyboardWidth) / 2;
                    window.setWidth(keyboardWidth);
                    window.showAsDropDown(editor, xOff, 0);
                } else {
                    editor.postDelayed(this, 50);
                }
            }
        });
        keyboard.makeView(landscape);
    }

    public boolean isShown() {
        return window != null;
    }

    @SuppressWarnings("unchecked")
    public <V extends View> V getContentView() {
        return (V) window.getContentView();
    }

    private void moveDialog(int gravity) {
        if (dialog == null) {
            return;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        final WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = gravity;
        window.setAttributes(lp);
    }
}
