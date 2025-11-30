/**
 * Authentication entry screen with sign-in and create-account sheets.
 *
 * Handles Firebase Auth sign-in and registration, persists user profile
 * data to Firestore, and routes to the appropriate app surface based
 * on role (Admin / Organizer / Entrant).
 *
 * @author DuckDuckGoose Development Team
 */
package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity that shows sign-in and registration flows, sets the app login role,
 * and navigates to the appropriate screen.
 */
public class LoginActivity extends AppCompatActivity {

    /** Firebase authentication entry point. */
    private FirebaseAuth auth;
    /** Firestore instance for user profile storage and retrieval. */
    private FirebaseFirestore db;

    /** Launchers for sign-in and create-account sheets. */
    private View btnSignIn, btnCreateAccount;
    /** Bottom sheets for sign-in and create-account flows. */
    private CardView sheetSignIn, sheetCreate;
    /** Sheet close buttons. */
    private TextView btnSheetCancel1, btnSheetCancel2;
    /** Sheet action buttons. */
    private MaterialButton btnSheetSignIn, btnCreateSubmit;

    /** Email and password fields for the sign-in sheet. */
    private TextInputEditText edtEmail, edtPassword;

    /** Input fields for the create-account sheet (registration flow). */
    private TextInputEditText edtRegUserId, edtFullName, edtAge, edtRegEmail, edtPhone, edtRegPassword;
    /** Dropdown for selecting the account type (for example, Admin, Organizer, Entrant). */
    private MaterialAutoCompleteTextView edtAccountType;

    /**
     * Initializes the UI, checks for an active session, and wires listeners.
     *
     * @param savedInstanceState - Saved state bundle used when re-creating the activity, may be null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        WindowInsetsController controller = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller = getWindow().getInsetsController();
        }
        if (controller != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // If already signed in -> load profile and navigate
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            loadNavigate(currentUser.getUid());
            return;
        }

        TopBarWiring.attachProfileSheet(this);
        initViews();
        setupListeners();
        setupBackPress();
    }

    /**
     * Binds all sheet and input views and configures the account type dropdown.
     * Initializes sign-in, create-account, and all related input fields.
     */
    private void initViews() {
        btnSignIn = findViewById(R.id.btnSignIn);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        sheetSignIn = findViewById(R.id.sheetSignIn);
        sheetCreate = findViewById(R.id.sheetCreate);
        btnSheetCancel1 = findViewById(R.id.btnSheetCancel1);
        btnSheetCancel2 = findViewById(R.id.btnSheetCancel2);
        btnSheetSignIn = findViewById(R.id.btnSheetSignIn);
        btnCreateSubmit = findViewById(R.id.btnCreateSubmit);

        // sign in sheet fields
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        // create account sheet fields
        edtAccountType = findViewById(R.id.edtAccountType);
        edtRegUserId = findViewById(R.id.edtRegUserId);
        edtFullName = findViewById(R.id.edtFullName);
        edtAge = findViewById(R.id.edtAge);
        edtRegEmail = findViewById(R.id.edtRegEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtRegPassword = findViewById(R.id.edtRegPassword);

        // Account type dropdown
        if (edtAccountType != null) {
            ArrayAdapter<String> acctAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    getResources().getStringArray(R.array.account_types)
            );
            edtAccountType.setAdapter(acctAdapter);
        }
    }

    /**
     * Sets up click listeners for all interactive UI elements.
     * Handles sheet visibility and primary action button behavior.
     */
    private void setupListeners() {
        // Open sign-in sheet, hide other elements
        btnSignIn.setOnClickListener(v -> {
            sheetSignIn.setVisibility(View.VISIBLE);
            sheetCreate.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });

        // Open create-account sheet, hide other elements
        btnCreateAccount.setOnClickListener(v -> {
            sheetCreate.setVisibility(View.VISIBLE);
            sheetSignIn.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });

        // Close sign-in sheet when canceled
        btnSheetCancel1.setOnClickListener(v -> closeSheets());

        // Close create-account sheet when canceled
        btnSheetCancel2.setOnClickListener(v -> closeSheets());

        // Process sign-in with Firebase Auth when submitted
        btnSheetSignIn.setOnClickListener(v -> handleSignIn());

        // Create new account and save profile when submitted
        btnCreateSubmit.setOnClickListener(v -> handleCreateAccount());
    }

    /**
     * Sets up back-press handling to close open sheets before exiting the activity.
     * Intercepts the system back button to hide sheets instead of leaving immediately.
     */
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // if a sheet is open, close it instead of exiting
                if (sheetSignIn != null && sheetSignIn.getVisibility() == View.VISIBLE) {
                    closeSheets();
                    return;
                }
                if (sheetCreate != null && sheetCreate.getVisibility() == View.VISIBLE) {
                    closeSheets();
                    return;
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * Hides all sheets and restores visibility of the primary action buttons.
     * Call this when canceling or completing a sheet action.
     */
    private void closeSheets() {
        sheetSignIn.setVisibility(View.GONE);
        sheetCreate.setVisibility(View.GONE);
        btnSignIn.setVisibility(View.VISIBLE);
        btnCreateAccount.setVisibility(View.VISIBLE);
    }

    /**
     * Attempts to authenticate the user with Firebase Authentication.
     *
     * Performs basic input validation, then attempts sign in. On success, the
     * user profile is loaded from Firestore and navigation is based on the user's role.
     */
    private void handleSignIn() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Validation
        if (email.isEmpty()) {
            edtEmail.setError("Email is required");
            edtEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            edtPassword.setError("Password is required");
            edtPassword.requestFocus();
            return;
        }

        // Disable to avoid double-submits
        btnSheetSignIn.setEnabled(false);
        btnSheetSignIn.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnSheetSignIn.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            loadNavigate(user.getUid());
                        } else {
                            Toast.makeText(this, "Login successful but user data unavailable",
                                    Toast.LENGTH_SHORT).show();
                            btnSheetSignIn.setText("Sign In");
                        }
                    } else {
                        String e = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Authentication failed: " + e,
                                Toast.LENGTH_LONG).show();
                        btnSheetSignIn.setText("Sign In");
                    }
                });
    }

    /**
     * Creates a new user account with Firebase Authentication.
     *
     * Performs input validation before attempting account creation. After
     * successful authentication, saves the user profile to Firestore, sets
     * the login mode, and navigates to the appropriate activity.
     */
    private void handleCreateAccount() {
        String accountType = edtAccountType.getText().toString().trim();
        String userId = edtRegUserId.getText().toString().trim();
        String fullName = edtFullName.getText().toString().trim();
        String ageStr = edtAge.getText().toString().trim();
        String email = edtRegEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtRegPassword.getText().toString().trim();

        if (accountType.isEmpty()) { edtAccountType.setError("Account type is required"); edtAccountType.requestFocus(); return; }
        if (userId.isEmpty())      { edtRegUserId.setError("User ID is required");       edtRegUserId.requestFocus();  return; }
        if (fullName.isEmpty())    { edtFullName.setError("Full name is required");      edtFullName.requestFocus();   return; }
        if (ageStr.isEmpty())      { edtAge.setError("Age is required");                 edtAge.requestFocus();        return; }
        if (email.isEmpty())       { edtRegEmail.setError("Email is required");          edtRegEmail.requestFocus();   return; }
        if (phone.isEmpty())       { edtPhone.setError("Phone number is required");      edtPhone.requestFocus();      return; }
        if (password.isEmpty())    { edtRegPassword.setError("Password is required");    edtRegPassword.requestFocus();return; }
        if (password.length() < 6) { edtRegPassword.setError("Password must be at least 6 characters"); edtRegPassword.requestFocus(); return; }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 0) { edtAge.setError("Please enter a valid age"); edtAge.requestFocus(); return; }
        } catch (NumberFormatException e) {
            edtAge.setError("Please enter a valid number"); edtAge.requestFocus(); return;
        }

        btnCreateSubmit.setEnabled(false);
        btnCreateSubmit.setText("Creating account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), accountType, userId, fullName, age, email, phone);
                        } else {
                            btnCreateSubmit.setEnabled(true);
                            btnCreateSubmit.setText("Create Account");
                            Toast.makeText(this, "Account created but user data missing",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        btnCreateSubmit.setEnabled(true);
                        btnCreateSubmit.setText("Create Account");
                        String e = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Registration failed: " + e,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Saves a newly created user's profile information to Firestore.
     *
     * After a successful save, navigates to the appropriate activity based on the account type.
     *
     * @param uid - Firebase Authentication user ID
     * @param accountType - Account type string (for example, ADMIN, ORGANIZER, ENTRANT)
     * @param userId - Application-specific user ID
     * @param fullName - User's full name
     * @param age - User's age in years
     * @param email - User's email address
     * @param phone - User's phone number
     */
    private void saveUserToFirestore(String uid, String accountType, String userId,
                                     String fullName, int age, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("accountType", accountType);
        userData.put("userId", userId);
        userData.put("fullName", fullName);
        userData.put("age", age);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    loginNavigate(accountType);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // fallback: still navigate based on chosen accountType
                    loginNavigate(accountType);
                });
    }

    /**
     * Retrieves the user's profile from Firestore to determine their role
     * and navigates to the appropriate activity based on account type.
     *
     * If the profile lookup fails or account type is missing, it falls back
     * to the Entrant role.
     *
     * @param uid - Firebase Authentication user ID whose profile should be loaded
     */
    private void loadNavigate(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String accountType = documentSnapshot.getString("accountType");
                        if (accountType != null) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            loginNavigate(accountType);
                        } else {
                            Toast.makeText(this, "Account type not found", Toast.LENGTH_SHORT).show();
                            loginNavigate("Entrant");
                        }
                    } else {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        loginNavigate("Entrant");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    loginNavigate("Entrant");
                });
    }

    /**
     * Sets the application's login mode and navigates to the appropriate activity.
     *
     * Updates the global login mode and launches the correct screen based on the role.
     *
     * @param accountType - Account type used to determine the destination screen
     */
    private void loginNavigate(String accountType) {
        AppConfig.setLoginMode(accountType);

        Intent intent;
        if (AppConfig.LOGIN_MODE.equals("ADMIN")) {
            // Admin mode - go to Admin Console
            intent = new Intent(this, AdminConsoleActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
            if (AppConfig.LOGIN_MODE.equals("ORGANIZER")) {
                intent.putExtra("startOn", "MY_EVENTS");
            } else {
                intent.putExtra("startOn", "EVENT_LIST");
            }
        }

        startActivity(intent);
        finish();
    }
}