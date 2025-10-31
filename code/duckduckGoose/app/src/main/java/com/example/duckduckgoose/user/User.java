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
    public String userId;
    public String fullName;
    public Long   age;
    public String email;
    public String phone;
    public String accountType;
//    public Long createdAt;

    public User() {} // firestore needs no-arg ctor
}
