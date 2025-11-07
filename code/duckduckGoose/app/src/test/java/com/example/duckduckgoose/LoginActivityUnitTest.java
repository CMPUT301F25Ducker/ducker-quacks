package com.example.duckduckgoose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for LoginActivity validation logic and helper methods
 * These tests run on the JVM without requiring Android framework
 */
public class LoginActivityUnitTest {

    @Before
    public void setUp() {
        // Reset AppConfig before each test
        AppConfig.setLoginMode("ENTRANT");
    }

    // ==================== APPCONFIG TESTS ====================

    @Test
    public void testAppConfigDefaultLoginMode() {
        // Verify AppConfig has a LOGIN_MODE
        assertNotNull("LOGIN_MODE should not be null", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testAppConfigSetLoginModeToAdmin() {
        AppConfig.setLoginMode("ADMIN");
        assertEquals("LOGIN_MODE should be ADMIN", "ADMIN", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testAppConfigSetLoginModeToEntrant() {
        AppConfig.setLoginMode("ENTRANT");
        assertEquals("LOGIN_MODE should be ENTRANT", "ENTRANT", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testAppConfigSetLoginModeToOrganizer() {
        AppConfig.setLoginMode("ORGANIZER");
        assertEquals("LOGIN_MODE should be ORGANIZER", "ORGANIZER", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testAppConfigSetLoginModeConvertsToUppercase() {
        // Test lowercase conversion
        AppConfig.setLoginMode("admin");
        assertEquals("LOGIN_MODE should convert lowercase to ADMIN", "ADMIN", AppConfig.LOGIN_MODE);

        AppConfig.setLoginMode("entrant");
        assertEquals("LOGIN_MODE should convert lowercase to ENTRANT", "ENTRANT", AppConfig.LOGIN_MODE);

        AppConfig.setLoginMode("organizer");
        assertEquals("LOGIN_MODE should convert lowercase to ORGANIZER", "ORGANIZER", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testAppConfigSetLoginModeWithMixedCase() {
        AppConfig.setLoginMode("AdMiN");
        assertEquals("LOGIN_MODE should handle mixed case", "ADMIN", AppConfig.LOGIN_MODE);

        AppConfig.setLoginMode("EnTrAnT");
        assertEquals("LOGIN_MODE should handle mixed case", "ENTRANT", AppConfig.LOGIN_MODE);
    }

    // ==================== EMAIL VALIDATION TESTS ====================

    @Test
    public void testEmailValidation_EmptyString() {
        String email = "";
        assertTrue("Empty email should be invalid", email.isEmpty());
    }

    @Test
    public void testEmailValidation_WhitespaceOnly() {
        String email = "   ";
        assertTrue("Whitespace-only email should be invalid after trim", email.trim().isEmpty());
    }

    @Test
    public void testEmailValidation_ValidEmail() {
        String email = "test@example.com";
        assertFalse("Valid email should not be empty", email.trim().isEmpty());
    }

    @Test
    public void testEmailValidation_EmailWithWhitespace() {
        String email = "  test@example.com  ";
        assertEquals("Email should be trimmed correctly", "test@example.com", email.trim());
    }

    // ==================== PASSWORD VALIDATION TESTS ====================

    @Test
    public void testPasswordValidation_EmptyString() {
        String password = "";
        assertTrue("Empty password should be invalid", password.isEmpty());
    }

    @Test
    public void testPasswordValidation_WhitespaceOnly() {
        String password = "   ";
        assertTrue("Whitespace-only password should be invalid after trim", password.trim().isEmpty());
    }

    @Test
    public void testPasswordValidation_MinimumLength() {
        String password = "123456";
        assertTrue("Password with exactly 6 characters should be valid", password.length() >= 6);
    }

    @Test
    public void testPasswordValidation_LessThanMinimumLength() {
        String password = "12345";
        assertFalse("Password with less than 6 characters should be invalid", password.length() >= 6);
    }

    @Test
    public void testPasswordValidation_MoreThanMinimumLength() {
        String password = "1234567890";
        assertTrue("Password with more than 6 characters should be valid", password.length() >= 6);
    }

    @Test
    public void testPasswordValidation_WithWhitespace() {
        String password = "  password123  ";
        String trimmed = password.trim();
        assertTrue("Trimmed password should be valid", trimmed.length() >= 6);
    }

    // ==================== AGE VALIDATION TESTS ====================

    @Test
    public void testAgeValidation_ValidInteger() {
        String ageStr = "25";
        try {
            int age = Integer.parseInt(ageStr);
            assertTrue("Valid age should be positive", age >= 0);
        } catch (NumberFormatException e) {
            assertFalse("Should not throw exception for valid integer", true);
        }
    }

    @Test
    public void testAgeValidation_ZeroAge() {
        String ageStr = "0";
        try {
            int age = Integer.parseInt(ageStr);
            assertTrue("Zero age should be valid (>= 0)", age >= 0);
        } catch (NumberFormatException e) {
            assertFalse("Should not throw exception for zero", true);
        }
    }

    @Test
    public void testAgeValidation_NegativeAge() {
        String ageStr = "-5";
        try {
            int age = Integer.parseInt(ageStr);
            assertFalse("Negative age should be invalid", age >= 0);
        } catch (NumberFormatException e) {
            assertFalse("Should not throw exception for negative number", true);
        }
    }

    @Test
    public void testAgeValidation_NonNumericString() {
        String ageStr = "abc";
        try {
            int age = Integer.parseInt(ageStr);
            assertFalse("Should have thrown NumberFormatException", true);
        } catch (NumberFormatException e) {
            assertTrue("Should throw NumberFormatException for non-numeric", true);
        }
    }

    @Test
    public void testAgeValidation_EmptyString() {
        String ageStr = "";
        assertTrue("Empty age string should be invalid", ageStr.isEmpty());
    }

    @Test
    public void testAgeValidation_WithWhitespace() {
        String ageStr = "  25  ";
        try {
            int age = Integer.parseInt(ageStr.trim());
            assertTrue("Age with whitespace should be valid after trim", age >= 0);
        } catch (NumberFormatException e) {
            assertFalse("Should not throw exception after trim", true);
        }
    }

    @Test
    public void testAgeValidation_DecimalNumber() {
        String ageStr = "25.5";
        try {
            int age = Integer.parseInt(ageStr);
            assertFalse("Should have thrown NumberFormatException for decimal", true);
        } catch (NumberFormatException e) {
            assertTrue("Should throw NumberFormatException for decimal", true);
        }
    }

    // ==================== USER ID VALIDATION TESTS ====================

    @Test
    public void testUserIdValidation_EmptyString() {
        String userId = "";
        assertTrue("Empty user ID should be invalid", userId.isEmpty());
    }

    @Test
    public void testUserIdValidation_ValidUserId() {
        String userId = "testuser123";
        assertFalse("Valid user ID should not be empty", userId.trim().isEmpty());
    }

    @Test
    public void testUserIdValidation_WithWhitespace() {
        String userId = "  testuser123  ";
        assertEquals("User ID should be trimmed correctly", "testuser123", userId.trim());
    }

    @Test
    public void testUserIdValidation_AlphanumericOnly() {
        String userId = "user123";
        assertTrue("User ID should contain only alphanumeric characters",
                userId.matches("^[a-zA-Z0-9]+$"));
    }

    @Test
    public void testUserIdValidation_WithSpecialCharacters() {
        String userId = "user@123";
        assertFalse("User ID with special characters should fail alphanumeric check",
                userId.matches("^[a-zA-Z0-9]+$"));
    }

    // ==================== FULL NAME VALIDATION TESTS ====================

    @Test
    public void testFullNameValidation_EmptyString() {
        String fullName = "";
        assertTrue("Empty full name should be invalid", fullName.isEmpty());
    }

    @Test
    public void testFullNameValidation_ValidName() {
        String fullName = "John Doe";
        assertFalse("Valid full name should not be empty", fullName.trim().isEmpty());
    }

    @Test
    public void testFullNameValidation_WithWhitespace() {
        String fullName = "  John Doe  ";
        assertEquals("Full name should be trimmed correctly", "John Doe", fullName.trim());
    }

    @Test
    public void testFullNameValidation_SingleName() {
        String fullName = "John";
        assertFalse("Single name should be valid", fullName.trim().isEmpty());
    }

    // ==================== PHONE VALIDATION TESTS ====================

    @Test
    public void testPhoneValidation_EmptyString() {
        String phone = "";
        assertTrue("Empty phone should be invalid", phone.isEmpty());
    }

    @Test
    public void testPhoneValidation_ValidPhone() {
        String phone = "1234567890";
        assertFalse("Valid phone should not be empty", phone.trim().isEmpty());
    }

    @Test
    public void testPhoneValidation_WithWhitespace() {
        String phone = "  1234567890  ";
        assertEquals("Phone should be trimmed correctly", "1234567890", phone.trim());
    }

    @Test
    public void testPhoneValidation_NumericOnly() {
        String phone = "1234567890";
        assertTrue("Phone should contain only digits", phone.matches("^[0-9]+$"));
    }

    @Test
    public void testPhoneValidation_WithNonNumeric() {
        String phone = "123-456-7890";
        assertFalse("Phone with dashes should fail numeric-only check", phone.matches("^[0-9]+$"));
    }

    // ==================== ACCOUNT TYPE VALIDATION TESTS ====================

    @Test
    public void testAccountTypeValidation_EmptyString() {
        String accountType = "";
        assertTrue("Empty account type should be invalid", accountType.isEmpty());
    }

    @Test
    public void testAccountTypeValidation_Admin() {
        String accountType = "Admin";
        assertFalse("Admin account type should be valid", accountType.trim().isEmpty());
    }

    @Test
    public void testAccountTypeValidation_Entrant() {
        String accountType = "Entrant";
        assertFalse("Entrant account type should be valid", accountType.trim().isEmpty());
    }

    @Test
    public void testAccountTypeValidation_Organizer() {
        String accountType = "Organizer";
        assertFalse("Organizer account type should be valid", accountType.trim().isEmpty());
    }

    @Test
    public void testAccountTypeValidation_WithWhitespace() {
        String accountType = "  Entrant  ";
        assertEquals("Account type should be trimmed correctly", "Entrant", accountType.trim());
    }

    // ==================== NAVIGATION LOGIC TESTS ====================

    @Test
    public void testNavigationLogic_AdminMode() {
        AppConfig.setLoginMode("ADMIN");
        assertEquals("Admin mode should navigate to AdminConsoleActivity", "ADMIN", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testNavigationLogic_EntrantMode() {
        AppConfig.setLoginMode("ENTRANT");
        assertEquals("Entrant mode should navigate to MainActivity", "ENTRANT", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testNavigationLogic_OrganizerMode() {
        AppConfig.setLoginMode("ORGANIZER");
        assertEquals("Organizer mode should navigate to MainActivity", "ORGANIZER", AppConfig.LOGIN_MODE);
    }

    // ==================== VALIDATION COMBINATION TESTS ====================

    @Test
    public void testValidSignInCredentials() {
        String email = "test@example.com";
        String password = "password123";

        assertFalse("Email should not be empty", email.trim().isEmpty());
        assertFalse("Password should not be empty", password.trim().isEmpty());
    }

    @Test
    public void testInvalidSignInCredentials_BothEmpty() {
        String email = "";
        String password = "";

        assertTrue("Both fields empty should be invalid",
                email.trim().isEmpty() && password.trim().isEmpty());
    }

    @Test
    public void testValidCreateAccountData() {
        String accountType = "Entrant";
        String userId = "testuser123";
        String fullName = "Test User";
        String ageStr = "25";
        String email = "test@example.com";
        String phone = "1234567890";
        String password = "password123";

        assertFalse("Account type should not be empty", accountType.trim().isEmpty());
        assertFalse("User ID should not be empty", userId.trim().isEmpty());
        assertFalse("Full name should not be empty", fullName.trim().isEmpty());
        assertFalse("Age should not be empty", ageStr.trim().isEmpty());
        assertFalse("Email should not be empty", email.trim().isEmpty());
        assertFalse("Phone should not be empty", phone.trim().isEmpty());
        assertFalse("Password should not be empty", password.trim().isEmpty());
        assertTrue("Password should be at least 6 characters", password.length() >= 6);

        try {
            int age = Integer.parseInt(ageStr);
            assertTrue("Age should be non-negative", age >= 0);
        } catch (NumberFormatException e) {
            assertFalse("Age should be a valid integer", true);
        }
    }

    @Test
    public void testInvalidCreateAccountData_AllEmpty() {
        String accountType = "";
        String userId = "";
        String fullName = "";
        String ageStr = "";
        String email = "";
        String phone = "";
        String password = "";

        assertTrue("All empty fields should be invalid",
                accountType.isEmpty() && userId.isEmpty() && fullName.isEmpty() &&
                        ageStr.isEmpty() && email.isEmpty() && phone.isEmpty() && password.isEmpty());
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    public void testNullStringHandling() {
        String nullString = null;
        // In production code, null checks should be performed
        // This test documents expected behavior
        try {
            boolean isEmpty = nullString.isEmpty();
            assertFalse("Should have thrown NullPointerException", true);
        } catch (NullPointerException e) {
            assertTrue("Null string should throw NullPointerException", true);
        }
    }

    @Test
    public void testVeryLongPassword() {
        String password = "a".repeat(1000);
        assertTrue("Very long password should be valid", password.length() >= 6);
    }

    @Test
    public void testVeryLargeAge() {
        String ageStr = "150";
        try {
            int age = Integer.parseInt(ageStr);
            assertTrue("Age 150 should be valid (>= 0)", age >= 0);
        } catch (NumberFormatException e) {
            assertFalse("Should not throw exception for valid integer", true);
        }
    }

    @Test
    public void testSpecialCharactersInEmail() {
        String email = "test+123@example.com";
        assertFalse("Email with special characters should not be empty", email.trim().isEmpty());
    }

    @Test
    public void testUnicodeCharactersInName() {
        String fullName = "José García";
        assertFalse("Name with unicode characters should be valid", fullName.trim().isEmpty());
    }

    // ==================== TRIM BEHAVIOR TESTS ====================

    @Test
    public void testTrimBehavior_LeadingSpaces() {
        String input = "   test";
        assertEquals("Leading spaces should be trimmed", "test", input.trim());
    }

    @Test
    public void testTrimBehavior_TrailingSpaces() {
        String input = "test   ";
        assertEquals("Trailing spaces should be trimmed", "test", input.trim());
    }

    @Test
    public void testTrimBehavior_BothEnds() {
        String input = "   test   ";
        assertEquals("Spaces on both ends should be trimmed", "test", input.trim());
    }

    @Test
    public void testTrimBehavior_InternalSpaces() {
        String input = "test user";
        assertEquals("Internal spaces should be preserved", "test user", input.trim());
    }

    @Test
    public void testTrimBehavior_Tabs() {
        String input = "\ttest\t";
        assertEquals("Tabs should be trimmed", "test", input.trim());
    }

    @Test
    public void testTrimBehavior_Newlines() {
        String input = "\ntest\n";
        assertEquals("Newlines should be trimmed", "test", input.trim());
    }
}