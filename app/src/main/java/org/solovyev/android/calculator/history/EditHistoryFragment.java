package org.solovyev.android.calculator.history;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import org.solovyev.android.calculator.AppComponent;
import org.solovyev.android.calculator.BaseDialogFragment;
import org.solovyev.android.calculator.R;

import javax.inject.Inject;
import org.solovyev.android.calculator.databinding.FragmentHistoryEditBinding;

public class EditHistoryFragment extends BaseDialogFragment {

    public static final String ARG_STATE = "state";
    public static final String ARG_NEW = "new";

    @Inject
    History history;

    HistoryState state;

    boolean newState;

    TextView expressionView;
    EditText commentView;

    public static void show(@NonNull HistoryState state, boolean newState, @NonNull FragmentManager fm) {
        final EditHistoryFragment fragment = new EditHistoryFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_STATE, state);
        args.putBoolean(ARG_NEW, newState);
        fragment.setArguments(args);
        fragment.show(fm, "edit-history-fragment");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        state = arguments.getParcelable(ARG_STATE);
        newState = arguments.getBoolean(ARG_NEW);
    }

    @Override
    protected void inject(@NonNull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    protected void onPrepareDialog(@NonNull AlertDialog.Builder builder) {
        builder.setNegativeButton(R.string.cpp_cancel, null);
        builder.setPositiveButton(R.string.c_save, null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                final HistoryState.Builder b = HistoryState.builder(state, newState)
                        .withComment(commentView.getText().toString());
                history.updateSaved(b.build());
                dismiss();
                break;
            default:
                super.onClick(dialog, which);
                break;
        }
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    protected View onCreateDialogView(@NonNull Context context, @NonNull LayoutInflater inflater, @Nullable Bundle savedInstanceState) {
        final FragmentHistoryEditBinding binding = FragmentHistoryEditBinding.inflate(inflater, null, false);
        expressionView = binding.historyExpression;
        commentView = binding.historyComment;
        if (savedInstanceState == null) {
            expressionView.setText(BaseHistoryFragment.getHistoryText(state));
            commentView.setText(state.getComment());
        }
        return binding.getRoot();
    }
}
