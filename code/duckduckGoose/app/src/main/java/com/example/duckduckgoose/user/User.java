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
