package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.duckduckgoose.user.User;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Administrator functionality. Tests admin operations like removing events,
 * profiles, and browsing, this will just verify that our code works as intended and does not have
 * any undefined behaviour inside variables and what have you.
 */
public class AdministratorUnitTest {

    private User admin;
    private Event event;
    private List<Event> eventList;
    private List<User> userList;

    @Before
    public void setUp() {
        admin = new User();
        admin.setAccountType("Admin");

        event = new Event();
        event.setEventId("event123");

        eventList = new ArrayList<>();
        userList = new ArrayList<>();
    }

    // Test event can be identified for removal
    @Test
    public void testEventHasId() {
        assertNotNull(event.getEventId());
        assertEquals("event123", event.getEventId());
    } // basically that the event exists somewhere in the system (stupid but it's there)

    // Test event list can be browsed
    @Test
    public void testEventListCanBeBrowsed() {
        Event e1 = new Event();
        e1.setEventId("event1");
        Event e2 = new Event();
        e2.setEventId("event2");

        eventList.add(e1);
        eventList.add(e2);

        assertEquals(2, eventList.size());
    } // just a primitive check here, that eventList size dynamically grows/truncates over time

    // Test event can be removed from list
    @Test
    public void testEventCanBeRemovedFromList() {
        Event e1 = new Event();
        eventList.add(e1);

        assertEquals(1, eventList.size());

        eventList.remove(e1);

        assertEquals(0, eventList.size());
    }

    // Test user profile has account type
    @Test
    public void testUserProfileHasAccountType() {
        User user = new User();
        user.setAccountType("Organizer");

        assertNotNull(user.getAccountType());
    }

    // Test organizer profile can be identified
    @Test
    public void testOrganizerProfileCanBeIdentified() {
        User organizer = new User();
        organizer.setAccountType("Organizer");

        assertEquals("Organizer", organizer.getAccountType());
    }

    // Test user list can be browsed
    @Test
    public void testUserListCanBeBrowsed() {
        User u1 = new User();
        u1.setFullName("User 1");
        User u2 = new User();
        u2.setFullName("User 2");

        userList.add(u1);

        assertEquals(1, userList.size());

        userList.add(u2);

        assertEquals(2, userList.size());
    }

    // Test user can be removed from list
    @Test
    public void testUserCanBeRemovedFromList() {
        User u1 = new User();
        userList.add(u1);

        assertEquals(1, userList.size());

        userList.remove(u1);

        assertEquals(0, userList.size());
    }

    // Test empty event list
    @Test
    public void testEmptyEventList() {
        List<Event> emptyList = new ArrayList<>(); // NULL at start yk
        assertEquals(0, emptyList.size());
    }

    // Test empty user list
    @Test
    public void testEmptyUserList() {
        List<User> emptyList = new ArrayList<>(); // NULL at start yk
        assertEquals(0, emptyList.size());
    }

    // Test event has organizer ID
    @Test
    public void testEventHasOrganizerId() {
        event.setOrganizerId("org123");
        assertEquals("org123", event.getOrganizerId());
    } // verifying getters and setters ops

    // Test filtering organizers from user list
    @Test
    public void testFilteringOrganizers() {
        User entrant = new User();
        entrant.setAccountType("Entrant");
        User organizer = new User();
        organizer.setAccountType("Organizer");

        userList.add(entrant);
        userList.add(organizer);

        List<User> organizers = new ArrayList<>();
        for (User u : userList) {
            if ("Organizer".equals(u.getAccountType())) {
                organizers.add(u);
            }
        }

        assertEquals(1, organizers.size());
    }

    // Test event name is not empty for browsing
    @Test
    public void testEventNameNotEmpty() {
        Event e = new Event("1", "Pool Party", null, null, null, null, null, false, null);
        assertNotNull(e.getName());
        assertFalse(e.getName().isEmpty());
    }

    // Test user full name for profile browsing
    @Test
    public void testUserFullNameForBrowsing() {
        User user = new User();
        user.setFullName("John Doe");

        assertNotNull(user.getFullName());
        assertEquals("John Doe", user.getFullName());
    }

    // Test multiple events can be browsed
    @Test
    public void testMultipleEventsCanBeBrowsed() {
        for (int i = 0; i < 5; i++) {
            Event e = new Event();
            e.setEventId("event" + i);
            eventList.add(e);
        }

        assertEquals(5, eventList.size());
    }

    // Test finding specific event by ID
    @Test
    public void testFindingSpecificEventById() {
        Event e1 = new Event();
        e1.setEventId("event1");
        Event e2 = new Event();
        e2.setEventId("event2");

        eventList.add(e1);
        eventList.add(e2);

        Event found = null;
        for (Event e : eventList) {
            if ("event2".equals(e.getEventId())) {
                found = e;
                break;
            }
        }

        assertNotNull(found);
        assertEquals("event2", found.getEventId());
    }

    // Test admin can identify policy violations
    @Test
    public void testAdminCanIdentifyAccountTypes() {
        User violator = new User();
        violator.setAccountType("Organizer");

        assertTrue("Organizer".equals(violator.getAccountType()));
    }

    // Test event waiting list size for review
    @Test
    public void testEventWaitingListSizeForReview() {
        Event e = new Event();
        assertNotNull(e.getWaitingList());
        assertEquals(0, e.getWaitingList().size());
    }
}