package org.solovyev.android.calculator.wizard;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import org.solovyev.android.calculator.R;

import javax.annotation.Nonnull;
import java.util.List;

final class WizardArrayAdapter<T> extends ArrayAdapter<T> {

    public WizardArrayAdapter(@Nonnull Context context, @Nonnull T[] items) {
        super(context, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, items);
    }

    public WizardArrayAdapter(@Nonnull Context context, @Nonnull List<T> items) {
        super(context, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, items);
    }

    @Nonnull
    public static WizardArrayAdapter<String> create(@Nonnull Context context, int array) {
        return new WizardArrayAdapter<>(context, context.getResources().getStringArray(array));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        if (view instanceof TextView) {
            ((TextView) view).setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
        }
        return view;
    }
}
