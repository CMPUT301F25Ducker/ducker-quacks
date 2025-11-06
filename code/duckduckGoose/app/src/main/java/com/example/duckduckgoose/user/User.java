//package com.example.duckduckgoose.user;
//
//public abstract class User {
//    protected String userId;
//    protected String fullName;
//    protected int age;
//    protected String email;
//    protected String phone;
//
//    public User(String userId, String fullName, int age, String email, String phone) {
//        this.userId = userId;
//        this.fullName = fullName;
//        this.age = age;
//        this.email = email;
//        this.phone = phone;
//    }
//
//    public abstract String userType();
//
//
//    // getters
//    public String getUserId() {
//        return userId;
//    }
//
//    public String getFullName() {
//        return fullName;
//    }
//
//    public int getAge() {
//        return age;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public String getPhone() {
//        return phone;
//    }
//
//
//    // setters
//    // no setter for userid bc we dont want to ever change it after creating the account
//    public void setFullName(String fullName) {
//        this.fullName = fullName;
//    }
//
//    public void setAge(int age) {
//        this.age = age;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    public void setPhone(String phone) {
//        this.phone = phone;
//    }
//
//}
//

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

    public User() {
        waitlistedEventIds = new ArrayList<>(); // Initialize empty list in no-arg constructor
        acceptedEventIds = new ArrayList<>();
    }

    // Waitlist management
    public List<String> getWaitlistedEventIds() {
        return waitlistedEventIds != null ? waitlistedEventIds : new ArrayList<>();
    }

    public List<String> getAcceptedEventIds() {
        return acceptedEventIds != null ? acceptedEventIds : new ArrayList<>();
    }

    public void addToWaitlist(String eventId) {
        if (waitlistedEventIds == null) {
            waitlistedEventIds = new ArrayList<>();
        }
        if (!waitlistedEventIds.contains(eventId)) {
            waitlistedEventIds.add(eventId);
        }
    }

    public void removeFromWaitlist(String eventId) {
        if (waitlistedEventIds != null) {
            waitlistedEventIds.remove(eventId);
        }
    }

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

    public void removeFromAcceptedEvents(String eventId) {
        if (acceptedEventIds != null) {
            acceptedEventIds.remove(eventId);
        }
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAccountType() {
        return accountType;
    }

    // Setters
    // No setter for userId to prevent changing it after creation

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setAge(Long age) {
        this.age = age;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

}
