package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.duckduckgoose.waitlist.WaitlistEntry;

import org.junit.Test;

public class WaitlistEntryUnitTest {

    @Test
    public void testDefaultConstructorSetsWaiting() {
        WaitlistEntry w = new WaitlistEntry("u1", "e1");
        assertEquals("u1", w.getUserId());
        assertEquals("e1", w.getEventId());
        assertNotNull(w.getJoinedAt());
        assertEquals("waiting", w.getStatus());
    }

    @Test
    public void testLatLonConstructor() {
        WaitlistEntry w = new WaitlistEntry("u2", "e2", 1.23, 4.56);
        assertEquals(Double.valueOf(1.23), w.getLatitude());
        assertEquals(Double.valueOf(4.56), w.getLongitude());
    }
}
