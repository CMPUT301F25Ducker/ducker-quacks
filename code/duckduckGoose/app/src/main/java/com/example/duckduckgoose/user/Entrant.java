package com.example.duckduckgoose.user;

public class Entrant extends User {

    public Entrant(String userId, String fullName, int age, String email, String phone) {
        super(userId, fullName, age, email, phone);
    }

    @Override
    public String userType() {
        return "entrant";
    }

}
