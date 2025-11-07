/**
 * Represents an organizer user with a list of managed events.
 *
 * Extends {@link User} and maintains a list of {@link Event} objects
 * that belong to the organizer. Provides methods to access and count events.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose.user;

import com.example.duckduckgoose.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized user who manages one or more events.
 *
 * The Organizer class inherits from {@link User} and includes
 * additional functionality for handling events that the organizer
 * has created or is responsible for.
 */
public class Organizer extends User {
    /** List of events created or managed by the organizer. */
    private List<Event> events;

    /**
     * Default constructor initializes an empty event list.
     */
    public Organizer() {
        super();
        events = new ArrayList<>();
    }

    /**
     * Returns the list of events managed by the organizer.
     *
     * @return List of {@link Event} objects managed by this organizer
     */
    public List<Event> getEvents() {
        return events;
    }

    /**
     * Returns the number of events the organizer manages.
     *
     * @return The total number of events in the organizer's event list
     */
    public int getEventCount() {
        return events.size();
    }
}
