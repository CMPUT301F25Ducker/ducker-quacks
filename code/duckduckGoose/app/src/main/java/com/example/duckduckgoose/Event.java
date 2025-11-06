package com.example.duckduckgoose;

import android.util.Log;
import com.example.duckduckgoose.waitlist.WaitlistEntry;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class Event {
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
    private List<String> waitingList;             // user IDs waiting
    private List<String> acceptedFromWaitlist;    // user IDs accepted
    private List<String> registeredUsers;         // user IDs registered
    private int signupCount;                      // number of signed-up users

    // 🔹 Default no-arg constructor required for Firestore deserialization
    public Event() {
        this.waitingList = new ArrayList<>();
        this.acceptedFromWaitlist = new ArrayList<>();
        this.registeredUsers = new ArrayList<>();
        this.imagePaths = new ArrayList<>();
        this.signupCount = 0;
    }

    // 🔹 Full constructor for creating an event in code
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
        this.organizerId = organizerId;
        this.waitingList = new ArrayList<>();
        this.acceptedFromWaitlist = new ArrayList<>();
        this.registeredUsers = new ArrayList<>();
        this.signupCount = 0;
    }

    // ================================
    // Getters and setters
    // ================================
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

    public boolean isOnWaitingList(String userId) {
        return waitingList != null && waitingList.contains(userId);
    }

    public List<String> getAcceptedFromWaitlist() {
        return acceptedFromWaitlist != null ? acceptedFromWaitlist : new ArrayList<>();
    }

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

    public boolean hasAcceptedFromWaitlist(String userId) {
        return acceptedFromWaitlist != null && acceptedFromWaitlist.contains(userId);
    }

    public int getSignupCount() {
        return signupCount;
    }

    public void addRegisteredUser(String userId) {
        if (registeredUsers == null) registeredUsers = new ArrayList<>();
        if (!registeredUsers.contains(userId)) {
            registeredUsers.add(userId);
            signupCount++;
        }
    }

    public void removeRegisteredUser(String userId) {
        if (registeredUsers != null && registeredUsers.contains(userId)) {
            registeredUsers.remove(userId);
            signupCount--;
        }
    }

    public boolean isRegistered(String userId) {
        return registeredUsers != null && registeredUsers.contains(userId);
    }
}
