/**
 * @file Event.java
 * @brief Model for an event with waitlist and registration helpers.
 *
 * Stores event metadata and provides methods to manage waitlist, accepted,
 * and registered user lists, syncing with Firestore as needed.
 *
 * @author
 *      DuckDuckGoose Development Team
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
 * @class Event
 * @brief Event model with Firestore-backed waitlist/registration operations.
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
     * @brief No-arg constructor for Firestore deserialization.
     */
    public Event() {
        this.waitingList = new ArrayList<>();
        this.acceptedFromWaitlist = new ArrayList<>();
        this.registeredUsers = new ArrayList<>();
        this.imagePaths = new ArrayList<>();
        this.signupCount = 0;
    }

    /**
     * @brief Creates a new Event with supplied metadata.
     * @param eventId Event identifier.
     * @param name Display name.
     * @param eventDate Human-readable date/period string.
     * @param registrationOpens When registration opens (string form).
     * @param registrationCloses When registration closes (string form).
     * @param maxSpots Maximum number of spots (string form).
     * @param cost Cost string (e.g., "$10" or "Free").
     * @param geolocationEnabled Whether location features are enabled.
     * @param imagePaths Optional image path list.
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
     * @brief Accessors for event fields.
     * @return Field values or empty lists where applicable.
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
     * @brief Adds a user to the waitlist and syncs Firestore.
     * @param userId The user to add.
     */
    public void addToWaitingList(String userId) {
        if (waitingList == null) waitingList = new ArrayList<>();
        if (waitingList.contains(userId)) return;

        waitingList.add(userId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        WaitlistEntry entry = new WaitlistEntry(userId, eventId);
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
     * @brief Removes a user from the waitlist and syncs Firestore.
     * @param userId The user to remove.
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

        batch.commit().addOnFailureListener(e ->
                Log.e("Event", "Failed to remove from waiting list", e)
        );
    }

    /**
     * @brief Checks if a user is on the waitlist.
     * @param userId Target user id.
     * @return true if the user is currently waitlisted.
     */
    public boolean isOnWaitingList(String userId) {
        return waitingList != null && waitingList.contains(userId);
    }

    /**
     * @brief Returns the list of users accepted from the waitlist.
     * @return Non-null list of accepted user IDs.
     */
    public List<String> getAcceptedFromWaitlist() {
        return acceptedFromWaitlist != null ? acceptedFromWaitlist : new ArrayList<>();
    }

    /**
     * @brief Accepts a user from the waitlist and updates Firestore.
     * Moves the user from waitlist to accepted state.
     * @param userId The user to accept.
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
     * @brief Removes a user from the accepted list and updates Firestore.
     * @param userId The user to remove from accepted.
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
     * @brief Checks if a user is in the accepted-from-waitlist list.
     * @param userId Target user id.
     * @return true if accepted.
     */
    public boolean hasAcceptedFromWaitlist(String userId) {
        return acceptedFromWaitlist != null && acceptedFromWaitlist.contains(userId);
    }

    /**
     * @brief Returns the number of registered users.
     * @return Current signup count.
     */
    public int getSignupCount() {
        return signupCount;
    }

    /**
     * @brief Adds a user to the registered list and increments count.
     * @param userId The user to add.
     */
    public void addRegisteredUser(String userId) {
        if (registeredUsers == null) registeredUsers = new ArrayList<>();
        if (!registeredUsers.contains(userId)) {
            registeredUsers.add(userId);
            signupCount++;
        }
    }

    /**
     * @brief Removes a user from the registered list and decrements count.
     * @param userId The user to remove.
     */
    public void removeRegisteredUser(String userId) {
        if (registeredUsers != null && registeredUsers.contains(userId)) {
            registeredUsers.remove(userId);
            signupCount--;
        }
    }

    /**
     * @brief Checks if a user is registered for this event.
     * @param userId Target user id.
     * @return true if registered.
     */
    public boolean isRegistered(String userId) {
        return registeredUsers != null && registeredUsers.contains(userId);
    }
}
