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

package org.solovyev.android.calculator.about;

import android.content.Context;
import androidx.annotation.NonNull;

import org.solovyev.android.calculator.App;
import org.solovyev.android.calculator.BaseActivity;
import org.solovyev.android.calculator.FragmentTab;
import org.solovyev.android.calculator.R;
import org.solovyev.android.calculator.view.Tabs;

import javax.annotation.Nonnull;

public class AboutActivity extends BaseActivity {

    public AboutActivity() {
        super(R.string.cpp_about);
    }

    public static class Dialog extends AboutActivity {
    }

    @Nonnull
    public static Class<? extends AboutActivity> getClass(@NonNull Context context) {
        return App.isTablet(context) ? Dialog.class : AboutActivity.class;
    }


    @Override
    protected void populateTabs(@Nonnull Tabs tabs) {
        super.populateTabs(tabs);
        tabs.addTab(FragmentTab.about);
        tabs.addTab(FragmentTab.release_notes);
    }
}
