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

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.view.*;
import androidx.fragment.app.FragmentActivity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import jscl.math.function.IConstant;
import org.solovyev.android.Check;
import org.solovyev.android.calculator.*;
import org.solovyev.android.calculator.entities.BaseEntitiesFragment;
import org.solovyev.android.calculator.entities.Category;
import org.solovyev.android.calculator.math.MathType;
import org.solovyev.common.text.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VariablesFragment extends BaseEntitiesFragment<IConstant> {

    @Inject
    VariablesRegistry registry;
    @Inject
    Calculator calculator;
    @Inject
    Bus bus;

    @Override
    protected void inject(@Nonnull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        bus.register(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        bus.unregister(this);
        super.onDestroyView();
    }

    @Nonnull
    @Override
    protected List<IConstant> getEntities() {
        final List<IConstant> entities = new ArrayList<>(registry.getEntities());
        for (Iterator<IConstant> it = entities.iterator(); it.hasNext(); ) {
            final IConstant constant = it.next();
            switch (constant.getName()) {
                case MathType.INFINITY_JSCL:
                case MathType.NAN:
                    it.remove();
                    break;
            }
        }
        return entities;
    }

    @Override
    protected Category getCategory(@Nonnull IConstant variable) {
        return registry.getCategory(variable);
    }

    @Override
    protected void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull IConstant variable, @Nonnull MenuItem.OnMenuItemClickListener listener) {
        addMenu(menu, R.string.c_use, listener);
        if (!variable.isSystem()) {
            addMenu(menu, R.string.cpp_edit, listener);
            addMenu(menu, R.string.cpp_delete, listener);
        }

        if (!Strings.isEmpty(variable.getValue())) {
            addMenu(menu, R.string.cpp_copy, listener);
        }
    }

    @Override
    protected boolean onMenuItemClicked(@Nonnull MenuItem item, @Nonnull final IConstant variable) {
        FragmentActivity activity = getActivity();
        int itemId = item.getItemId();
        if (itemId == R.string.c_use) {
            onClick(variable);
            return true;
        } else if (itemId == R.string.cpp_edit) {
            EditVariableFragment.showDialog(CppVariable.builder(variable).build(), activity);
            return true;
        } else if (itemId == R.string.cpp_delete) {
            RemovalConfirmationDialog.showForVariable(getActivity(), variable.getName(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Check.isTrue(which == DialogInterface.BUTTON_POSITIVE);
                    registry.remove(variable);
                }
            });
            return true;
        } else if (itemId == R.string.cpp_copy) {
            copyText(variable.getValue());
            return true;
        }
        return false;
    }

    @Subscribe
    public void onVariableRemoved(@NonNull VariablesRegistry.RemovedEvent e) {
        onEntityRemoved(e.variable);
    }

    @Subscribe
    public void onVariableAdded(@NonNull VariablesRegistry.AddedEvent e) {
        onEntityAdded(e.variable);
    }

    @Subscribe
    public void onVariableChanged(@NonNull VariablesRegistry.ChangedEvent e) {
        onEntityChanged(e.newVariable);
    }

    @Nullable
    @Override
    protected String getDescription(@NonNull IConstant variable) {
        return registry.getDescription(variable.getName());
    }

    @NonNull
    @Override
    protected String getName(@Nonnull IConstant variable) {
        final String name = variable.getName();
        if (!variable.isDefined()) {
            return name;
        }
        final String value = variable.getValue();
        if(TextUtils.isEmpty(value)) {
           return name;
        }
        return name + " = " + value;
    }
}
