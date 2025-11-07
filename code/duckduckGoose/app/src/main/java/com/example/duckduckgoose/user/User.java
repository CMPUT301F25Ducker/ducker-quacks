/**
 * @file User.java
 * @brief Data model representing an application user and their event participation status.
 *
 * Stores user identity, contact information, account type, and event-related lists
 * (waitlisted and accepted events). Provides helper methods to manage those lists.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose.user;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String fullName;
    private Long   age;
    private String email;
    private String phone;
    private String accountType;
    private List<String> waitlistedEventIds; // List of event IDs the user is waitlisted for
    private List<String> acceptedEventIds; // List of event IDs the user has been accepted into (from waitlist)

    /**
     * @class User
     * @brief Model class for representing a user profile and event associations.
     *
     * Encapsulates basic profile information and tracks events that a user has been
     * waitlisted for or accepted into. Provides safe getters and utility methods
     * for list management.
     */
    public User() {
        waitlistedEventIds = new ArrayList<>(); // Initialize empty list in no-arg constructor
        acceptedEventIds = new ArrayList<>();
    }

    // --- Waitlist management ---

    /**
     * @brief Returns the list of event IDs where the user is waitlisted.
     * @return Non-null list of waitlisted event IDs.
     */
    public List<String> getWaitlistedEventIds() {
        return waitlistedEventIds != null ? waitlistedEventIds : new ArrayList<>();
    }

    /**
     * @brief Returns the list of event IDs the user has been accepted into.
     * @return Non-null list of accepted event IDs.
     */
    public List<String> getAcceptedEventIds() {
        return acceptedEventIds != null ? acceptedEventIds : new ArrayList<>();
    }

    /**
     * @brief Adds an event to the user's waitlist if not already present.
     * @param eventId Unique identifier of the event to add.
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
     * @brief Removes an event from the user's waitlist.
     * @param eventId Unique identifier of the event to remove.
     */
    public void removeFromWaitlist(String eventId) {
        if (waitlistedEventIds != null) {
            waitlistedEventIds.remove(eventId);
        }
    }

    /**
     * @brief Adds an event to the user's accepted list and removes it from the waitlist.
     * @param eventId Unique identifier of the event accepted.
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
     * @brief Removes an event from the user's accepted list.
     * @param eventId Unique identifier of the event to remove.
     */
    public void removeFromAcceptedEvents(String eventId) {
        if (acceptedEventIds != null) {
            acceptedEventIds.remove(eventId);
        }
    }

    // --- Getters ---

    /**
     * @brief Returns the user ID.
     * @return User's unique identifier.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @brief Returns the user's full name.
     * @return Full name string.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * @brief Returns the user's age.
     * @return Age as a Long, may be null.
     */
    public Long getAge() {
        return age;
    }

    /**
     * @brief Returns the user's email address.
     * @return Email address string.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @brief Returns the user's phone number.
     * @return Phone number string.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @brief Returns the user's account type (e.g., attendee, organizer, admin).
     * @return Account type string.
     */
    public String getAccountType() {
        return accountType;
    }

    // --- Setters ---

    /**
     * @brief Sets the user's full name.
     * @param fullName New full name.
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * @brief Sets the user's age.
     * @param age Age as a Long.
     */
    public void setAge(Long age) {
        this.age = age;
    }

    /**
     * @brief Sets the user's email address.
     * @param email New email address.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @brief Sets the user's phone number.
     * @param phone New phone number.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @brief Sets the user's account type (e.g., "attendee", "organizer").
     * @param accountType Account type label.
     */
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
