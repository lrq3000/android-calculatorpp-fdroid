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

package org.solovyev.android.calculator.entities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.solovyev.android.Check;
import org.solovyev.android.calculator.BaseActivity;
import org.solovyev.android.calculator.BaseFragment;
import org.solovyev.android.calculator.CalculatorActivity;
import org.solovyev.android.calculator.Keyboard;
import org.solovyev.android.calculator.R;
import org.solovyev.android.calculator.databinding.FragmentEntitiesBinding;
import org.solovyev.android.calculator.databinding.FragmentEntitiesItemBinding;
import org.solovyev.common.math.MathEntity;
import org.solovyev.common.text.Strings;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;



public abstract class BaseEntitiesFragment<E extends MathEntity> extends BaseFragment {

    public static final String ARG_CATEGORY = "category";
    private static final Comparator<MathEntity> COMPARATOR = new Comparator<MathEntity>() {
        @Override
        public int compare(MathEntity l, MathEntity r) {
            return l.getName().compareTo(r.getName());
        }
    };

    public RecyclerView recyclerView;
    @Inject
    Keyboard keyboard;
    private EntitiesAdapter adapter;
    @Nullable
    private String category;

    public BaseEntitiesFragment() {
        super(R.layout.fragment_entities);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle != null) {
            category = bundle.getString(ARG_CATEGORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final FragmentEntitiesBinding binding = FragmentEntitiesBinding.bind(view);
        recyclerView = binding.entitiesRecyclerview;
        final Context context = inflater.getContext();
        adapter = new EntitiesAdapter(context, TextUtils.isEmpty(category) ? getEntities() : getEntities(category));
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        return view;
    }

    protected final void onClick(@Nonnull E entity) {
        keyboard.buttonPressed(entity.getName());
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof CalculatorActivity)) {
            activity.finish();
        }
    }

    @Nonnull
    private List<E> getEntities(@NonNull String category) {
        Check.isNotEmpty(category);
        final List<E> entities = getEntities();

        final Iterator<E> iterator = entities.iterator();
        while (iterator.hasNext()) {
            final E entity = iterator.next();
            if (!isInCategory(entity, category)) {
                iterator.remove();
            }
        }

        return entities;
    }

    protected final boolean isInCategory(@NonNull E entity) {
        return TextUtils.isEmpty(category) || isInCategory(entity, category);
    }

    private boolean isInCategory(@NonNull E entity, @NonNull String category) {
        final Category entityCategory = getCategory(entity);
        if (entityCategory == null) {
            return false;
        }
        return TextUtils.equals(entityCategory.name(), category);
    }

    @Nonnull
    protected abstract List<E> getEntities();

    @Nullable
    protected abstract Category getCategory(@Nonnull E e);

    protected EntitiesAdapter getAdapter() {
        return adapter;
    }

    @SuppressWarnings("deprecation")
    protected final void copyText(@Nullable String text) {
        if (!Strings.isEmpty(text)) {
            return;
        }
        final ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
        clipboard.setText(text);
    }

    protected void onEntityAdded(@NonNull E entity) {
        final EntitiesAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (!isInCategory(entity)) {
            return;
        }
        adapter.add(entity);
    }

    protected void onEntityChanged(@NonNull E entity) {
        final EntitiesAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (!isInCategory(entity)) {
            return;
        }
        adapter.update(entity);
    }

    protected void onEntityRemoved(@NonNull E entity) {
        final EntitiesAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (!isInCategory(entity)) {
            return;
        }
        adapter.remove(entity);
    }

    @Nullable
    protected abstract String getDescription(@NonNull E entity);

    @NonNull
    protected abstract String getName(@Nonnull E entity);

    protected abstract void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull E entity, @Nonnull MenuItem.OnMenuItemClickListener listener);

    protected abstract boolean onMenuItemClicked(@Nonnull MenuItem item, @Nonnull E entity);

    public class EntityViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        TextView textView;
        TextView descriptionView;
        @Nullable
        private E entity;

        public EntityViewHolder(@Nonnull FragmentEntitiesItemBinding binding) {
            super(binding.getRoot());
            BaseActivity.fixFonts(itemView, typeface);
            textView = binding.entityText;
            descriptionView = binding.entityDescription;
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        public void bind(@Nonnull E entity) {
            this.entity = entity;
            textView.setText(getName(entity));

            final String description = getDescription(entity);
            if (!Strings.isEmpty(description)) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(description);
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            Check.isNotNull(entity);
            BaseEntitiesFragment.this.onClick(entity);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            Check.isNotNull(entity);
            BaseEntitiesFragment.this.onCreateContextMenu(menu, entity, this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Check.isNotNull(entity);
            return BaseEntitiesFragment.this.onMenuItemClicked(item, entity);
        }
    }

    private class EntitiesAdapter extends RecyclerView.Adapter<EntityViewHolder> {
        @Nonnull
        private final LayoutInflater inflater;
        @Nonnull
        private final List<E> list;

        private EntitiesAdapter(@Nonnull Context context,
                                @Nonnull List<E> list) {
            this.list = list;
            Collections.sort(this.list, COMPARATOR);
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public EntityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new EntityViewHolder(FragmentEntitiesItemBinding.inflate(inflater, parent, false));
        }

        @Override
        public void onBindViewHolder(EntityViewHolder holder, int position) {
            holder.bind(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Nonnull
        public E getItem(int position) {
            return list.get(position);
        }

        public void set(int position, @Nonnull E entity) {
            list.set(position, entity);
        }

        public void add(@Nonnull E entity) {
            final int itemCount = getItemCount();
            for (int i = 0; i < itemCount; i++) {
                final E adapterEntity = getItem(i);
                if (COMPARATOR.compare(adapterEntity, entity) > 0) {
                    list.add(i, entity);
                    notifyItemInserted(i);
                    return;
                }
            }
            list.add(itemCount, entity);
            notifyItemInserted(itemCount);
        }

        public void remove(@Nonnull E entity) {
            final int i = list.indexOf(entity);
            if (i >= 0) {
                list.remove(i);
                notifyItemRemoved(i);
            }
        }

        public void update(@NonNull E entity) {
            if (!entity.isIdDefined()) {
                return;
            }
            for (int i = 0; i < adapter.getItemCount(); i++) {
                final E adapterEntity = adapter.getItem(i);
                if (adapterEntity.isIdDefined() && entity.getId().equals(adapterEntity.getId())) {
                    adapter.set(i, entity);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }
}
