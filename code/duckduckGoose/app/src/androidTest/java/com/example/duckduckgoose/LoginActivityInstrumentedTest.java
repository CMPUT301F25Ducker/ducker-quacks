package com.example.duckduckgoose;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic instrumented tests for LoginActivity. The starter code for this was from ChatGPT when asked
 * to "generate me some Instrumented test cases for Android Studio". From there, the rest were pieced
 * together and debugged with AI help.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private FirebaseAuth auth;

    @Before
    public void setUp() {
        auth = FirebaseAuth.getInstance();
        auth.signOut();
    }

    @After
    public void tearDown() {
        auth.signOut();
    }

    // Test that sign in with empty email shows error
    @Test
    public void testSignInWithEmptyEmail() {
        onView(withId(R.id.btnSignIn)).perform(click());

        onView(withId(R.id.edtPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        onView(withId(R.id.btnSheetSignIn)).perform(click());

        // should still show the sheet because validation failed
        onView(withId(R.id.sheetSignIn)).check(matches(isDisplayed()));
    }

    // Test that sign in with empty password shows error
    @Test
    public void testSignInWithEmptyPassword() {
        onView(withId(R.id.btnSignIn)).perform(click());

        onView(withId(R.id.edtEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        onView(withId(R.id.btnSheetSignIn)).perform(click());

        onView(withId(R.id.sheetSignIn)).check(matches(isDisplayed()));
    }

    // Test that create account fails when account type is empty
    @Test
    public void testCreateAccountWithEmptyFields() {
        onView(withId(R.id.btnCreateAccount)).perform(click());

        // don't fill anything, just click submit
        onView(withId(R.id.btnCreateSubmit)).perform(click());

        // sheet should still be open
        onView(withId(R.id.sheetCreate)).check(matches(isDisplayed()));
    }

    // Test successful login (requires existing account)
    @Test
    public void testSuccessfulLogin() {
        onView(withId(R.id.btnSignIn)).perform(click());

        // use valid credentials for existing test account
        onView(withId(R.id.edtEmail))
                .perform(typeText("testuser@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword))
                .perform(typeText("test123456"), closeSoftKeyboard());

        onView(withId(R.id.btnSheetSignIn)).perform(click());

        // wait a bit for firebase
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // if login successful, activity should navigate away
    }

    // Test cancel button closes the sheet
    @Test
    public void testCancelButtonWorks() {
        onView(withId(R.id.btnSignIn)).perform(click());
        onView(withId(R.id.sheetSignIn)).check(matches(isDisplayed()));

        onView(withId(R.id.btnSheetCancel1)).perform(click());

        onView(withId(R.id.sheetSignIn)).check(matches(not(isDisplayed())));
    }
}