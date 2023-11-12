package org.solovyev.common;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static java.lang.Math.pow;
import static java.math.BigInteger.TEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.solovyev.common.NumberFormatter.DEFAULT_MAGNITUDE;
import static org.solovyev.common.NumberFormatter.NO_ROUNDING;

public class NumberFormatterTest {

    private NumberFormatter numberFormatter;

    @Before
    public void setUp() throws Exception {
        numberFormatter = new NumberFormatter();
    }

    @Test
    public void testEngineeringFormat() throws Exception {
        numberFormatter.useEngineeringFormat(5);
        assertEquals("0.1", numberFormatter.format(0.1d));
        assertEquals("0.01", numberFormatter.format(0.01d));
        assertEquals("0.001", numberFormatter.format(0.001d));
        assertEquals("5", numberFormatter.format(5d));
        assertEquals("5000", numberFormatter.format(5000d));
    }

    @Test
    public void testScientificFormatNoRounding() throws Exception {
        numberFormatter.useScientificFormat(DEFAULT_MAGNITUDE);
        numberFormatter.setPrecision(NO_ROUNDING);

        assertEquals("1", numberFormatter.format(1d));
        assertEquals("0.333333333333333", numberFormatter.format(1d / 3));
        assertEquals("3.333333333333333E-19", numberFormatter.format(pow(10, -18) / 3));
        assertEquals("1.23456789E18", numberFormatter.format(123456789 * pow(10, 10)));
        assertEquals("1E-16", numberFormatter.format(pow(10, -16)));
        assertEquals("5.999999999999995E18", numberFormatter.format(5999999999999994999d));

        testScientificFormat();
    }

    @Test
    public void testScientificFormatWithRounding() throws Exception {
        numberFormatter.useScientificFormat(DEFAULT_MAGNITUDE);
        numberFormatter.setPrecision(5);

        assertEquals("1", numberFormatter.format(1d));
        assertEquals("0.33333", numberFormatter.format(1d / 3));
        assertEquals("3.33333E-19", numberFormatter.format(pow(10, -18) / 3));
        assertEquals("1.23457E18", numberFormatter.format(123456789 * pow(10, 10)));
        assertEquals("1E-16", numberFormatter.format(pow(10, -16)));
        assertEquals("6E18", numberFormatter.format(5999999999999994999d));


        testScientificFormat();
    }

    @Test
    public void testSimpleFormatNoRounding() throws Exception {
        numberFormatter.useSimpleFormat();
        numberFormatter.setPrecision(NO_ROUNDING);

        assertEquals("1", numberFormatter.format(1d));
        assertEquals("0.000001", numberFormatter.format(pow(10, -6)));
        assertEquals("0.333333333333333", numberFormatter.format(1d / 3));
        assertEquals("3.333333333333333E-19", numberFormatter.format(pow(10, -18) / 3));
        assertEquals("1234567890000000000", numberFormatter.format(123456789 * pow(10, 10)));
        assertEquals("1E-16", numberFormatter.format(pow(10, -16)));
        assertEquals("1E-17", numberFormatter.format(pow(10, -17)));
        assertEquals("1E-18", numberFormatter.format(pow(10, -18)));
        assertEquals("1.5E-18", numberFormatter.format(1.5 * pow(10, -18)));
        assertEquals("1E-100", numberFormatter.format(pow(10, -100)));

        testSimpleFormat();
    }

    @Test
    public void testSimpleFormatWithRounding() throws Exception {
        numberFormatter.useSimpleFormat();
        numberFormatter.setPrecision(5);

        assertEquals("1", numberFormatter.format(1d));
        assertEquals("0", numberFormatter.format(pow(10, -6)));
        assertEquals("0.33333", numberFormatter.format(1d / 3));
        assertEquals("0", numberFormatter.format(pow(10, -18) / 3));
        assertEquals("1234567890000000000", numberFormatter.format(123456789 * pow(10, 10)));
        assertEquals("0", numberFormatter.format(pow(10, -16)));
        assertEquals("0", numberFormatter.format(pow(10, -17)));
        assertEquals("0", numberFormatter.format(pow(10, -18)));
        assertEquals("0", numberFormatter.format(1.5 * pow(10, -18)));
        assertEquals("0", numberFormatter.format(pow(10, -100)));

        testSimpleFormat();
    }

    // testing simple format with and without rounding
    private void testSimpleFormat() {
        assertEquals("0.00001", numberFormatter.format(pow(10, -5)));
        assertEquals("0.01", numberFormatter.format(3.11 - 3.1));

        assertEquals("100", numberFormatter.format(pow(10, 2)));
        assertEquals("1", numberFormatter.format(BigInteger.ONE));
        assertEquals("1000", numberFormatter.format(BigInteger.valueOf(1000)));

        assertEquals("1000000000000000000", numberFormatter.format(pow(10, 18)));
        assertEquals("1000000000000000000", numberFormatter.format(BigInteger.valueOf(10).pow(18)));

        assertEquals("1E19", numberFormatter.format(pow(10, 19)));
        assertEquals("1E19", numberFormatter.format(BigInteger.valueOf(10).pow(19)));

        assertEquals("1E20", numberFormatter.format(pow(10, 20)));
        assertEquals("1E20", numberFormatter.format(BigInteger.valueOf(10).pow(20)));

        assertEquals("1E100", numberFormatter.format(pow(10, 100)));
        assertEquals("1E100", numberFormatter.format(BigInteger.valueOf(10).pow(100)));

        assertEquals("0.01", numberFormatter.format(pow(10, -2)));

        assertEquals("5000000000000000000", numberFormatter.format(5000000000000000000d));
        assertEquals("5000000000000000000", numberFormatter.format(BigInteger.valueOf(5000000000000000000L)));

        assertEquals("5000000000000000000", numberFormatter.format(5000000000000000001d));
        assertEquals("5000000000000000001", numberFormatter.format(BigInteger.valueOf(5000000000000000001L)));

        assertEquals("5999999999999995000", numberFormatter.format(5999999999999994999d));
        assertEquals("5999999999999994999", numberFormatter.format(BigInteger.valueOf(5999999999999994999L)));

        assertEquals("5E19", numberFormatter.format(50000000000000000000d));
        assertEquals("5E19", numberFormatter.format(BigInteger.valueOf(5L).multiply(TEN.pow(19))));

        assertEquals("5E40", numberFormatter.format(50000000000000000000000000000000000000000d));
        assertEquals("5E40", numberFormatter.format(BigInteger.valueOf(5L).multiply(TEN.pow(40))));
    }

    // testing scientific format with and without rounding
    private void testScientificFormat() {
        assertEquals("0.00001", numberFormatter.format(pow(10, -5)));
        assertEquals("1E-6", numberFormatter.format(pow(10, -6)));

        assertEquals("100", numberFormatter.format(pow(10, 2)));
        assertEquals("100", numberFormatter.format(TEN.pow(2)));

        assertEquals("10000", numberFormatter.format(pow(10, 4)));
        assertEquals("10000", numberFormatter.format(TEN.pow(4)));

        assertEquals("1E5", numberFormatter.format(pow(10, 5)));
        assertEquals("1E5", numberFormatter.format(TEN.pow(5)));

        assertEquals("1E18", numberFormatter.format(pow(10, 18)));
        assertEquals("1E18", numberFormatter.format(TEN.pow(18)));

        assertEquals("1E19", numberFormatter.format(pow(10, 19)));
        assertEquals("1E19", numberFormatter.format(TEN.pow( 19)));

        assertEquals("1E20", numberFormatter.format(pow(10, 20)));
        assertEquals("1E20", numberFormatter.format(TEN.pow(20)));

        assertEquals("1E100", numberFormatter.format(pow(10, 100)));
        assertEquals("1E100", numberFormatter.format(TEN.pow(100)));

        assertEquals("0.01", numberFormatter.format(pow(10, -2)));
        assertEquals("1E-17", numberFormatter.format(pow(10, -17)));
        assertEquals("1E-18", numberFormatter.format(pow(10, -18)));
        assertEquals("1.5E-18", numberFormatter.format(1.5 * pow(10, -18)));
        assertEquals("1E-100", numberFormatter.format(pow(10, -100)));

        assertEquals("5E18", numberFormatter.format(5000000000000000000d));
        assertEquals("5E18", numberFormatter.format(5000000000000000001d));
        assertEquals("5E19", numberFormatter.format(50000000000000000000d));
        assertEquals("5E40", numberFormatter.format(50000000000000000000000000000000000000000d));
    }

    @Test
    public void testMaximumPrecision() throws Exception {
        numberFormatter.useSimpleFormat();
        numberFormatter.setPrecision(10);

        for (int i = 0; i < 1000; i++) {
            for (int j = 2; j < 1000; j += j - 1) {
                for (int k = 2; k < 1000; k += k - 1) {
                    final double first = makeDouble(i, j);
                    final double second = makeDouble(i, 1000 - k);
                    checkMaximumPrecision(first + "-" + second, numberFormatter.format(first - second));
                    checkMaximumPrecision(second + "-" + first, numberFormatter.format(second - first));
                    checkMaximumPrecision(second + "+" + first, numberFormatter.format(first + second));
                }
            }
        }
    }

    private void checkMaximumPrecision(String expression, CharSequence value) {
        assertTrue(expression + "=" + value, value.length() <= 8);
    }

    private static double makeDouble(int integerPart, int fractionalPart) {
        return Double.parseDouble(integerPart + "." + fractionalPart);
    }
}
