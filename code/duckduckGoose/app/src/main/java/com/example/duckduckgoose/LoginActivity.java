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

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private View btnSignIn, btnCreateAccount;
    private CardView sheetSignIn, sheetCreate;
    private TextView btnSheetCancel1, btnSheetCancel2;
    private MaterialButton btnSheetSignIn, btnCreateSubmit;

    // sign in fields
    private TextInputEditText edtEmail, edtPassword;

    // create account fields
    private TextInputEditText edtRegUserId, edtFullName, edtAge, edtRegEmail, edtPhone, edtRegPassword;
    private MaterialAutoCompleteTextView edtAccountType;

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

        // firebase Auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // check if user is already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // user is already logged in, load their data and navigate
            loadNavigate(currentUser.getUid());
            return;
        }

        TopBarWiring.attachProfileSheet(this);
        initViews();
        setupListeners();
        setupBackPress();
    }

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

        // account type dropdown for create account sheet
        if (edtAccountType != null) {
            ArrayAdapter<String> acctAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    getResources().getStringArray(R.array.account_types)
            );
            edtAccountType.setAdapter(acctAdapter);
        }
    }

    private void setupListeners() {
        // Open sign in sheet
        btnSignIn.setOnClickListener(v -> {
            sheetSignIn.setVisibility(View.VISIBLE);
            sheetCreate.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });

        // Open create account sheet
        btnCreateAccount.setOnClickListener(v -> {
            sheetCreate.setVisibility(View.VISIBLE);
            sheetSignIn.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });

        // Close sign in sheet
        btnSheetCancel1.setOnClickListener(v -> closeSheets());

        // Close create account sheet
        btnSheetCancel2.setOnClickListener(v -> closeSheets());

        // Login actions
        btnSheetSignIn.setOnClickListener(this::handleSignIn);
        btnCreateSubmit.setOnClickListener(this::handleCreateAccount);
    }

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

    private void closeSheets() {
        sheetSignIn.setVisibility(View.GONE);
        sheetCreate.setVisibility(View.GONE);
        btnSignIn.setVisibility(View.VISIBLE);
        btnCreateAccount.setVisibility(View.VISIBLE);
    }

    // sign in with firebase auth
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

        // Disable button to prevent double-clicks
        btnSheetSignIn.setEnabled(false);
        btnSheetSignIn.setText("Logging in...");

        // sign in with firebase
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnSheetSignIn.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Sign in success - now load user data from Firestore
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            loadNavigate(user.getUid());
                        } else {
                            Toast.makeText(this, "Login successful but user data unavailable",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Sign in failed
                        String e = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Authentication failed: " + e,
                                Toast.LENGTH_LONG).show();
                        btnSheetSignIn.setText("Sign In");
                    }
                });
    }

    // create account with firebase auth
    private void handleCreateAccount() {
        String accountType = edtAccountType.getText().toString().trim();
        String userId = edtRegUserId.getText().toString().trim();
        String fullName = edtFullName.getText().toString().trim();
        String ageStr = edtAge.getText().toString().trim();
        String email = edtRegEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtRegPassword.getText().toString().trim();

        // Validation
        if (accountType.isEmpty()) {
            edtAccountType.setError("Account type is required");
            edtAccountType.requestFocus();
            return;
        }

        if (userId.isEmpty()) {
            edtRegUserId.setError("User ID is required");
            edtRegUserId.requestFocus();
            return;
        }

        if (fullName.isEmpty()) {
            edtFullName.setError("Full name is required");
            edtFullName.requestFocus();
            return;
        }

        if (ageStr.isEmpty()) {
            edtAge.setError("Age is required");
            edtAge.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            edtRegEmail.setError("Email is required");
            edtRegEmail.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            edtPhone.setError("Phone number is required");
            edtPhone.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            edtRegPassword.setError("Password is required");
            edtRegPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtRegPassword.setError("Password must be at least 6 characters");
            edtRegPassword.requestFocus();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 0) {
                edtAge.setError("Please enter a valid age");
                edtAge.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            edtAge.setError("Please enter a valid number");
            edtAge.requestFocus();
            return;
        }

        btnCreateSubmit.setEnabled(false);
        btnCreateSubmit.setText("Creating account...");

        // create user with firebase
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // success
                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            // store user data in firestore
                            saveUserToFirestore(user.getUid(), accountType, userId, fullName, age, email, phone);
                        } else {
                            btnCreateSubmit.setEnabled(true);
                            btnCreateSubmit.setText("Create Account");
                            Toast.makeText(this, "Account created but user data missing",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // failed
                        btnCreateSubmit.setEnabled(true);
                        btnCreateSubmit.setText("Create Account");
                        String e = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Registration failed: " + e,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // save profile to firestore
    private void saveUserToFirestore(String uid, String accountType, String userId,
                                     String fullName, int age, String email, String phone) {
        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("accountType", accountType);
        userData.put("userId", userId);
        userData.put("fullName", fullName);
        userData.put("age", age);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("createdAt", System.currentTimeMillis());

        // save to firestore under users collection with uid as document ID
        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(() -> {
                    Toast.makeText(this, "Account created successfully!",
                            Toast.LENGTH_SHORT).show();
                    // set login mode based on account type and navigate
                    loginNavigate(accountType);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // use default entrant mode as fallback
                    loginNavigate(accountType);
                });
    }

    // load user data from firestore
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
                            // Default to ENTRANT if account type is missing
                            loginNavigate("Entrant");
                        }
                    } else {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        // Default to ENTRANT if profile doesn't exist
                        loginNavigate("Entrant");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // Default to ENTRANT on error
                    loginNavigate("Entrant");
                });
    }

    /**
     * Set LOGIN_MODE in AppConfig and navigate to appropriate screen
     */
    private void loginNavigate(String accountType) {
        AppConfig.setLoginMode(accountType);

        Intent intent;

        if (AppConfig.LOGIN_MODE.equals("ADMIN")) {
            // Admin mode - go to Admin Console
            intent = new Intent(this, AdminConsoleActivity.class);
        } else {
            // Both Organizer and Entrant go to MainActivity
            intent = new Intent(this, MainActivity.class);
            if (AppConfig.LOGIN_MODE.equals("ORGANIZER")) {
                intent.putExtra("startOn", "MY_EVENTS");
            } else {
                intent.putExtra("startOn", "EVENT_LIST");
            }
        }

        startActivity(intent);
        finish(); // prevent going back to login with back button
    }
}
