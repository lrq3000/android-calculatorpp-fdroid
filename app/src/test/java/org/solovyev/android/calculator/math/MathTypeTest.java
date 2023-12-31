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

package org.solovyev.android.calculator.math;

import static org.junit.Assert.assertEquals;
import static org.solovyev.android.calculator.math.MathType.postfix_function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.solovyev.android.calculator.BuildConfig;
import org.solovyev.android.calculator.Engine;
import org.solovyev.android.calculator.Tests;

@RunWith(value = RobolectricTestRunner.class)
public class MathTypeTest {

    Engine engine;

    @Before
    public void setUp() throws Exception {
        engine = Tests.makeEngine();
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(MathType.function, MathType.getType("sin", 0, false, engine).type);
        assertEquals(MathType.text, MathType.getType("sn", 0, false, engine).type);
        assertEquals(MathType.text, MathType.getType("s", 0, false, engine).type);
        assertEquals(MathType.text, MathType.getType("", 0, false, engine).type);

        try {
            assertEquals(MathType.text, MathType.getType("22", -1, false, engine).type);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            assertEquals(MathType.text, MathType.getType("22", 2, false, engine).type);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }

        assertEquals("atanh", MathType.getType("atanh", 0, false, engine).match);
    }

    @Test
    public void testPostfixFunctionsProcessing() throws Exception {
        assertEquals(postfix_function, MathType.getType("5!", 1, false, engine).type);
        assertEquals(postfix_function, MathType.getType("!", 0, false, engine).type);
    }
}
