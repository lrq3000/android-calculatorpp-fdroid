package org.solovyev.android.calculator;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import javax.inject.Inject;

public abstract class BaseDialogFragment extends DialogFragment {

    @Inject
    SharedPreferences preferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((CalculatorApplication) getActivity().getApplication()).getComponent().inject(this);
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        final Preferences.Gui.Theme theme = Preferences.Gui.getTheme(preferences);
        final Context context = getActivity();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = onCreateDialogView(context, inflater, savedInstanceState);
        final int spacing = context.getResources().getDimensionPixelSize(R.dimen.cpp_dialog_spacing);
        final AlertDialog.Builder b = new AlertDialog.Builder(context, theme.alertDialogTheme);
        b.setView(view, spacing, spacing, spacing, spacing);
        onPrepareDialog(b);
        return b.create();
    }

    protected abstract void onPrepareDialog(@NonNull AlertDialog.Builder builder);

    @NonNull
    protected abstract View onCreateDialogView(@NonNull Context context, @NonNull LayoutInflater inflater, @Nullable Bundle savedInstanceState);
}