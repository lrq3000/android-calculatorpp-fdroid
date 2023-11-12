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

package org.solovyev.android.calculator.functions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.textfield.TextInputLayout;
import org.solovyev.android.Check;
import org.solovyev.android.calculator.AppComponent;
import org.solovyev.android.calculator.BaseDialogFragment;
import org.solovyev.android.calculator.Calculator;
import org.solovyev.android.calculator.Engine;
import org.solovyev.android.calculator.FloatingCalculatorKeyboard;
import org.solovyev.android.calculator.Keyboard;
import org.solovyev.android.calculator.ParseException;
import org.solovyev.android.calculator.PreparedExpression;
import org.solovyev.android.calculator.R;
import org.solovyev.android.calculator.VariablesRegistry;
import org.solovyev.android.calculator.keyboard.FloatingKeyboardWindow;
import org.solovyev.android.calculator.view.EditTextCompat;
import org.solovyev.common.math.MathRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import jscl.math.function.IConstant;

import static org.solovyev.android.calculator.functions.CppFunction.NO_ID;

public abstract class BaseFunctionFragment extends BaseDialogFragment implements View.OnClickListener, View.OnFocusChangeListener, View.OnKeyListener {

    protected static final String ARG_FUNCTION = "function";
    private static final int MENU_FUNCTION = Menu.FIRST;
    private static final int MENU_CONSTANT = Menu.FIRST + 1;
    private static final int MENU_CATEGORY = Menu.FIRST + 2;

    @NonNull
    private final FloatingKeyboardWindow keyboardWindow = new FloatingKeyboardWindow(null);
    @NonNull
    private final KeyboardUser keyboardUser = new KeyboardUser();
    public FunctionParamsView paramsView;
    public TextInputLayout nameLabel;
    public EditText nameView;
    public TextInputLayout bodyLabel;
    public EditTextCompat bodyView;
    public TextInputLayout descriptionLabel;
    public EditText descriptionView;
    @Inject
    Calculator calculator;
    @Inject
    Keyboard keyboard;
    @Inject
    Typeface typeface;
    @Inject
    FunctionsRegistry functionsRegistry;
    @Inject
    VariablesRegistry variablesRegistry;
    @Nullable
    protected CppFunction function;
    @LayoutRes
    private final int layout;

    protected BaseFunctionFragment(@LayoutRes int layout) {
        this.layout = layout;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            function = arguments.getParcelable(ARG_FUNCTION);
        }
    }

    @Override
    protected void inject(@NonNull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @Override
    protected void onPrepareDialog(@NonNull AlertDialog.Builder builder) {
        builder.setNegativeButton(R.string.cpp_cancel, null);
        builder.setPositiveButton(R.string.cpp_done, null);
        builder.setTitle(isNewFunction() ? R.string.function_create_function :
                R.string.function_edit_function);
        if (!isNewFunction()) {
            builder.setNeutralButton(R.string.cpp_delete, null);
        }
    }

    protected final boolean isNewFunction() {
        return function == null || function.id == NO_ID;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    protected void onShowDialog(@NonNull AlertDialog dialog, boolean firstTime) {
        if (firstTime) {
            nameView.selectAll();
            showIme(nameView);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v instanceof EditText && FunctionParamsView.PARAM_VIEW_TAG.equals(v.getTag())) {
            final ViewParent parentView = v.getParent();
            if (parentView instanceof TextInputLayout) {
                if (hasFocus) {
                    clearError((TextInputLayout) parentView);
                } else {
                    validateParameters();
                }
            }
            return;
        }

        final int id = v.getId();
        if (id == R.id.function_name) {
            if (hasFocus) {
                clearError(nameLabel);
            } else {
                validateName();
            }
        } else if (id == R.id.function_body) {
            if (hasFocus) {
                clearError(bodyLabel);
                showKeyboard();
            } else {
                keyboardWindow.hide();
                validateBody();
            }
        }
    }

    private void showKeyboard() {
        keyboardWindow.show(new FloatingCalculatorKeyboard(keyboardUser, collectParameters()),
                getDialog());
    }

    @Nonnull
    protected final List<String> collectParameters() {
        final List<String> parameters = new ArrayList<>();
        for (String parameter : paramsView.getParams()) {
            if (!TextUtils.isEmpty(parameter)) {
                parameters.add(parameter);
            }
        }
        return parameters;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.function_body) {
            showKeyboard();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                tryClose();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                Check.isNotNull(function);
                showRemovalDialog(function);
                break;
            default:
                super.onClick(dialog, which);
                break;
        }
    }

    protected abstract void showRemovalDialog(@NonNull CppFunction function);

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (v.getId() == R.id.function_body) {
            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && keyboardWindow.isShown()) {
                keyboardWindow.hide();
                return true;
            }
        }
        return false;
    }

    protected void tryClose() {
        if (!validate()) {
            return;
        }
        final CppFunction function = collectData();
        if (function == null) {
            return;
        }
        if (applyData(function)) {
            dismiss();
        }
    }

    @Nullable
    private CppFunction collectData() {
        try {
            final String body = calculator.prepare(bodyView.getText().toString()).getValue();

            return CppFunction.builder(nameView.getText().toString(), body)
                .withId(isNewFunction() ? NO_ID : function.id)
                .withParameters(collectParameters())
                .withDescription(descriptionView.getText().toString()).build();
        } catch (RuntimeException e) {
            setError(bodyLabel, e.getLocalizedMessage());
        }
        return null;
    }

    private boolean validate() {
        return validateName() & validateParameters() & validateBody();
    }

    protected boolean validateName() {
        return true;
    }

    private boolean validateBody() {
        final String body = bodyView.getText().toString();
        if (TextUtils.isEmpty(body)) {
            setError(bodyLabel, getString(R.string.cpp_field_cannot_be_empty));
            return false;
        }
        try {
            final PreparedExpression pe = calculator.prepare(body);
            if (pe.hasUndefinedVariables()) {
                // check that all undefined variables are actually function parameters
                final List<String> parameters = collectParameters();
                for (IConstant undefinedVariable : pe.getUndefinedVariables()) {
                    if (!parameters.contains(undefinedVariable.getName())) {
                        setError(bodyLabel, getString(R.string.c_error));
                        return false;
                    }
                }
            }
            clearError(bodyLabel);
            return true;
        } catch (ParseException e) {
            setError(bodyLabel, e.getLocalizedMessage());
            return false;
        }
    }

    private boolean validateParameters() {
        boolean valid = true;
        final List<String> parameters = paramsView.getParams();
        final Set<String> usedParameters = new HashSet<>();
        for (int i = 0; i < parameters.size(); i++) {
            final String parameter = parameters.get(i);
            final TextInputLayout paramLabel = paramsView.getParamLabel(i);
            if (TextUtils.isEmpty(parameter)) {
                clearError(paramLabel);
            } else if (!Engine.isValidName(parameter)) {
                valid = false;
                setError(paramLabel, getString(R.string.cpp_name_contains_invalid_characters));
            } else if (usedParameters.contains(parameter)) {
                valid = false;
                setError(paramLabel, getString(R.string.cpp_duplicate_parameter, parameter));
            } else {
                usedParameters.add(parameter);
                clearError(paramLabel);
            }
        }
        return valid;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    protected View onCreateDialogView(@NonNull Context context, @NonNull LayoutInflater inflater, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(layout, null);
        paramsView = view.findViewById(R.id.function_params);
        nameLabel = view.findViewById(R.id.function_name_label);
        nameView = view.findViewById(R.id.function_name);
        bodyLabel = view.findViewById(R.id.function_body_label);
        bodyView = view.findViewById(R.id.function_body);
        descriptionLabel = view.findViewById(R.id.function_description_label);
        descriptionView = view.findViewById(R.id.function_description);

        if (savedInstanceState == null && function != null) {
            paramsView.addParams(function.getParameters());
            nameView.setText(function.getName());
            descriptionView.setText(function.getDescription());
            bodyView.setText(function.getBody());
        }
        nameView.setOnFocusChangeListener(this);
        paramsView.setOnFocusChangeListener(this);
        bodyView.setOnClickListener(this);
        bodyView.setOnFocusChangeListener(this);
        bodyView.setOnKeyListener(this);
        bodyView.dontShowSoftInputOnFocusCompat();
        descriptionView.setOnFocusChangeListener(this);

        return view;
    }

    private class KeyboardUser implements FloatingCalculatorKeyboard.User, MenuItem.OnMenuItemClickListener {
        @NonNull
        @Override
        public Context getContext() {
            return getActivity();
        }

        @NonNull
        @Override
        public EditText getEditor() {
            return bodyView;
        }

        @NonNull
        @Override
        public ViewGroup getKeyboard() {
            return keyboardWindow.getContentView();
        }

        @Override
        public void insertOperator(char operator) {
            insertOperator(String.valueOf(operator));
        }

        public int clampSelection(int selection) {
            return selection < 0 ? 0 : selection;
        }

        @Override
        public void insertOperator(@NonNull String operator) {
            final int start = clampSelection(bodyView.getSelectionStart());
            final int end = clampSelection(bodyView.getSelectionEnd());
            final Editable e = bodyView.getText();
            e.replace(start, end, getOperator(start, end, e, operator));
        }

        @NonNull
        private String getOperator(int start, int end, @NonNull Editable e, @NonNull CharSequence operator) {
            boolean spaceBefore = true;
            boolean spaceAfter = true;
            if (start > 0 && Character.isSpaceChar(e.charAt(start - 1))) {
                spaceBefore = false;
            }
            if (end < e.length() && Character.isSpaceChar(e.charAt(end))) {
                spaceAfter = false;
            }

            if (spaceBefore && spaceAfter) {
                return " " + operator + " ";
            }
            if (spaceBefore) {
                return " " + operator;
            }
            if (spaceAfter) {
                return operator + " ";
            }
            return String.valueOf(operator);
        }

        @Override
        public void showConstants(@NonNull View v) {
            bodyView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    final int id = v.getId();
                    if (id == R.id.function_body) {
                        menu.clear();
                        addEntities(menu, getNamesSorted(variablesRegistry), MENU_CONSTANT);
                        unregisterForContextMenu(bodyView);
                    }
                }
            });
            bodyView.showContextMenu();
        }

        @Nonnull
        private List<String> getNamesSorted(@NonNull MathRegistry<?> registry) {
            final List<String> names = new ArrayList<>(registry.getNames());
            Collections.sort(names);
            return names;
        }

        @Override
        public void showFunctions(@NonNull View v) {
            bodyView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    final int id = v.getId();
                    if (id == R.id.function_body) {
                        menu.clear();
                        addEntities(menu, getNamesSorted(functionsRegistry), MENU_FUNCTION);
                        unregisterForContextMenu(bodyView);
                    }
                }
            });
            bodyView.showContextMenu();
        }

        private void addEntities(@NonNull Menu menu, @NonNull List<String> entities, int groupId) {
            for (String entity : entities) {
                menu.add(groupId, Menu.NONE, Menu.NONE, entity).setOnMenuItemClickListener(KeyboardUser.this);
            }
        }

        @Override
        public void showFunctionsConstants(@NonNull View v) {
            bodyView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    final int id = v.getId();
                    if (id == R.id.function_body) {
                        menu.clear();
                        // can't use sub-menus as AlertDialog doesn't support them
                        menu.add(MENU_CATEGORY, MENU_CONSTANT, Menu.NONE, R.string.cpp_vars_and_constants).setOnMenuItemClickListener(KeyboardUser.this);
                        menu.add(MENU_CATEGORY, MENU_FUNCTION, Menu.NONE, R.string.c_functions).setOnMenuItemClickListener(KeyboardUser.this);
                        unregisterForContextMenu(bodyView);
                    }
                }
            });
            bodyView.showContextMenu();
        }

        @Override
        public void insertText(@NonNull CharSequence text, int selectionOffset) {
            EditTextCompat.insert(text, bodyView);
            if (selectionOffset != 0) {
                final int selection = clampSelection(bodyView.getSelectionEnd());
                final int newSelection = selection + selectionOffset;
                if (newSelection >= 0 && newSelection < bodyView.getText().length()) {
                    bodyView.setSelection(newSelection);
                }
            }
            // if a parameter has been inserted - update the parameter's list
            if (TextUtils.equals(text, "x") || TextUtils.equals(text, "y")) {
                final String possibleParam = text.toString();
                if (!collectParameters().contains(possibleParam)) {
                    paramsView.addParam(possibleParam);
                }
            }
        }

        @Override
        public boolean isVibrateOnKeypress() {
            return keyboard.isVibrateOnKeypress();
        }

        @NonNull
        @Override
        public Typeface getTypeface() {
            return typeface;
        }

        @Override
        public void done() {
            keyboardWindow.hide();
            validateBody();
        }

        @Override
        public void showIme() {
            final InputMethodManager keyboard = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(getEditor(), InputMethodManager.SHOW_FORCED);
            keyboardWindow.hide();
        }

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            final int groupId = item.getGroupId();
            final CharSequence title = item.getTitle();
            switch (groupId) {
                case MENU_FUNCTION:
                    final int argsListIndex = title.toString().indexOf("(");
                    if (argsListIndex < 0) {
                        keyboardUser.insertText(title + "()", -1);
                    } else {
                        keyboardUser.insertText(title.subSequence(0, argsListIndex) + "()", -1);
                    }
                    return true;
                case MENU_CONSTANT:
                    keyboardUser.insertText(title.toString(), 0);
                    return true;
                case MENU_CATEGORY:
                    bodyView.post(new Runnable() {
                        @Override
                        public void run() {
                            final int itemId = item.getItemId();
                            if (itemId == MENU_FUNCTION) {
                                showFunctions(bodyView);
                            } else if (itemId == MENU_CONSTANT) {
                                showConstants(bodyView);
                            }
                        }
                    });
                    return true;
            }
            return false;
        }
    }

    protected abstract boolean applyData(@NonNull CppFunction function);
}
