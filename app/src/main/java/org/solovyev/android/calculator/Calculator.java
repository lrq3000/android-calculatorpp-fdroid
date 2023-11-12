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

package org.solovyev.android.calculator;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.solovyev.android.Check;
import org.solovyev.android.calculator.calculations.CalculationCancelledEvent;
import org.solovyev.android.calculator.calculations.CalculationFailedEvent;
import org.solovyev.android.calculator.calculations.CalculationFinishedEvent;
import org.solovyev.android.calculator.calculations.ConversionFailedEvent;
import org.solovyev.android.calculator.calculations.ConversionFinishedEvent;
import org.solovyev.android.calculator.functions.FunctionsRegistry;
import org.solovyev.android.calculator.jscl.JsclOperation;
import org.solovyev.android.calculator.variables.CppVariable;
import org.solovyev.common.msg.ListMessageRegistry;
import org.solovyev.common.msg.Message;
import org.solovyev.common.msg.MessageRegistry;
import org.solovyev.common.msg.MessageType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.converter.ConversionException;

import jscl.JsclArithmeticException;
import jscl.MathEngine;
import jscl.NumeralBase;
import jscl.math.Generic;
import jscl.math.function.Constants;
import jscl.math.function.IConstant;
import jscl.text.ParseInterruptedException;

@Singleton
public class Calculator implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final long NO_SEQUENCE = -1;

    @Nonnull
    private static final AtomicLong SEQUENCER = new AtomicLong(NO_SEQUENCE);
    @Nonnull
    private final SharedPreferences preferences;
    @Nonnull
    final Bus bus;
    @Nonnull
    private final TaskExecutor executor = new TaskExecutor();

    private volatile boolean calculateOnFly = true;

    @Inject
    Editor editor;
    @Inject
    Engine engine;
    @Inject
    ToJsclTextProcessor preprocessor;

    @Inject
    public Calculator(@Nonnull SharedPreferences preferences, @Nonnull Bus bus) {
        this.preferences = preferences;
        this.bus = bus;
        bus.register(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @VisibleForTesting
    void setSynchronous() {
        executor.setSynchronous();
    }

    @Nonnull
    private static String convert(@Nonnull Generic generic, @Nonnull NumeralBase to) throws ConversionException {
        final BigInteger value = generic.toBigInteger();
        if (value == null) {
            throw new ConversionException();
        }
        return to.toString(value);
    }

    public void evaluate() {
        final EditorState state = editor.getState();
        evaluate(JsclOperation.numeric, state.getTextString(), state.sequence);
    }

    public void simplify() {
        final EditorState state = editor.getState();
        evaluate(JsclOperation.simplify, state.getTextString(), state.sequence);
    }

    public long evaluate(@Nonnull final JsclOperation operation, @Nonnull final String expression,
            final long sequence) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                evaluateAsync(sequence, operation, expression);
            }
        }, true);

        return sequence;
    }

    public void init(@Nonnull Executor init) {
        engine.init(init);
        setCalculateOnFly(Preferences.Calculations.calculateOnFly.getPreference(preferences));
    }

    public boolean isCalculateOnFly() {
        return calculateOnFly;
    }

    public void setCalculateOnFly(boolean calculateOnFly) {
        if (this.calculateOnFly != calculateOnFly) {
            this.calculateOnFly = calculateOnFly;
            if (this.calculateOnFly) {
                evaluate();
            }
        }
    }

    private void evaluateAsync(long sequence, @Nonnull JsclOperation o, @Nonnull String e) {
        evaluateAsync(sequence, o, e, new ListMessageRegistry());
    }

    private void evaluateAsync(long sequence,
                               @Nonnull JsclOperation o,
                               @Nonnull String e,
                               @Nonnull MessageRegistry mr) {
        e = e.trim();
        if (TextUtils.isEmpty(e)) {
            bus.post(new CalculationFinishedEvent(o, e, sequence));
            return;
        }

        PreparedExpression pe = null;
        try {
            pe = prepare(e);

            try {
                final MathEngine mathEngine = engine.getMathEngine();
                mathEngine.setMessageRegistry(mr);

                final Generic result = o.evaluateGeneric(pe.value, mathEngine);

                // NOTE: toString() method must be called here as ArithmeticOperationException may occur in it (just to avoid later check!)
                //noinspection ResultOfMethodCallIgnored
                result.toString();

                final String stringResult = o.getFromProcessor(engine).process(result);
                bus.post(new CalculationFinishedEvent(o, e, sequence, result, stringResult, collectMessages(mr)));

            } catch (JsclArithmeticException exception) {
                bus.post(new CalculationFailedEvent(o, e, sequence, exception));
            }
        } catch (ArithmeticException exception) {
            onException(sequence, o, e, mr, pe, new ParseException(e, new CalculatorMessage(CalculatorMessages.msg_001, MessageType.error, exception.getMessage())));
        } catch (StackOverflowError exception) {
            onException(sequence, o, e, mr, pe, new ParseException(e, new CalculatorMessage(CalculatorMessages.msg_002, MessageType.error)));
        } catch (jscl.text.ParseException exception) {
            onException(sequence, o, e, mr, pe, new ParseException(exception));
        } catch (ParseInterruptedException exception) {
            bus.post(new CalculationCancelledEvent(o, e, sequence));
        } catch (ParseException exception) {
            onException(sequence, o, e, mr, pe, exception);
        } catch (RuntimeException exception) {
            onException(sequence, o, e, mr, pe, new ParseException(e, new CalculatorMessage(CalculatorMessages.syntax_error, MessageType.error)));
        }
    }

    @Nonnull
    private List<Message> collectMessages(@Nonnull MessageRegistry mr) {
        if (mr.hasMessage()) {
            try {
                final List<Message> messages = new ArrayList<>();
                while (mr.hasMessage()) {
                    messages.add(mr.getMessage());
                }
                return messages;
            } catch (Throwable exception) {
                // several threads might use the same instance of MessageRegistry, as no proper synchronization is done
                // catch Throwable here
                Log.e("Calculator", exception.getMessage(), exception);
            }
        }
        return Collections.emptyList();
    }

    @Nonnull
    public PreparedExpression prepare(@Nonnull String expression) throws ParseException {
        return preprocessor.process(expression);
    }

    private void onException(long sequence,
                             @Nonnull JsclOperation operation,
                             @Nonnull String e,
                             @Nonnull MessageRegistry mr,
                             @Nullable PreparedExpression pe,
                             @Nonnull ParseException parseException) {
        if (operation == JsclOperation.numeric
                && pe != null
                && pe.hasUndefinedVariables()) {
            evaluateAsync(sequence, JsclOperation.simplify, e, mr);
            return;
        }
        bus.post(new CalculationFailedEvent(operation, e, sequence, parseException));
    }

    public void convert(@Nonnull final DisplayState state,  @Nonnull final NumeralBase to) {
        final Generic value = state.getResult();
        Check.isNotNull(value);
        final NumeralBase from = engine.getMathEngine().getNumeralBase();
        if (from == to) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = convert(value, to);
                    bus.post(new ConversionFinishedEvent(result, to, state));
                } catch (ConversionException e) {
                    bus.post(new ConversionFailedEvent(state));
                }
            }
        }, false);
    }

    public boolean canConvert(@Nonnull Generic generic, @NonNull NumeralBase from, @Nonnull NumeralBase to) {
        if(from == to) {
            return false;
        }
        try {
            convert(generic, to);
            return true;
        } catch (ConversionException e) {
            return false;
        }
    }

    @Subscribe
    public void onEditorChanged(@Nonnull Editor.ChangedEvent e) {
        if (!calculateOnFly) {
            return;
        }
        if (!e.shouldEvaluate()) {
            return;
        }
        evaluate(JsclOperation.numeric, e.newState.getTextString(), e.newState.sequence);
    }

    @Subscribe
    public void onDisplayChanged(@Nonnull Display.ChangedEvent e) {
        final DisplayState newState = e.newState;
        if (!newState.valid) {
            return;
        }
        final String text = newState.text;
        if (TextUtils.isEmpty(text)) {
            return;
        }
        updateAnsVariable(text);
    }

    void updateAnsVariable(@NonNull String value) {
        final VariablesRegistry variablesRegistry = engine.getVariablesRegistry();
        final IConstant variable = variablesRegistry.get(Constants.ANS);

        final CppVariable.Builder b = variable != null ? CppVariable.builder(variable) : CppVariable.builder(Constants.ANS);
        b.withValue(value);
        b.withSystem(true);
        b.withDescription(CalculatorMessages.getBundle().getString(CalculatorMessages.ans_description));

        variablesRegistry.addOrUpdate(b.build().toJsclConstant(), variable);
    }

    @Subscribe
    public void onFunctionAdded(@Nonnull FunctionsRegistry.AddedEvent event) {
        evaluate();
    }

    @Subscribe
    public void onFunctionsChanged(@Nonnull FunctionsRegistry.ChangedEvent event) {
        evaluate();
    }

    @Subscribe
    public void onFunctionsRemoved(@Nonnull FunctionsRegistry.RemovedEvent event) {
        evaluate();
    }

    @Subscribe
    public void onVariableRemoved(@NonNull VariablesRegistry.RemovedEvent e) {
        evaluate();
    }

    @Subscribe
    public void onVariableAdded(@NonNull VariablesRegistry.AddedEvent e) {
        evaluate();
    }

    @Subscribe
    public void onVariableChanged(@NonNull VariablesRegistry.ChangedEvent e) {
        if (!e.newVariable.getName().equals(Constants.ANS)) {
            evaluate();
        }
    }

    @Override
    public void onSharedPreferenceChanged(@Nonnull SharedPreferences prefs, @Nonnull String key) {
        if (Preferences.Calculations.calculateOnFly.getKey().equals(key)) {
            setCalculateOnFly(Preferences.Calculations.calculateOnFly.getPreference(prefs));
        }
    }

    public static long nextSequence() {
        return SEQUENCER.incrementAndGet();
    }
}
