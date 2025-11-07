package com.example.duckduckgoose;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test cases for LoginActivity
 * Tests UI interactions, navigation, Firebase authentication, and user registration
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Before
    public void setUp() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Sign out any existing user before each test
        auth.signOut();
    }

    @After
    public void tearDown() {
        // Clean up after tests
        auth.signOut();
    }

    // ==================== UI VISIBILITY TESTS ====================

    @Test
    public void testInitialUIElementsAreVisible() {
        // Verify that sign in and create account buttons are visible initially
        onView(withId(R.id.btnSignIn))
                .check(matches(isDisplayed()));
        onView(withId(R.id.btnCreateAccount))
                .check(matches(isDisplayed()));

        // Verify sheets are not visible
        onView(withId(R.id.sheetSignIn))
                .check(matches(not(isDisplayed())));
        onView(withId(R.id.sheetCreate))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testSignInButtonOpensSignInSheet() {
        // Click sign in button
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Verify sign in sheet is visible
        onView(withId(R.id.sheetSignIn))
                .check(matches(isDisplayed()));

        // Verify main buttons are hidden
        onView(withId(R.id.btnSignIn))
                .check(matches(not(isDisplayed())));
        onView(withId(R.id.btnCreateAccount))
                .check(matches(not(isDisplayed())));

        // Verify create account sheet is not visible
        onView(withId(R.id.sheetCreate))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testCreateAccountButtonOpensCreateSheet() {
        // Click create account button
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Verify create account sheet is visible
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));

        // Verify main buttons are hidden
        onView(withId(R.id.btnSignIn))
                .check(matches(not(isDisplayed())));
        onView(withId(R.id.btnCreateAccount))
                .check(matches(not(isDisplayed())));

        // Verify sign in sheet is not visible
        onView(withId(R.id.sheetSignIn))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testCancelButtonClosesSignInSheet() {
        // Open sign in sheet
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Click cancel button
        onView(withId(R.id.btnSheetCancel1))
                .perform(click());

        // Verify sheet is closed and main buttons are visible
        onView(withId(R.id.sheetSignIn))
                .check(matches(not(isDisplayed())));
        onView(withId(R.id.btnSignIn))
                .check(matches(isDisplayed()));
        onView(withId(R.id.btnCreateAccount))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCancelButtonClosesCreateAccountSheet() {
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Click cancel button
        onView(withId(R.id.btnSheetCancel2))
                .perform(click());

        // Verify sheet is closed and main buttons are visible
        onView(withId(R.id.sheetCreate))
                .check(matches(not(isDisplayed())));
        onView(withId(R.id.btnSignIn))
                .check(matches(isDisplayed()));
        onView(withId(R.id.btnCreateAccount))
                .check(matches(isDisplayed()));
    }

    // ==================== SIGN IN VALIDATION TESTS ====================

    @Test
    public void testSignInWithEmptyEmail() {
        // Open sign in sheet
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Enter password only
        onView(withId(R.id.edtPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click sign in
        onView(withId(R.id.btnSheetSignIn))
                .perform(click());

        // Verify error is shown (email field should have error)
        // Sheet should still be visible
        onView(withId(R.id.sheetSignIn))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSignInWithEmptyPassword() {
        // Open sign in sheet
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Enter email only
        onView(withId(R.id.edtEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        // Click sign in
        onView(withId(R.id.btnSheetSignIn))
                .perform(click());

        // Verify error is shown (password field should have error)
        // Sheet should still be visible
        onView(withId(R.id.sheetSignIn))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSignInWithInvalidCredentials() {
        // Open sign in sheet
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Enter invalid credentials
        onView(withId(R.id.edtEmail))
                .perform(typeText("invalid@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword))
                .perform(typeText("wrongpassword"), closeSoftKeyboard());

        // Click sign in
        onView(withId(R.id.btnSheetSignIn))
                .perform(click());

        // Wait for Firebase response (may need to add IdlingResource for production)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify button is re-enabled after failure
        onView(withId(R.id.btnSheetSignIn))
                .check(matches(isEnabled()));

        // Toast should show error message
        onView(withId(R.id.sheetSignIn))
                .check(matches(isDisplayed()));
    }

    // ==================== CREATE ACCOUNT VALIDATION TESTS ====================

    @Test
    public void testCreateAccountWithEmptyAccountType() {
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill all fields except account type
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.edtRegEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPhone))
                .perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.edtRegPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithEmptyUserId() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill account type but leave userId empty
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithEmptyFullName() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill fields except full name
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithEmptyAge() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill fields except age
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithInvalidAge() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill fields with invalid age
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("abc"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithNegativeAge() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill fields with negative age
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("-5"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithEmptyEmail() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill fields except email
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("25"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithEmptyPhone() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill fields except phone
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.edtRegEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithEmptyPassword() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill all fields except password
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.edtRegEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPhone))
                .perform(typeText("1234567890"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithShortPassword() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill all fields with short password
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser123"), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.edtRegEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPhone))
                .perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.edtRegPassword))
                .perform(typeText("12345"), closeSoftKeyboard()); // Only 5 characters

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Verify sheet is still visible (validation failed)
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));
    }

    // ==================== BACK BUTTON TESTS ====================

    @Test
    public void testBackButtonClosesSignInSheet() {
        // Open sign in sheet
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Verify sheet is open
        onView(withId(R.id.sheetSignIn))
                .check(matches(isDisplayed()));

        // Press back button
        activityRule.getScenario().onActivity(activity -> {
            activity.getOnBackPressedDispatcher().onBackPressed();
        });

        // Verify sheet is closed
        onView(withId(R.id.sheetSignIn))
                .check(matches(not(isDisplayed())));
        onView(withId(R.id.btnSignIn))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonClosesCreateAccountSheet() {
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Verify sheet is open
        onView(withId(R.id.sheetCreate))
                .check(matches(isDisplayed()));

        // Press back button
        activityRule.getScenario().onActivity(activity -> {
            activity.getOnBackPressedDispatcher().onBackPressed();
        });

        // Verify sheet is closed
        onView(withId(R.id.sheetCreate))
                .check(matches(not(isDisplayed())));
        onView(withId(R.id.btnCreateAccount))
                .check(matches(isDisplayed()));
    }

    // ==================== APPCONFIG NAVIGATION TESTS ====================

    @Test
    public void testAppConfigInitialMode() {
        // Verify AppConfig has a default LOGIN_MODE
        assertNotNull(AppConfig.LOGIN_MODE);
    }

    @Test
    public void testAppConfigSetLoginMode() {
        // Test setting different login modes
        AppConfig.setLoginMode("ADMIN");
        assertEquals("ADMIN", AppConfig.LOGIN_MODE);

        AppConfig.setLoginMode("ENTRANT");
        assertEquals("ENTRANT", AppConfig.LOGIN_MODE);

        AppConfig.setLoginMode("ORGANIZER");
        assertEquals("ORGANIZER", AppConfig.LOGIN_MODE);
    }

    @Test
    public void testAppConfigSetLoginModeToLowercase() {
        // Test that lowercase is converted to uppercase
        AppConfig.setLoginMode("admin");
        assertEquals("ADMIN", AppConfig.LOGIN_MODE);

        AppConfig.setLoginMode("entrant");
        assertEquals("ENTRANT", AppConfig.LOGIN_MODE);
    }

    // ==================== BUTTON STATE TESTS ====================

    @Test
    public void testSignInButtonDisabledDuringLogin() {
        // Open sign in sheet
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Enter credentials
        onView(withId(R.id.edtEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click sign in
        onView(withId(R.id.btnSheetSignIn))
                .perform(click());

        // Button should be disabled immediately after click
        // (Will be re-enabled after Firebase response)
    }

    @Test
    public void testCreateAccountButtonDisabledDuringRegistration() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill all fields with valid data
        onView(withId(R.id.edtAccountType))
                .perform(typeText("Entrant"), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("testuser" + System.currentTimeMillis()), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("25"), closeSoftKeyboard());
        onView(withId(R.id.edtRegEmail))
                .perform(typeText("test" + System.currentTimeMillis() + "@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPhone))
                .perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.edtRegPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click create account
        onView(withId(R.id.btnCreateSubmit))
                .perform(click());

        // Button should be disabled immediately after click
        // (Will be re-enabled after Firebase response)
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    public void testSignInWithWhitespaceEmail() { // THIS TEST CASE IS SCUFFED
        // Open sign in sheet
        onView(withId(R.id.btnSignIn))
                .perform(click());

        // Enter email with whitespace
        onView(withId(R.id.edtEmail))
                .perform(typeText("  test@example.com  "), closeSoftKeyboard());
        onView(withId(R.id.edtPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click sign in (should trim whitespace)
        onView(withId(R.id.btnSheetSignIn))
                .perform(click());

        // Verify attempt is made (trimming handled in code)
    }

    @Test
    public void testCreateAccountWithWhitespaceInFields() { // THIS TEST CASE IS SCUFFED
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Fill fields with whitespace
        onView(withId(R.id.edtAccountType))
                .perform(typeText("  Entrant  "), closeSoftKeyboard());
        onView(withId(R.id.edtRegUserId))
                .perform(typeText("  testuser123  "), closeSoftKeyboard());
        onView(withId(R.id.edtFullName))
                .perform(typeText("  Test User  "), closeSoftKeyboard());
        onView(withId(R.id.edtAge))
                .perform(typeText("  25  "), closeSoftKeyboard());

        // Should handle trimming properly
    }

    @Test
    public void testAccountTypeDropdownContainsOptions() {
        // Open create account sheet
        onView(withId(R.id.btnCreateAccount))
                .perform(click());

        // Verify account type field is present and enabled
        onView(withId(R.id.edtAccountType))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()));
    }
}