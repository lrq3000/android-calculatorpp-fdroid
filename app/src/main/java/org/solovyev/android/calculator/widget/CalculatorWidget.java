/*
 * Copyright 2013 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.calculator.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;

import org.solovyev.android.calculator.App;
import org.solovyev.android.calculator.Display;
import org.solovyev.android.calculator.DisplayState;
import org.solovyev.android.calculator.Editor;
import org.solovyev.android.calculator.EditorState;
import org.solovyev.android.calculator.Engine;
import org.solovyev.android.calculator.Preferences.SimpleTheme;
import org.solovyev.android.calculator.R;
import org.solovyev.android.calculator.buttons.CppButton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import static android.content.Intent.ACTION_CONFIGURATION_CHANGED;
import static org.solovyev.android.calculator.App.cast;
import static org.solovyev.android.calculator.Broadcaster.ACTION_DISPLAY_STATE_CHANGED;
import static org.solovyev.android.calculator.Broadcaster.ACTION_EDITOR_STATE_CHANGED;
import static org.solovyev.android.calculator.Broadcaster.ACTION_INIT;
import static org.solovyev.android.calculator.Broadcaster.ACTION_THEME_CHANGED;
import static org.solovyev.android.calculator.WidgetReceiver.newButtonClickedIntent;

public class CalculatorWidget extends AppWidgetProvider {

    private static final int WIDGET_CATEGORY_KEYGUARD = 2;
    private static final String OPTION_APPWIDGET_HOST_CATEGORY = "appWidgetCategory";
    @Nullable
    private static SpannedString cursorString;
    @Inject
    Editor editor;
    @Inject
    Display display;
    @Inject
    Engine engine;

    public CalculatorWidget() {
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        initCursorString(context);
    }

    @Nonnull
    private SpannedString getCursorString(@Nonnull Context context) {
        return initCursorString(context);
    }

    @Nonnull
    private SpannedString initCursorString(@Nonnull Context context) {
        if (cursorString == null) {
            final SpannableString s = App.colorString("|", readWidgetCursorColor(context));
            // this will override any other style span (f.e. italic)
            s.setSpan(new StyleSpan(Typeface.NORMAL), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            cursorString = new SpannedString(s);
        }
        return cursorString;
    }

    @ColorInt
    private int readWidgetCursorColor(@Nonnull Context context) {
        try {
            return ContextCompat.getColor(context, R.color.cpp_widget_cursor);
        } catch (Resources.NotFoundException e) {
            // on Lenovo K910 the color for some reason can't be found
            return 0x757575;
        }
    }

    @Override
    public void onUpdate(@Nonnull Context context,
                         @Nonnull AppWidgetManager appWidgetManager,
                         @Nonnull int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        updateWidget(context, appWidgetManager, appWidgetIds, false);
    }

    public void updateWidget(@Nonnull Context context, boolean partially) {
        final AppWidgetManager manager = AppWidgetManager.getInstance(context);
        final int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, CalculatorWidget.class));
        updateWidget(context, manager, widgetIds, partially);
    }

    private void updateWidget(@Nonnull Context context,
                              @Nonnull AppWidgetManager manager,
                              @Nonnull int[] widgetIds,
                              boolean partially) {
        final EditorState editorState = editor.getState();
        final DisplayState displayState = display.getState();

        final Resources resources = context.getResources();
        final SimpleTheme theme = App.getWidgetTheme().resolveThemeFor(App.getTheme());
        for (int widgetId : widgetIds) {
            final RemoteViews views = new RemoteViews(context.getPackageName(), getLayout(manager, widgetId, resources, theme));

            if (!partially) {
                for (CppButton button : CppButton.values()) {
                    final PendingIntent intent = PendingIntent.getBroadcast(context, button.id, newButtonClickedIntent(context, button), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    if (intent != null) {
                        final int buttonId;
                        if (button == CppButton.settings_widget) {
                            // overriding default settings button behavior
                            buttonId = CppButton.settings.id;
                        } else {
                            buttonId = button.id;
                        }
                        views.setOnClickPendingIntent(buttonId, intent);
                    }
                }
            }

            updateEditorState(context, views, editorState, theme);
            updateDisplayState(context, views, displayState, theme);

            views.setTextViewText(R.id.cpp_button_multiplication, engine.getMultiplicationSign());

            if (partially) {
                manager.partiallyUpdateAppWidget(widgetId, views);
            } else {
                manager.updateAppWidget(widgetId, views);
            }
        }
    }

    private int getDefaultLayout(@Nonnull SimpleTheme theme) {
        return theme.getWidgetLayout(App.getTheme());
    }

    private int getLayout(@Nonnull AppWidgetManager manager, int widgetId, @Nonnull Resources resources, @Nonnull SimpleTheme theme) {
        final Bundle options = manager.getAppWidgetOptions(widgetId);
        if (options == null) {
            return getDefaultLayout(theme);
        }

        final int category = options.getInt(OPTION_APPWIDGET_HOST_CATEGORY, -1);
        if (category == -1) {
            return getDefaultLayout(theme);
        }

        final boolean keyguard = category == WIDGET_CATEGORY_KEYGUARD;
        if (!keyguard) {
            return getDefaultLayout(theme);
        }

        final int widgetMinHeight = App.toPixels(resources.getDisplayMetrics(), options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0));
        final int lockScreenMinHeight = resources.getDimensionPixelSize(R.dimen.min_expanded_height_lock_screen);
        final boolean expanded = widgetMinHeight >= lockScreenMinHeight;
        if (expanded) {
            return R.layout.widget_layout_lockscreen;
        } else {
            return R.layout.widget_layout_lockscreen_collapsed;
        }
    }

    @Override
    public void onReceive(@Nonnull Context context, @Nonnull Intent intent) {
        cast(context).getComponent().inject(this);

        super.onReceive(context, intent);

        final String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (action) {
            case ACTION_EDITOR_STATE_CHANGED:
            case ACTION_DISPLAY_STATE_CHANGED:
                updateWidget(context, true);
                break;
            case ACTION_CONFIGURATION_CHANGED:
            case ACTION_THEME_CHANGED:
            case ACTION_INIT:
            case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
            case AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED:
                updateWidget(context, false);
                break;
        }
    }

    private void updateDisplayState(@Nonnull Context context, @Nonnull RemoteViews views, @Nonnull DisplayState displayState, @Nonnull SimpleTheme theme) {
        final boolean error = !displayState.valid;
        if (!error) {
            views.setTextViewText(R.id.calculator_display, displayState.text);
        }
        views.setTextColor(R.id.calculator_display, ContextCompat.getColor(context, theme.getDisplayTextColor(error)));
    }

    private void updateEditorState(@Nonnull Context context, @Nonnull RemoteViews views, @Nonnull EditorState state, @Nonnull SimpleTheme theme) {
        final boolean unspan = App.getTheme().light != theme.light;

        final CharSequence text = state.text;
        final int selection = state.selection;
        if (selection < 0 || selection > text.length()) {
            views.setTextViewText(R.id.calculator_editor, unspan ? App.unspan(text) : text);
            return;
        }

        final SpannableStringBuilder result;
        // inject cursor
        if (unspan) {
            final CharSequence beforeCursor = text.subSequence(0, selection);
            final CharSequence afterCursor = text.subSequence(selection, text.length());

            result = new SpannableStringBuilder();
            result.append(App.unspan(beforeCursor));
            result.append(getCursorString(context));
            result.append(App.unspan(afterCursor));
        } else {
            result = new SpannableStringBuilder(text);
            result.insert(selection, getCursorString(context));
        }
        views.setTextViewText(R.id.calculator_editor, result);
    }
}
