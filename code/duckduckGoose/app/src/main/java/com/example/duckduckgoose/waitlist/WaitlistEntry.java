package com.example.duckduckgoose.waitlist;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.Timestamp;

/**
 * Represents an entry in the waitlist collection, tracking the relationship between users and events
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

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getEventId() {
        return eventId;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
}