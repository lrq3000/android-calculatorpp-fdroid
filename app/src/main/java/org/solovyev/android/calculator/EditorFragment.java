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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.solovyev.android.calculator.databinding.CppAppEditorBinding;

public class EditorFragment extends BaseFragment {

    @Inject
    Editor editor;
    EditorView editorView;

    public EditorFragment() {
        super(R.layout.cpp_app_editor);
    }

    @Override
    protected void inject(@Nonnull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        CppAppEditorBinding binding = CppAppEditorBinding.bind(view);
        editorView = binding.calculatorEditor;
        editor.setView(editorView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        editorView.requestFocus();
    }

    @Override
    public void onDestroyView() {
        editor.clearView(editorView);
        super.onDestroyView();
    }
}
