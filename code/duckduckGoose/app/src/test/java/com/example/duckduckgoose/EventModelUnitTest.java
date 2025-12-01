package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the Event model's in-memory behaviours (no Firestore).
 */
public class EventModelUnitTest {

    @Test
    public void testWaitingListAddRemove() {
        Event e = new Event();
        // initially empty
        assertFalse(e.isOnWaitingList("user1"));

        // add to waiting list (in-memory)
        e.getWaitingList().add("user1");
        assertTrue(e.isOnWaitingList("user1"));

        // remove from waiting list
        e.removeFromWaitingList("user1");
        assertFalse(e.isOnWaitingList("user1"));
    }

    @Test
    public void testAcceptFromWaitlistUpdatesLists() {
        Event e = new Event();
        e.getWaitingList().add("u1");
        assertTrue(e.isOnWaitingList("u1"));

        // accept in-memory (eventId is null so no Firestore calls)
        e.acceptFromWaitlist("u1");

        assertFalse(e.isOnWaitingList("u1"));
        assertTrue(e.hasAcceptedFromWaitlist("u1"));
    }

    @Test
    public void testRegisteredUsersCount() {
        Event e = new Event();
        assertEquals(0, e.getSignupCount());
        e.addRegisteredUser("r1");
        assertTrue(e.isRegistered("r1"));
        assertEquals(1, e.getSignupCount());
        e.removeRegisteredUser("r1");
        assertFalse(e.isRegistered("r1"));
        assertEquals(0, e.getSignupCount());
    }
}
