package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.lang.reflect.Method;

public class MainActivityUnitTest {

    @Test
    public void testParseCostVariousInputs() throws Exception {
        Method parseCost = MainActivity.class.getDeclaredMethod("parseCost", String.class);
        parseCost.setAccessible(true);

        double v1 = (double) parseCost.invoke(null, (Object) null);
        assertEquals(Double.MAX_VALUE, v1, 0.0);

        double v2 = (double) parseCost.invoke(null, "Free");
        assertEquals(0.0, v2, 0.0);

        double v3 = (double) parseCost.invoke(null, "$25");
        assertEquals(25.0, v3, 0.0);

        double v4 = (double) parseCost.invoke(null, "€12.50");
        assertEquals(12.50, v4, 1e-6);

        double v5 = (double) parseCost.invoke(null, "$abc");
        assertEquals(Double.MAX_VALUE, v5, 0.0);
    }
}
