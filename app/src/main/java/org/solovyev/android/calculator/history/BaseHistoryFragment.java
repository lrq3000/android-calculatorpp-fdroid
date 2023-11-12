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

package org.solovyev.android.calculator.history;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.ClipboardManager;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.solovyev.android.Check;
import org.solovyev.android.calculator.AppComponent;
import org.solovyev.android.calculator.BaseActivity;
import org.solovyev.android.calculator.BaseFragment;
import org.solovyev.android.calculator.CalculatorActivity;
import org.solovyev.android.calculator.Editor;
import org.solovyev.android.calculator.R;
import org.solovyev.android.calculator.databinding.FragmentHistoryBinding;
import org.solovyev.android.calculator.databinding.FragmentHistoryItemBinding;
import org.solovyev.android.calculator.jscl.JsclOperation;
import org.solovyev.common.text.Strings;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;


public abstract class BaseHistoryFragment extends BaseFragment {
    private final boolean recentHistory;
    @Inject
    History history;
    @Inject
    Editor editor;
    @Inject
    Bus bus;
    @Inject
    Typeface typeface;
    RecyclerView recyclerView;
    private HistoryAdapter adapter;

    protected BaseHistoryFragment(boolean recentHistory) {
        super(R.layout.fragment_history);
        this.recentHistory = recentHistory;
    }

    @Override
    protected void inject(@Nonnull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @Nonnull
    public static String getHistoryText(@Nonnull HistoryState state) {
        return state.editor.getTextString() + getIdentitySign(state.display.getOperation()) + state.display.text;
    }

    @Nonnull
    private static String getIdentitySign(@Nonnull JsclOperation operation) {
        return operation == JsclOperation.simplify ? "≡" : "=";
    }

    public void useState(@Nonnull final HistoryState state) {
        editor.setState(state.editor);
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof CalculatorActivity)) {
            activity.finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        FragmentHistoryBinding binding = FragmentHistoryBinding.bind(view);
        recyclerView = binding.historyRecyclerview;
        final Context context = inflater.getContext();
        adapter = new HistoryAdapter(context);
        bus.register(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        return view;
    }

    @SuppressWarnings("deprecation")
    protected final void copyResult(@Nonnull HistoryState state) {
        final Context context = getActivity();
        final String displayText = state.display.text;
        if (Strings.isEmpty(displayText)) {
            return;
        }
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        clipboard.setText(displayText);
    }

    @Override
    public void onDestroyView() {
        bus.unregister(adapter);
        super.onDestroyView();
    }

    @SuppressWarnings("deprecation")
    protected final void copyExpression(@Nonnull HistoryState state) {
        final Context context = getActivity();
        final String editorText = state.editor.getTextString();
        if (Strings.isEmpty(editorText)) {
            return;
        }
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        clipboard.setText(editorText);
    }

    protected final boolean shouldHaveCopyResult(@Nonnull HistoryState state) {
        return !state.display.valid || !Strings.isEmpty(state.display.text);
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, View.OnClickListener, MenuItem.OnMenuItemClickListener {

        private static final int DATETIME_FORMAT = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_TIME;
        TextView valueView;
        TextView commentView;
        TextView timeView;
        @Nullable
        private HistoryState state;

        public HistoryViewHolder(@NonNull FragmentHistoryItemBinding binding) {
            super(binding.getRoot());
            BaseActivity.fixFonts(binding.getRoot(), typeface);
            valueView = binding.historyItemValue;
            commentView = binding.historyItemComment;
            timeView = binding.historyItemTime;
            itemView.setOnCreateContextMenuListener(this);
            itemView.setOnClickListener(this);
        }

        void bind(@Nonnull HistoryState state) {
            this.state = state;
            valueView.setText(BaseHistoryFragment.getHistoryText(state));
            timeView.setText(DateUtils.formatDateTime(getContext(), state.getTime(), DATETIME_FORMAT));
            final String comment = state.getComment();
            if (!Strings.isEmpty(comment)) {
                commentView.setText(comment);
                commentView.setVisibility(VISIBLE);
            } else {
                commentView.setText(null);
                commentView.setVisibility(GONE);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            Check.isNotNull(state);
            if (recentHistory) {
                addMenu(menu, R.string.c_use, this);
                addMenu(menu, R.string.c_copy_expression, this);
                if (shouldHaveCopyResult(state)) {
                    addMenu(menu, R.string.c_copy_result, this);
                }
                addMenu(menu, R.string.c_save, this);
            } else {
                addMenu(menu, R.string.c_use, this);
                addMenu(menu, R.string.c_copy_expression, this);
                if (shouldHaveCopyResult(state)) {
                    addMenu(menu, R.string.c_copy_result, this);
                }
                addMenu(menu, R.string.cpp_edit, this);
                addMenu(menu, R.string.cpp_delete, this);
            }
        }

        @Override
        public void onClick(View v) {
            Check.isNotNull(state);
            useState(state);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Check.isNotNull(state);
            int itemId = item.getItemId();
            if (itemId == R.string.c_use) {
                useState(state);
                return true;
            } else if (itemId == R.string.c_copy_expression) {
                copyExpression(state);
                return true;
            } else if (itemId == R.string.c_copy_result) {
                copyResult(state);
                return true;
            } else if (itemId == R.string.cpp_edit) {
                EditHistoryFragment.show(state, false, getParentFragmentManager());
                return true;
            } else if (itemId == R.string.c_save) {
                EditHistoryFragment.show(state, true, getParentFragmentManager());
                return true;
            } else if (itemId == R.string.cpp_delete) {
                history.removeSaved(state);
                return true;
            }
            return false;
        }
    }

    public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {

        @NonNull
        private final LayoutInflater inflater;

        @NonNull
        private final List<HistoryState> list;

        public HistoryAdapter(@NonNull Context context) {
            inflater = LayoutInflater.from(context);
            list = loadHistory();
            setHasStableIds(true);
        }

        @NonNull
        private List<HistoryState> loadHistory() {
            return recentHistory ? history.getRecent() : history.getSaved();
        }

        @Override
        public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new HistoryViewHolder(FragmentHistoryItemBinding.inflate(inflater, parent, false));
        }

        @Override
        public void onBindViewHolder(HistoryViewHolder holder, int position) {
            holder.bind(list.get(position));
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).hashCode();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Subscribe
        public void onHistoryCleared(@Nonnull History.ClearedEvent e) {
            if (e.recent != recentHistory) {
                return;
            }
            list.clear();
            notifyDataSetChanged();
        }

        @Subscribe
        public void onHistoryAdded(@Nonnull History.AddedEvent e) {
            if (e.recent != recentHistory) {
                return;
            }
            list.add(e.state);
            notifyItemInserted(0);
        }

        @Subscribe
        public void onHistoryUpdated(@Nonnull History.UpdatedEvent e) {
            if (e.recent != recentHistory) {
                return;
            }
            final int i = list.indexOf(e.state);
            if (i >= 0) {
                list.set(i, e.state);
                notifyItemChanged(i);
            }
        }

        @Subscribe
        public void onHistoryRemoved(@Nonnull History.RemovedEvent e) {
            if (e.recent != recentHistory) {
                return;
            }
            final int i = list.indexOf(e.state);
            if (i >= 0) {
                list.remove(i);
                notifyItemRemoved(i);
            }
        }
    }
}
