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

package org.solovyev.android.calculator.operators;

import androidx.annotation.NonNull;
import android.view.ContextMenu;
import android.view.MenuItem;
import jscl.math.operator.Operator;
import org.solovyev.android.calculator.AppComponent;
import org.solovyev.android.calculator.R;
import org.solovyev.android.calculator.entities.BaseEntitiesFragment;
import org.solovyev.android.calculator.entities.Category;
import org.solovyev.common.text.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class OperatorsFragment extends BaseEntitiesFragment<Operator> {

    @Inject
    OperatorsRegistry operatorsRegistry;
    @Inject
    PostfixFunctionsRegistry postfixFunctionsRegistry;

    @Override
    protected void inject(@Nonnull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @Nonnull
    @Override
    protected List<Operator> getEntities() {
        final List<Operator> result = new ArrayList<Operator>();

        result.addAll(operatorsRegistry.getEntities());
        result.addAll(postfixFunctionsRegistry.getEntities());

        return result;
    }

    @Override
    protected Category getCategory(@Nonnull Operator operator) {
        final Category result = operatorsRegistry.getCategory(operator);
        if (result != null) {
            return result;
        }
        return postfixFunctionsRegistry.getCategory(operator);
    }

    @Override
    protected void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull Operator operator, @Nonnull MenuItem.OnMenuItemClickListener listener) {
        addMenu(menu, R.string.c_use, listener);
    }

    @Override
    protected boolean onMenuItemClicked(@Nonnull MenuItem item, @Nonnull Operator operator) {
        if (item.getItemId() == R.string.c_use) {
            onClick(operator);
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    protected String getDescription(@NonNull Operator operator) {
        final String name = operator.getName();
        final String result = operatorsRegistry.getDescription(name);
        if (!Strings.isEmpty(result)) {
            return result;
        }
        return postfixFunctionsRegistry.getDescription(name);
    }

    @NonNull
    @Override
    protected String getName(@Nonnull Operator operator) {
        return operator.toString();
    }
}

