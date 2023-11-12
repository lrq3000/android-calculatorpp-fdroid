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

package org.solovyev.android.calculator;

import static org.solovyev.android.calculator.Engine.Preferences.numeralBase;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.solovyev.android.Check;
import org.solovyev.android.calculator.buttons.CppSpecialButton;
import org.solovyev.android.calculator.history.History;
import org.solovyev.android.calculator.math.MathType;
import org.solovyev.android.calculator.memory.Memory;
import org.solovyev.android.calculator.text.NumberSpan;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import jscl.NumeralBase;
import jscl.math.Expression;
import jscl.math.Generic;

@Singleton
public class Keyboard implements SharedPreferences.OnSharedPreferenceChangeListener {

    final Bus bus;
    @Nonnull
    private final MathType.Result mathType = new MathType.Result();
    @Inject
    Editor editor;
    @Inject
    Display display;
    @Inject
    History history;
    @Inject
    Lazy<Memory> memory;
    @Inject
    Calculator calculator;
    @Inject
    Engine engine;
    @Inject
    Lazy<Clipboard> clipboard;
    @Inject
    ActivityLauncher launcher;
    private boolean vibrateOnKeypress;
    @Nonnull
    private NumeralBase numberMode;
    private boolean highContrast;

    @Inject
    public Keyboard(@Nonnull SharedPreferences preferences, @Nonnull Bus bus) {
        this.bus = bus;
        this.bus.register(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        vibrateOnKeypress = Preferences.Gui.vibrateOnKeypress.getPreference(preferences);
        numberMode = numeralBase.getPreference(preferences);
        highContrast = Preferences.Gui.highContrast.getPreference(preferences);
    }

    public boolean isVibrateOnKeypress() {
        return vibrateOnKeypress;
    }

    @Nonnull
    public NumeralBase getNumberMode() {
        return numberMode;
    }

    public boolean isHighContrast() {
        return highContrast;
    }

    public boolean buttonPressed(@Nullable String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }

        if (text.length() == 1) {
            final char glyph = text.charAt(0);
            final CppSpecialButton button = CppSpecialButton.getByGlyph(glyph);
            if (button != null) {
                handleSpecialAction(button);
                return true;
            }
        }

        if (!processSpecialAction(text)) {
            processText(prepareText(text));
        }
        return true;
    }

    private void processText(@Nonnull String text) {
        int cursorPositionOffset = 0;
        final StringBuilder textToBeInserted = new StringBuilder(text);

        MathType.getType(text, 0, false, mathType, engine);
        switch (mathType.type) {
            case function:
                textToBeInserted.append("()");
                cursorPositionOffset = -1;
                break;
            case operator:
                textToBeInserted.append("()");
                cursorPositionOffset = -1;
                break;
            case comma:
                textToBeInserted.append(" ");
                break;
        }

        if (cursorPositionOffset == 0) {
            if (MathType.groupSymbols.contains(text)) {
                cursorPositionOffset = -1;
            }
        }

        editor.insert(textToBeInserted.toString(), cursorPositionOffset);
    }

    @Nonnull
    private String prepareText(@Nonnull String text) {
        if ("(  )".equals(text) || "( )".equals(text)) {
            return "()";
        } else {
            return text;
        }
    }

    private boolean processSpecialAction(@Nonnull String action) {
        final CppSpecialButton button = CppSpecialButton.getByAction(action);
        if (button == null) {
            return false;
        }
        handleSpecialAction(button);
        return true;
    }

    private void handleSpecialAction(@NonNull CppSpecialButton button) {
        switch (button) {
            case history:
                launcher.showHistory();
                break;
            case history_undo:
                history.undo();
                break;
            case history_redo:
                history.redo();
                break;
            case cursor_right:
                editor.moveCursorRight();
                break;
            case cursor_to_end:
                editor.setCursorOnEnd();
                break;
            case cursor_left:
                editor.moveCursorLeft();
                break;
            case cursor_to_start:
                editor.setCursorOnStart();
                break;
            case settings:
                launcher.showSettings();
                break;
            case settings_widget:
                launcher.showWidgetSettings();
                break;
            case like:
                launcher.openFacebook();
                break;
            case memory:
                memory.get().requestValue();
                break;
            case memory_plus:
                handleMemoryButton(true);
                break;
            case memory_minus:
                handleMemoryButton(false);
                break;
            case memory_clear:
                memory.get().clear();
                break;
            case erase:
                editor.erase();
                break;
            case paste:
                final String text = clipboard.get().getText();
                if (!TextUtils.isEmpty(text)) {
                    editor.insert(text);
                }
                break;
            case copy:
                bus.post(new Display.CopyOperation());
                break;
            case brackets_wrap:
                handleBracketsWrap();
                break;
            case equals:
                equalsButtonPressed();
                break;
            case clear:
                editor.clear();
                break;
            case functions:
                launcher.showFunctions();
                break;
            case function_add:
                launcher.showFunctionEditor();
                break;
            case var_add:
                launcher.showConstantEditor();
                break;
            case plot_add:
                launcher.plotDisplayedExpression();
                break;
            case open_app:
                launcher.openApp();
                break;
            case vars:
                launcher.showVariables();
                break;
            case operators:
                launcher.showOperators();
                break;
            case simplify:
                calculator.simplify();
                break;
            default:
                Check.shouldNotHappen();
        }
    }

    @Subscribe
    public void onCursorMoved(@Nonnull Editor.CursorMovedEvent e) {
        updateNumberMode(e.state);
    }

    @Subscribe
    public void onEditorChanged(@Nonnull Editor.ChangedEvent e) {
        updateNumberMode(e.newState);
    }

    private void updateNumberMode(@Nonnull EditorState state) {
        if (!(state.text instanceof Spannable)) {
            setNumberMode(NumeralBase.dec);
            return;
        }
        if (state.selection < 0) {
            setNumberMode(NumeralBase.dec);
            return;
        }
        final Spannable text = (Spannable) state.text;
        final NumberSpan[] spans = text.getSpans(state.selection, state.selection, NumberSpan.class);
        if (spans != null && spans.length > 0) {
            setNumberMode(spans[0].numeralBase);
            return;
        }
        setNumberMode(NumeralBase.dec);
    }

    private void setNumberMode(@Nonnull NumeralBase newNumberMode) {
        if (numberMode == newNumberMode) {
            return;
        }
        numberMode = newNumberMode;
        bus.post(new NumberModeChangedEvent(newNumberMode));
    }

    private void equalsButtonPressed() {
        if (!calculator.isCalculateOnFly()) {
            // no automatic calculations are => equals button must be used to calculate
            calculator.evaluate();
            return;
        }

        final DisplayState state = display.getState();
        if (!state.valid) {
            return;
        }
        editor.setText(state.text);
    }

    public void handleBracketsWrap() {
        final EditorState state = editor.getState();
        final int cursorPosition = state.selection;
        final CharSequence oldText = state.text;
        editor.setText("(" + oldText.subSequence(0, cursorPosition) + ")" + oldText.subSequence(cursorPosition, oldText.length()), cursorPosition + 2);
    }

    private boolean handleMemoryButton(boolean plus) {
        final DisplayState state = display.getState();
        if (!state.valid) {
            return false;
        }
        Generic value = state.getResult();
        if (value == null) {
            try {
                value = Expression.valueOf(state.text);
            } catch (jscl.text.ParseException e) {
                Log.w(App.TAG, e.getMessage(), e);
            }
        }
        if (value == null) {
            memory.get().requestShow();
            return false;
        }
        if (plus) {
            memory.get().add(value);
        } else {
            memory.get().subtract(value);
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (Preferences.Gui.vibrateOnKeypress.isSameKey(key)) {
            vibrateOnKeypress = Preferences.Gui.vibrateOnKeypress.getPreference(preferences);
        } else if (numeralBase.isSameKey(key)) {
            setNumberMode(numeralBase.getPreference(preferences));
        } else if (Preferences.Gui.highContrast.isSameKey(key)) {
            highContrast = Preferences.Gui.highContrast.getPreference(preferences);
        }
    }

    public static final class NumberModeChangedEvent {
        @Nonnull
        public final NumeralBase mode;

        public NumberModeChangedEvent(@Nonnull  NumeralBase mode) {
            this.mode = mode;
        }
    }
}
