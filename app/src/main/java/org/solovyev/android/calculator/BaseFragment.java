package org.solovyev.android.calculator;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.*;
import org.solovyev.android.plotter.Check;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static android.view.Menu.NONE;
import static org.solovyev.android.calculator.App.cast;

public abstract class BaseFragment extends Fragment {

    private final int layout;
    @Inject
    public Typeface typeface;

    protected BaseFragment(@LayoutRes int layout) {
        this.layout = layout;
    }

    @Nonnull
    public static MenuItem addMenu(@Nonnull ContextMenu menu, @StringRes int label,
                                   @Nonnull MenuItem.OnMenuItemClickListener listener) {
        return menu.add(NONE, label, NONE, label).setOnMenuItemClickListener(listener);
    }

    @NonNull
    public static <P extends Parcelable> P getParcelable(@NonNull Bundle bundle,
                                                         @NonNull String key) {
        final P parcelable = bundle.getParcelable(key);
        Check.isNotNull(parcelable);
        return parcelable;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject(cast(getActivity().getApplication()).getComponent());
    }

    protected void inject(@Nonnull AppComponent component) {
        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(layout, container, false);
        BaseActivity.fixFonts(view, typeface);
        return view;
    }
}
