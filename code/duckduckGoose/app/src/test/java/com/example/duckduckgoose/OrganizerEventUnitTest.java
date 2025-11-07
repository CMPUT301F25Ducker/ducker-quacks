package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Unit tests for Event model and organizer-related logic, this will just verify that our code works
 * as intended and does not have any undefined behaviour inside variables and what have you.
 */
public class OrganizerEventUnitTest {

    private Event event;

    @Before
    public void setUp() {
        event = new Event();
    }

    // Test event has required fields
    @Test
    public void testEventHasName() {
        Event e = new Event("event1", "Test Event", "2024-12-01", "2024-11-01", "2024-11-30", "100", "$10", false, null);
        assertEquals("Test Event", e.getName());
    }

    // Test event ID is set correctly
    @Test
    public void testEventIdIsSet() {
        Event e = new Event();
        e.setEventId("test123");
        assertEquals("test123", e.getEventId());
    }

    // Test organizer ID is stored
    @Test
    public void testOrganizerIdIsSet() {
        Event e = new Event();
        e.setOrganizerId("org123");
        assertEquals("org123", e.getOrganizerId());
    }

    // Test event date is stored
    @Test
    public void testEventDateIsStored() {
        Event e = new Event("1", "Event", "2024-12-01", null, null, null, null, false, null);
        assertEquals("2024-12-01", e.getEventDate());
    }

    // Test registration dates
    @Test
    public void testRegistrationDates() {
        Event e = new Event("1", "Event", null, "2024-11-01", "2024-11-30", null, null, false, null);
        assertEquals("2024-11-01", e.getRegistrationOpens());
        assertEquals("2024-11-30", e.getRegistrationCloses()); // quick lil addition
    }

    // Test event cost
    @Test
    public void testEventCost() {
        Event e = new Event("1", "Event", null, null, null, null, "$25", false, null);
        assertEquals("$25", e.getCost());
    }

    // Test max spots
    @Test
    public void testMaxSpots() {
        Event e = new Event("1", "Event", null, null, null, "50", null, false, null);
        assertEquals("50", e.getMaxSpots());
    }

    // Test geolocation is disabled by default
    @Test
    public void testGeolocationDisabledByDefault() {
        Event e = new Event("1", "Event", null, null, null, null, null, false, null);
        assertFalse(e.isGeolocationEnabled());
    }

    // Test geolocation can be enabled
    @Test
    public void testGeolocationCanBeEnabled() {
        Event e = new Event("1", "Event", null, null, null, null, null, true, null);
        assertTrue(e.isGeolocationEnabled());
    } // this was able to be enabled or disabled but really rough in testing with an instrumented test suite

    // Test waiting list is empty initially
    @Test
    public void testWaitingListEmptyInitially() {
        Event e = new Event();
        assertNotNull(e.getWaitingList());
        assertEquals(0, e.getWaitingList().size());
    }

    // Test signup count starts at zero
    @Test
    public void testSignupCountStartsAtZero() {
        Event e = new Event();
        assertEquals(0, e.getSignupCount());
    }

    // Test AppConfig organizer mode
    @Test
    public void testAppConfigOrganizerMode() {
        AppConfig.setLoginMode("ORGANIZER");
        assertEquals("ORGANIZER", AppConfig.LOGIN_MODE);
    }

    // Test event name is not empty
    @Test
    public void testEventNameNotEmpty() {
        String name = "Summer Pool Party";
        assertFalse(name.isEmpty());
    }

    // Test empty event name
    @Test
    public void testEmptyEventName() {
        String name = "";
        assertTrue(name.isEmpty());
    }

    // Test event has image paths list
    @Test
    public void testEventHasImagePaths() {
        Event e = new Event();
        assertNotNull(e.getImagePaths());
    }
}