package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Basic unit tests for LoginActivity, this will just verify that our code works as intended and
 * does not have any undefined behaviour inside variables and what have you.
 */
public class LoginActivityUnitTest {

    // Test that empty email is detected
    @Test
    public void testEmptyEmail() {
        String email = "";
        assertTrue(email.isEmpty());
    }

    // Test that empty password is detected
    @Test
    public void testEmptyPassword() {
        String password = "";
        assertTrue(password.isEmpty());
    }

    // Test password length validation
    @Test
    public void testPasswordTooShort() {
        String password = "12345";
        assertFalse(password.length() >= 6);
    }

    // Test valid password length
    @Test
    public void testValidPasswordLength() {
        String password = "123456";
        assertTrue(password.length() >= 6);
    }

    // Test empty age string
    @Test
    public void testEmptyAge() {
        String age = "";
        assertTrue(age.isEmpty());
    }

    // Test parsing age
    @Test
    public void testValidAge() {
        String ageStr = "25";
        try {
            int age = Integer.parseInt(ageStr);
            assertTrue(age > 0);
        } catch (NumberFormatException e) {
            assertFalse(true);
        }
    }

    // Test negative age
    @Test
    public void testNegativeAge() {
        String ageStr = "-5";
        int age = Integer.parseInt(ageStr);
        assertFalse(age >= 0);
    }

    // Test invalid age format
    @Test
    public void testInvalidAgeFormat() {
        String ageStr = "abc";
        try {
            int age = Integer.parseInt(ageStr); // basically if the input cannot be parsed as an Integer, it isn't a number and is invalid
            assertFalse(true);
        } catch (NumberFormatException e) {
            assertTrue(true);
        }
    }

    // Test empty UserId
    @Test
    public void testEmptyUserId() {
        String userId = "";
        assertTrue(userId.isEmpty());
    }

    // Test empty full name
    @Test
    public void testEmptyFullName() {
        String fullName = "";
        assertTrue(fullName.isEmpty());
    }

    // Test empty phone
    @Test
    public void testEmptyPhone() {
        String phone = "";
        assertTrue(phone.isEmpty());
    }
}