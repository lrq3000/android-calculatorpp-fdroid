package org.solovyev.android.calculator.plot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import org.solovyev.android.calculator.*;
import org.solovyev.android.plotter.Check;
import org.solovyev.android.plotter.Plot;
import org.solovyev.android.plotter.Plotter;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class PlotDimensionsFragment extends BaseDialogFragment
    implements TextView.OnEditorActionListener {
    private static final String ARG_BOUNDS = "arg-bounds";
    private static final String ARG_3D = "arg-3d";

    private class MyTextWatcher implements TextWatcher {
        @NonNull
        private final TextInputLayout input;
        private final boolean x;

        private MyTextWatcher(@NonNull TextInputLayout input, boolean x) {
            this.input = input;
            this.x = x;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (TextUtils.isEmpty(input.getError())) {
                return;
            }

            final RectF bounds = collectData();
            if (x) {
                validXBounds(bounds);
            } else {
                validYBounds(bounds);
            }
        }
    }

    @Inject
    Plotter plotter;
    @Bind(R.id.plot_x_min)
    EditText xMin;
    @Bind(R.id.plot_x_min_label)
    TextInputLayout xMinLabel;
    @Bind(R.id.plot_x_max)
    EditText xMax;
    @Bind(R.id.plot_x_max_label)
    TextInputLayout xMaxLabel;
    @Bind(R.id.plot_y_min)
    EditText yMin;
    @Bind(R.id.plot_y_min_label)
    TextInputLayout yMinLabel;
    @Bind(R.id.plot_y_max)
    EditText yMax;
    @Bind(R.id.plot_y_max_label)
    TextInputLayout yMaxLabel;
    @Bind(R.id.y_bounds)
    View yBounds;
    @NonNull
    private RectF bounds = new RectF();
    private boolean d3;

    public PlotDimensionsFragment() {
    }

    public static void show(@NonNull RectF bounds, boolean d3, @Nonnull FragmentManager fm) {
        App.showDialog(create(bounds, d3), "plot-dimensions", fm);
    }

    @NonNull
    private static PlotDimensionsFragment create(@NonNull RectF bounds, boolean d3) {
        final PlotDimensionsFragment dialog = new PlotDimensionsFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_BOUNDS, bounds);
        args.putBoolean(ARG_3D, d3);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        Check.isNotNull(arguments);
        bounds = BaseFragment.getParcelable(arguments, ARG_BOUNDS);
        d3 = arguments.getBoolean(ARG_3D);
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
    protected void onShowDialog(@NonNull AlertDialog dialog, boolean firstTime) {
        super.onShowDialog(dialog, firstTime);
        if (firstTime) {
            final InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(xMin, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    protected void onPrepareDialog(@NonNull AlertDialog.Builder builder) {
        builder.setTitle("Dimensions");
        builder.setPositiveButton(R.string.cpp_done, null);
    }

    @NonNull
    @Override
    protected View onCreateDialogView(@NonNull Context context, @NonNull LayoutInflater inflater,
        Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View view =
            LayoutInflater.from(context).inflate(R.layout.fragment_plot_dimensions, null);
        ButterKnife.bind(this, view);

        setDimension(xMin, bounds.left);
        setDimension(xMax, bounds.right);
        setDimension(yMin, bounds.top);
        setDimension(yMax, bounds.bottom);
        xMin.addTextChangedListener(new MyTextWatcher(xMinLabel, true));
        xMax.addTextChangedListener(new MyTextWatcher(xMaxLabel, true));
        yMin.addTextChangedListener(new MyTextWatcher(yMinLabel, false));
        yMax.addTextChangedListener(new MyTextWatcher(yMaxLabel, false));
        if (d3) {
            yBounds.setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                tryClose();
                return;
            default:
                super.onClick(dialog, which);
                return;
        }
    }

    private void setDimension(@NonNull EditText view, float value) {
        view.setOnEditorActionListener(this);
        view.setText(String.format("%.2f", value));
    }

    private void tryClose() {
        if (validate()) {
            applyData();
            dismiss();
        }
    }

    private boolean validate() {
        final RectF bounds = collectData();
        if (!validXBounds(bounds) | !validYBounds(bounds)) {
            return false;
        }
        return true;
    }

    private boolean validYBounds(@NonNull RectF bounds) {
        if (validNumbers(this.bounds.top, this.bounds.bottom, yMinLabel, yMaxLabel)) {
            return false;
        }
        if (bounds.top >= bounds.bottom) {
            setError(yMinLabel, " ");
            setError(yMaxLabel, "max ≯ min");
            return false;
        }
        clearError(yMinLabel);
        clearError(yMaxLabel);
        return true;
    }

    private boolean validXBounds(@NonNull RectF bounds) {
        if (validNumbers(bounds.left, bounds.right, xMinLabel, xMaxLabel)) {
            return false;
        }
        if (bounds.left >= bounds.right) {
            setError(xMinLabel, " ");
            setError(xMaxLabel, "max ≯ min");
            return false;
        }
        clearError(xMinLabel);
        clearError(xMaxLabel);
        return true;
    }

    private boolean validNumbers(float l, float r, @NonNull TextInputLayout lInput, @NonNull
    TextInputLayout rInput) {
        final boolean nanLeft = Float.isNaN(l);
        final boolean nanRight = Float.isNaN(r);
        if (nanLeft || nanRight) {
            if (nanLeft) {
                setError(lInput, R.string.cpp_nan);
            } else {
                clearError(lInput);
            }
            if (nanRight) {
                setError(rInput, R.string.cpp_nan);
            } else {
                clearError(rInput);
            }
            return true;
        }
        return false;
    }

    @NonNull
    private RectF collectData() {
        return new RectF(getDimension(xMin), getDimension(yMin), getDimension(xMax), getDimension(yMax));
    }

    private void applyData() {
        final RectF bounds = collectData();
        Plot.setGraphBounds(null, plotter, bounds, d3);
    }

    private float getDimension(@NonNull EditText view) {
        try {
            return Float.parseFloat(view.getText().toString().replace(",", ".").replace("−", "-"));
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            tryClose();
            return true;
        }
        return false;
    }
}