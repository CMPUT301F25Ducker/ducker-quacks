package com.example.duckduckgoose.user;

import com.google.firebase.Timestamp;

public class User {
    private String userId;
    private String fullName;
    private Long   age;
    private String email;
    private String phone;
    private String accountType;
//    private Long createdAt;

    public User() {} // firestore needs no-arg ctor

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
