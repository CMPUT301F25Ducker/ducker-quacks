package com.example.duckduckgoose.waitlist;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.Timestamp;

/**
 * Represents an entry in the waitlist collection, tracking the relationship between users and events.
 * Each entry maintains the user's status in relation to a specific event's waitlist, including
 * timestamps for when they joined and when they were accepted (if applicable).
 *
 * @author DuckDuckGoose Development Team
 */
public class WaitlistEntry {
    private String userId;
    private String eventId;
    private Timestamp joinedAt;
    private String status; // "waiting", "accepted", "removed"
    private String eventName; // For easier querying/display
    private String userName; // For easier querying/display
    private Timestamp acceptedAt; // When the user was accepted (if applicable)
    private String notes; // Optional notes about the waitlist entry

    private Double latitude;
    private Double longitude;

    // Required no-arg constructor for Firestore
    public WaitlistEntry() {}

    public WaitlistEntry(String userId, String eventId) {
        this.userId = userId;
        this.eventId = eventId;
        this.joinedAt = Timestamp.now();
        this.status = "waiting";
        this.acceptedAt = null;
        this.notes = "";
    }

    public WaitlistEntry(String userId, String eventId, Double latitude, Double longitude) {
        this.userId = userId;
        this.eventId = eventId;
        this.joinedAt = Timestamp.now();
        this.status = "waiting";
        this.acceptedAt = null;
        this.notes = "";
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Gets the ID of the user who is on the waitlist.
     * @return The unique identifier of the user
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the ID of the event being waitlisted for.
     * @return The unique identifier of the event
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the timestamp when the user joined the waitlist.
     * @return Timestamp marking when the user joined
     */
    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    /**
     * Gets the current status of the waitlist entry.
     * @return Status string: "waiting", "accepted", or "removed"
     */
    public String getStatus() {
        return status;
    }

    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }

    /**
     * Sets the user ID for this waitlist entry.
     * @param userId The unique identifier of the user
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Sets the event ID for this waitlist entry.
     * @param eventId The unique identifier of the event
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Sets the timestamp when the user joined the waitlist.
     * @param joinedAt Timestamp marking when the user joined
     */
    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    /**
     * Updates the status of the waitlist entry.
     * @param status New status: "waiting", "accepted", or "removed"
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Sets the display name of the user for convenience.
     * @param userName The user's display name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Sets the display name of the event for convenience.
     * @param eventName The event's display name
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
