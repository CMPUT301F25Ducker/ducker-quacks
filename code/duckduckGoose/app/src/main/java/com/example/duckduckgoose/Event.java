/**
 * Model for an event with waitlist and registration helpers.
 *
 * Stores event metadata and provides methods to manage waitlist, accepted,
 * and registered user lists, syncing with Firestore as needed.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.util.Log;
import com.example.duckduckgoose.waitlist.WaitlistEntry;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Event model with Firestore-backed waitlist/registration operations.
 *
 * Exposes getters for event fields and utility methods to add/remove users
 * from waitlist, accepted, and registered lists.
 */
public class Event {
    /** Event identity and basic info. */
    private String eventId;
    private String name;
    private String eventDate;
    private String registrationOpens;
    private String registrationCloses;
    private String maxSpots;
    private String cost;
    private boolean geolocationEnabled;
    private List<String> imagePaths;
    private String organizerId;

    /** Enrollment lists maintained for the event. */
    private List<String> waitingList;             // user IDs waiting
    private List<String> acceptedFromWaitlist;    // user IDs accepted
    private List<String> registeredUsers;         // user IDs registered
    private int signupCount;                      // number of signed-up users

    /**
     * No-arg constructor for Firestore deserialization.
     */
    public Event() {
        this.waitingList = new ArrayList<>();
        this.acceptedFromWaitlist = new ArrayList<>();
        this.registeredUsers = new ArrayList<>();
        this.imagePaths = new ArrayList<>();
        this.signupCount = 0;
    }

    /**
     * Creates a new Event with supplied metadata.
     * 
     * @param eventId - Event identifier
     * @param name - Display name
     * @param eventDate - Human-readable date/period string
     * @param registrationOpens - When registration opens (string form)
     * @param registrationCloses - When registration closes (string form)
     * @param maxSpots - Maximum number of spots (string form)
     * @param cost - Cost string (e.g., "$10" or "Free")
     * @param geolocationEnabled - Whether location features are enabled
     * @param imagePaths - Optional image path list
     */
    public Event(String eventId,
                 String name,
                 String eventDate,
                 String registrationOpens,
                 String registrationCloses,
                 String maxSpots,
                 String cost,
                 boolean geolocationEnabled,
                 List<String> imagePaths) {

        this.eventId = eventId;
        this.name = name;
        this.eventDate = eventDate;
        this.registrationOpens = registrationOpens;
        this.registrationCloses = registrationCloses;
        this.maxSpots = maxSpots;
        this.cost = cost;
        this.geolocationEnabled = geolocationEnabled;
        this.imagePaths = imagePaths != null ? imagePaths : new ArrayList<>();
        this.waitingList = new ArrayList<>();
        this.acceptedFromWaitlist = new ArrayList<>();
        this.registeredUsers = new ArrayList<>();
        this.signupCount = 0;
    }

    // ================================
    // Getters and setters
    // ================================
    /**
     * Returns the unique event identifier.
     *
     * @return The event's unique identifier
     */
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getName() {
        return name;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getRegistrationOpens() {
        return registrationOpens;
    }

    public String getRegistrationCloses() {
        return registrationCloses;
    }

    public String getMaxSpots() {
        return maxSpots;
    }

    public String getCost() {
        return cost;
    }

    public boolean isGeolocationEnabled() {
        return geolocationEnabled;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public List<String> getWaitingList() {
        return waitingList != null ? waitingList : new ArrayList<>();
    }

    public List<String> getRegisteredUsers() {
        return registeredUsers != null ? registeredUsers : new ArrayList<>();
    }

    // ================================
    // Waitlist and registration logic
    // ================================
    /**
     * Adds a user to the waitlist and synchronizes the change with Firestore.
     * Updates both the event's waitlist and the user's waitlisted events.
     *
     * @param userId The unique identifier of the user to add to waitlist
     */
    public void addToWaitingList(String userId) {
        addToWaitingList(userId, null, null);
    }

    public void addToWaitingList(String userId, Double latitude, Double longitude) {
        if (waitingList.contains(userId)) return;

        waitingList.add(userId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        WaitlistEntry entry = new WaitlistEntry(userId, eventId, latitude, longitude);
        batch.set(db.collection("waitlist").document(userId + "_" + eventId), entry);

        batch.update(db.collection("events").document(eventId),
                "waitingList", FieldValue.arrayUnion(userId));

        batch.update(db.collection("users").document(userId),
                "waitlistedEventIds", FieldValue.arrayUnion(eventId));

        batch.commit().addOnFailureListener(e ->
                Log.e("Event", "Failed to add to waiting list", e)
        );
    }

    /**
     * Removes a user from the waitlist and synchronizes the change with Firestore.
     * Updates both the event's waitlist and the user's waitlisted events.
     *
     * @param userId The unique identifier of the user to remove from waitlist
     */
    public void removeFromWaitingList(String userId) {
        if (waitingList == null || !waitingList.contains(userId)) return;
        waitingList.remove(userId);
        if (eventId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        batch.update(db.collection("events").document(eventId),
                "waitingList", FieldValue.arrayRemove(userId));
        batch.update(db.collection("users").document(userId),
                "waitlistedEventIds", FieldValue.arrayRemove(eventId));
        batch.delete(db.collection("waitlist").document(userId + "_" + eventId));
        batch.commit().addOnFailureListener(e ->
                Log.e("Event", "Failed to remove from waiting list", e)
        );
    }

    /**
     * Checks if a user is currently on the waitlist for this event.
     *
     * @param userId The unique identifier of the user to check
     * @return true if the user is currently on the waitlist, false otherwise
     */
    public boolean isOnWaitingList(String userId) {
        return waitingList != null && waitingList.contains(userId);
    }

    /**
     * Returns the list of users who have been accepted from the waitlist.
     *
     * @return A non-null list of user IDs who have been accepted from the waitlist
     */
    public List<String> getAcceptedFromWaitlist() {
        return acceptedFromWaitlist != null ? acceptedFromWaitlist : new ArrayList<>();
    }

    /**
     * Accepts a user from the waitlist and updates all related records in Firestore.
     * Moves the user from waitlist state to accepted state, updating both event and user records.
     *
     * @param userId The unique identifier of the user to accept from waitlist
     */
    public void acceptFromWaitlist(String userId) {
        if (waitingList == null || !waitingList.contains(userId)) return;
        if (acceptedFromWaitlist == null) acceptedFromWaitlist = new ArrayList<>();

        waitingList.remove(userId);
        if (!acceptedFromWaitlist.contains(userId)) acceptedFromWaitlist.add(userId);

        if (eventId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        DocumentReference waitlistRef = db.collection("waitlist").document(userId + "_" + eventId);
        batch.update(waitlistRef, "status", "accepted");

        batch.update(db.collection("events").document(eventId),
                "waitingList", FieldValue.arrayRemove(userId),
                "acceptedFromWaitlist", FieldValue.arrayUnion(userId));

        batch.update(db.collection("users").document(userId),
                "waitlistedEventIds", FieldValue.arrayRemove(eventId),
                "acceptedEventIds", FieldValue.arrayUnion(eventId));

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d("Event", "User accepted from waitlist"))
                .addOnFailureListener(e -> Log.e("Event", "Failed to accept user", e));
    }

    /**
     * Removes a user from the accepted list and updates Firestore records.
     * Updates both the event's accepted list and the user's accepted events.
     *
     * @param userId The unique identifier of the user to remove from accepted list
     */
    public void removeFromAcceptedList(String userId) {
        if (acceptedFromWaitlist == null || !acceptedFromWaitlist.contains(userId)) return;
        acceptedFromWaitlist.remove(userId);
        if (eventId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        batch.update(db.collection("events").document(eventId),
                "acceptedFromWaitlist", FieldValue.arrayRemove(userId));
        batch.update(db.collection("users").document(userId),
                "acceptedEventIds", FieldValue.arrayRemove(eventId));

        batch.commit().addOnFailureListener(e ->
                Log.e("Event", "Failed to remove from accepted list", e)
        );
    }

    /**
     * Checks if a user has been accepted from the waitlist for this event.
     *
     * @param userId The unique identifier of the user to check
     * @return true if the user has been accepted from waitlist, false otherwise
     */
    public boolean hasAcceptedFromWaitlist(String userId) {
        return acceptedFromWaitlist != null && acceptedFromWaitlist.contains(userId);
    }

    /**
     * Returns the total number of users currently registered for this event.
     *
     * @return The current number of registered users
     */
    public int getSignupCount() {
        return signupCount;
    }

    /**
     * Adds a user to the registered list and increments the signup count.
     * Only adds the user if they are not already registered.
     *
     * @param userId The unique identifier of the user to register
     */
    public void addRegisteredUser(String userId) {
        if (registeredUsers == null) registeredUsers = new ArrayList<>();
        if (!registeredUsers.contains(userId)) {
            registeredUsers.add(userId);
            signupCount++;
        }
    }

    /**
     * Removes a user from the registered list and decrements the signup count.
     * Only affects count if the user was actually registered.
     *
     * @param userId The unique identifier of the user to unregister
     */
    public void removeRegisteredUser(String userId) {
        if (registeredUsers != null && registeredUsers.contains(userId)) {
            registeredUsers.remove(userId);
            signupCount--;
        }
    }

    /**
     * Checks if a user is currently registered for this event.
     *
     * @param userId The unique identifier of the user to check
     * @return true if the user is registered for this event, false otherwise
     */
    public boolean isRegistered(String userId) {
        return registeredUsers != null && registeredUsers.contains(userId);
    }
}
