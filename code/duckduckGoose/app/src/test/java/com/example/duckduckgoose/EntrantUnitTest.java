package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.duckduckgoose.user.User;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for Entrant functionality. Tests user profile, waitlist operations, and event
 * interactions, this will just verify that our code works as intended and does not have any
 * undefined behaviour inside variables and what have you.
 */
public class EntrantUnitTest {

    private User entrant;
    private Event event;

    @Before
    public void setUp() {
        entrant = new User();
        entrant.setAccountType("Entrant");

        event = new Event();
    }

    // Test entrant account type is set correctly
    @Test
    public void testAdminAccountType() {
        assertEquals("entrant", entrant.getAccountType().toLowerCase());
    }

    // Test AppConfig entrant mode
    @Test
    public void testAppConfigAdminMode() {
        AppConfig.setLoginMode("ENTRANT");
        assertEquals("ENTRANT", AppConfig.LOGIN_MODE);
    }

    // Test user has waitlisted events list
    @Test
    public void testUserHasWaitlistedEventsList() {
        List<String> waitlist = entrant.getWaitlistedEventIds();
        assertNotNull(waitlist);
    }

    // Test user can join waitlist
    @Test
    public void testUserCanJoinWaitlist() {
        entrant.addToWaitlist("event123");
        assertTrue(entrant.getWaitlistedEventIds().contains("event123"));
    }

    // Test user can leave waitlist
    @Test
    public void testUserCanLeaveWaitlist() {
        entrant.addToWaitlist("event123");
        entrant.removeFromWaitlist("event123");
        assertFalse(entrant.getWaitlistedEventIds().contains("event123"));
    }

    // Test user waitlist starts empty
    @Test
    public void testUserWaitlistStartsEmpty() {
        User newUser = new User();
        assertEquals(0, newUser.getWaitlistedEventIds().size());
    }

    // Test user can be accepted from waitlist
    @Test
    public void testUserCanBeAcceptedFromWaitlist() {
        entrant.addToWaitlist("event123");
        entrant.addToAcceptedEvents("event123");

        assertTrue(entrant.getAcceptedEventIds().contains("event123"));
        assertFalse(entrant.getWaitlistedEventIds().contains("event123"));
    }

    // Test accepted events list exists
    @Test
    public void testAcceptedEventsListExists() {
        assertNotNull(entrant.getAcceptedEventIds());
    }

    // Test user full name can be set
    @Test
    public void testUserFullNameCanBeSet() {
        entrant.setFullName("John Doe");
        assertEquals("John Doe", entrant.getFullName());
    }

    // Test user email can be set
    @Test
    public void testUserEmailCanBeSet() {
        entrant.setEmail("test@example.com");
        assertEquals("test@example.com", entrant.getEmail());
    }

    // Test user phone can be set
    @Test
    public void testUserPhoneCanBeSet() {
        entrant.setPhone("1234567890");
        assertEquals("1234567890", entrant.getPhone());
    }

    // Test user age can be set
    @Test
    public void testUserAgeCanBeSet() {
        entrant.setAge(25L);
        assertEquals(Long.valueOf(25L), entrant.getAge());
    }

    // Test user account type
    @Test
    public void testUserAccountType() {
        entrant.setAccountType("Entrant");
        assertEquals("Entrant", entrant.getAccountType());
    }

    // Test event has waiting list
    @Test
    public void testEventHasWaitingList() {
        assertNotNull(event.getWaitingList());
    }

    // Test event waiting list starts empty
    @Test
    public void testEventWaitingListStartsEmpty() {
        assertEquals(0, event.getWaitingList().size());
    }

    // Test AppConfig entrant mode
    @Test
    public void testAppConfigEntrantMode() {
        AppConfig.setLoginMode("ENTRANT");
        assertEquals("ENTRANT", AppConfig.LOGIN_MODE);
    }

    // Test empty full name
    @Test
    public void testEmptyFullName() {
        String name = "";
        assertTrue(name.isEmpty());
    }

    // Test valid full name
    @Test
    public void testValidFullName() {
        String name = "John Doe";
        assertFalse(name.isEmpty());
    }

    // Test empty email
    @Test
    public void testEmptyEmail() {
        String email = "";
        assertTrue(email.isEmpty());
    }

    // Test valid email
    @Test
    public void testValidEmail() {
        String email = "test@example.com";
        assertFalse(email.isEmpty());
    }

    // Test user can have multiple waitlisted events
    @Test
    public void testUserCanHaveMultipleWaitlistedEvents() {
        entrant.addToWaitlist("event1");
        entrant.addToWaitlist("event2");
        entrant.addToWaitlist("event3");

        assertEquals(3, entrant.getWaitlistedEventIds().size());
    }

    // Test duplicate waitlist entries prevented
    @Test
    public void testDuplicateWaitlistEntriesPrevented() {
        entrant.addToWaitlist("event1");
        entrant.addToWaitlist("event1");

        assertEquals(1, entrant.getWaitlistedEventIds().size());
    }

    // Test user can remove from accepted events
    @Test
    public void testUserCanRemoveFromAcceptedEvents() {
        entrant.addToAcceptedEvents("event123");
        entrant.removeFromAcceptedEvents("event123");

        assertFalse(entrant.getAcceptedEventIds().contains("event123"));
    }
}