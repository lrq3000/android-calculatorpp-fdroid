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

package org.solovyev.android.calculator.variables;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import org.solovyev.android.calculator.App;
import org.solovyev.android.calculator.BaseActivity;
import org.solovyev.android.calculator.FragmentTab;
import org.solovyev.android.calculator.R;
import org.solovyev.android.calculator.view.Tabs;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VariablesActivity extends BaseActivity {

    public static final String EXTRA_VARIABLE = "variable";

    public VariablesActivity() {
        super(R.string.cpp_vars_and_constants);
    }

    @Nonnull
    public static Class<? extends VariablesActivity> getClass(@NonNull Context context) {
        return App.isTablet(context) ? Dialog.class : VariablesActivity.class;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final Bundle extras = getIntent().getExtras();
            final CppVariable variable = extras != null ? (CppVariable) extras.getParcelable(EXTRA_VARIABLE) : null;
            if (variable != null) {
                EditVariableFragment.showDialog(variable, this);
            }
        }

        withFab(R.drawable.ic_add_white_36dp, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditVariableFragment.showDialog(VariablesActivity.this);
            }
        });
    }

    @Override
    protected void populateTabs(@Nonnull Tabs tabs) {
        super.populateTabs(tabs);
        for (VariableCategory category : VariableCategory.values()) {
            tabs.addTab(category, FragmentTab.variables);
        }
        tabs.setDefaultSelectedTab(Arrays.asList(VariableCategory.values()).indexOf(VariableCategory.system));
    }

    public static final class Dialog extends VariablesActivity {
    }
}
