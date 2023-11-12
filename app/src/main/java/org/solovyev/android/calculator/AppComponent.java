package org.solovyev.android.calculator;

import org.solovyev.android.calculator.converter.ConverterFragment;
import org.solovyev.android.calculator.errors.FixableErrorFragment;
import org.solovyev.android.calculator.errors.FixableErrorsActivity;
import org.solovyev.android.calculator.floating.FloatingCalculatorBroadcastReceiver;
import org.solovyev.android.calculator.floating.FloatingCalculatorService;
import org.solovyev.android.calculator.floating.FloatingCalculatorView;
import org.solovyev.android.calculator.functions.BaseFunctionFragment;
import org.solovyev.android.calculator.functions.FunctionsFragment;
import org.solovyev.android.calculator.history.BaseHistoryFragment;
import org.solovyev.android.calculator.history.EditHistoryFragment;
import org.solovyev.android.calculator.history.HistoryActivity;
import org.solovyev.android.calculator.keyboard.BaseKeyboardUi;
import org.solovyev.android.calculator.operators.OperatorsFragment;
import org.solovyev.android.calculator.plot.PlotActivity;
import org.solovyev.android.calculator.plot.PlotDimensionsFragment;
import org.solovyev.android.calculator.plot.PlotEditFunctionFragment;
import org.solovyev.android.calculator.plot.PlotFunctionsFragment;
import org.solovyev.android.calculator.preferences.PreferencesActivity;
import org.solovyev.android.calculator.preferences.PreferencesFragment;
import org.solovyev.android.calculator.variables.EditVariableFragment;
import org.solovyev.android.calculator.variables.VariablesFragment;
import org.solovyev.android.calculator.view.Tabs;
import org.solovyev.android.calculator.widget.CalculatorWidget;
import org.solovyev.android.calculator.wizard.DragButtonWizardStep;
import org.solovyev.android.calculator.wizard.WizardActivity;
import org.solovyev.android.calculator.wizard.WizardFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(CalculatorApplication application);
    void inject(EditorFragment fragment);
    void inject(FloatingCalculatorService service);
    void inject(BaseHistoryFragment fragment);
    void inject(BaseDialogFragment fragment);
    void inject(PlotFunctionsFragment fragment);
    void inject(FixableErrorFragment fragment);
    void inject(BaseFunctionFragment fragment);
    void inject(PlotEditFunctionFragment fragment);
    void inject(EditVariableFragment fragment);
    void inject(EditHistoryFragment fragment);
    void inject(FunctionsFragment fragment);
    void inject(VariablesFragment fragment);
    void inject(OperatorsFragment fragment);
    void inject(ConverterFragment fragment);
    void inject(CalculatorActivity activity);
    void inject(FixableErrorsActivity activity);
    void inject(WidgetReceiver receiver);
    void inject(DisplayFragment fragment);
    void inject(KeyboardFragment fragment);
    void inject(PreferencesActivity activity);
    void inject(BaseKeyboardUi ui);
    void inject(FloatingCalculatorView view);
    void inject(DragButtonWizardStep fragment);
    void inject(BaseFragment fragment);
    void inject(PlotActivity.MyFragment fragment);
    void inject(PlotDimensionsFragment fragment);
    void inject(HistoryActivity activity);
    void inject(Tabs tabs);
    void inject(CalculatorWidget widget);
    void inject(WizardActivity activity);
    void inject(BaseActivity activity);
    void inject(PreferencesFragment fragment);
    void inject(WizardFragment fragment);
    void inject(FloatingCalculatorBroadcastReceiver receiver);
}
