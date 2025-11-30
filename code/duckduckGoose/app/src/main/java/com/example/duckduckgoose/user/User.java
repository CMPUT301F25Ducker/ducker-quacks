/**
 * Data model representing an application user and their event participation status.
 *
 * Stores user identity, contact information, account type, and event-related lists
 * (waitlisted and accepted events). Provides helper methods to manage those lists.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for representing a user profile and event associations.
 *
 * Encapsulates basic profile information and tracks events that a user has been
 * waitlisted for or accepted into. Provides safe getters and utility methods
 * for list management.
 */
public class User {
    /** The user's unique identifier in the system. */
    private String userId;

    /** The user's full name. */
    private String fullName;

    /** The user's age. */
    private Long   age;

    /** The user's email address. */
    private String email;

    /** The user's phone number. */
    private String phone;

    /** The user's account type (e.g., attendee, organizer, admin). */
    private String accountType;

    /** Flag to indicate if user has new notifications. */
    private boolean new_notifications = false;

    /** Opt-in flag for receiving organizer/admin notifications. */
    private boolean receive_notifications = true;

    /** Timestamp when the user was created (in milliseconds). */
    private Long createdAt;

    /** List of event IDs the user is waitlisted for. */
    private List<String> waitlistedEventIds;

    /** List of event IDs the user has been accepted into (from waitlist). */
    private List<String> acceptedEventIds;

    /**
     * Default constructor initializing empty event lists.
     */
    public User() {
        waitlistedEventIds = new ArrayList<>(); // Initialize empty list in no-arg constructor
        acceptedEventIds = new ArrayList<>();
    }

    // --- Waitlist management ---

    /**
     * Returns the list of event IDs where the user is currently waitlisted.
     * If the internal list is null, returns an empty list.
     *
     * @return A non-null list of event IDs where the user is waitlisted
     */
    public List<String> getWaitlistedEventIds() {
        return waitlistedEventIds != null ? waitlistedEventIds : new ArrayList<>();
    }

    /**
     * Returns the list of event IDs where the user has been accepted.
     * If the internal list is null, returns an empty list.
     *
     * @return A non-null list of event IDs where the user has been accepted
     */
    public List<String> getAcceptedEventIds() {
        return acceptedEventIds != null ? acceptedEventIds : new ArrayList<>();
    }

    /**
     * Adds an event to the user's waitlist if it's not already present.
     * Initializes the waitlist if it doesn't exist.
     *
     * @param eventId The unique identifier of the event to add to waitlist
     */
    public void addToWaitlist(String eventId) {
        if (waitlistedEventIds == null) {
            waitlistedEventIds = new ArrayList<>();
        }
        if (!waitlistedEventIds.contains(eventId)) {
            waitlistedEventIds.add(eventId);
        }
    }

    /**
     * Removes an event from the user's waitlist.
     * Does nothing if the waitlist is null or the event is not in the list.
     *
     * @param eventId The unique identifier of the event to remove from waitlist
     */
    public void removeFromWaitlist(String eventId) {
        if (waitlistedEventIds != null) {
            waitlistedEventIds.remove(eventId);
        }
    }

    /**
     * Adds an event to the user's accepted list and removes it from the waitlist.
     * Initializes the accepted list if it doesn't exist.
     * Only adds the event if it's not already in the accepted list.
     *
     * @param eventId The unique identifier of the event to mark as accepted
     */
    public void addToAcceptedEvents(String eventId) {
        if (acceptedEventIds == null) {
            acceptedEventIds = new ArrayList<>();
        }
        if (!acceptedEventIds.contains(eventId)) {
            acceptedEventIds.add(eventId);
            // Remove from waitlist since now accepted
            removeFromWaitlist(eventId);
        }
    }

    /**
     * Removes an event from the user's accepted list.
     * Does nothing if the accepted list is null or the event is not in the list.
     *
     * @param eventId The unique identifier of the event to remove from accepted list
     */
    public void removeFromAcceptedEvents(String eventId) {
        if (acceptedEventIds != null) {
            acceptedEventIds.remove(eventId);
        }
    }

    // --- Getters ---

    /**
     * Returns the user's unique identifier.
     *
     * @return The user's ID in the system
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the user's full name.
     *
     * @return The user's complete name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Returns the user's age.
     *
     * @return The user's age as a Long value, may be null if not set
     */
    public Long getAge() {
        return age;
    }

    /**
     * Returns the user's email address.
     *
     * @return The user's email address as a string
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user's phone number.
     *
     * @return The user's phone number as a string
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Returns the user's account type (e.g., attendee, organizer, admin).
     *
     * @return The user's account type as a string
     */
    public String getAccountType() {
        return accountType;
    }

    // --- Setters ---

    /**
     * Sets the user's full name.
     *
     * @param fullName The new full name for the user
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Sets the user's age.
     *
     * @param age The user's age as a Long value
     */
    public void setAge(Long age) {
        this.age = age;
    }

    /**
     * Sets the user's email address.
     *
     * @param email The new email address for the user
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Sets the user's phone number.
     *
     * @param phone The new phone number for the user
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Sets the user's account type (e.g., "attendee", "organizer").
     *
     * @param accountType The account type label to assign to the user
     */
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    /**
     * Returns whether the user has new notifications.
     *
     * @return true if the user has new notifications, false otherwise
     */
    public boolean getNew_notifications() {
        return new_notifications;
    }

    /**
     * Sets the user's notification flag.
     *
     * @param new_notifications true if the user has new notifications, false otherwise
     */
    public void setNew_notifications(boolean new_notifications) {
        this.new_notifications = new_notifications;
    }

    /**
     * Returns whether the user has opted in to receive notifications from organizers/admins.
     * Defaults to true for backwards compatibility for existing users.
     *
     * @return true if the user wants to receive notifications, false otherwise
     */
    public boolean getReceive_notifications() {
        return receive_notifications;
    }

    /**
     * Sets the user's opt-in preference for administrative/organizer notifications.
     *
     * @param receive_notifications true to opt-in to notifications, false to opt-out
     */
    public void setReceive_notifications(boolean receive_notifications) {
        this.receive_notifications = receive_notifications;
    }

    /**
     * Returns the timestamp when the user was created.
     *
     * @return Timestamp of user creation in milliseconds
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when the user was created.
     *
     * @param createdAt Timestamp of user creation in milliseconds
     */
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Sets the user's unique identifier.
     *
     * @param userId The unique identifier to assign to the user
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
}