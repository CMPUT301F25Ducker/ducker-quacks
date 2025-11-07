/**
 * @file EntrantManagerActivity.java
 *  Activity for managing and displaying a list of entrant users in the DuckDuckGoose app.
 *
 * This activity retrieves all user documents from Firestore, filters them to include only entrants,
 * and displays them in a RecyclerView. It also allows the admin to view entrant profiles
 * and delete entrants through a profile sheet interface.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  EntrantManagerActivity
 *  Manages and displays entrant users retrieved from Firestore.
 *
 * This activity provides administrative functionality to view, count,
 * and delete entrant accounts within the app. It interfaces with Firestore
 * to load user data and updates the UI accordingly.
 */
public class EntrantManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    /** List of entrants currently displayed in the RecyclerView. */
    private List<User> entrants;

    /** List of all entrants loaded from Firestore. */
    private List<User> allEntrants;

    /** Adapter for displaying entrant users in the RecyclerView. */
    private UserManagerAdapter adapter;

    /** Reference to the Firestore database instance. */
    private FirebaseFirestore db;


    /** Reference to the Firestore functions instance. */
    private FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");

    /** Reference to the Firestore "users" collection. */
    private CollectionReference usersRef;

    /** RecyclerView UI element that displays entrants. */
    private RecyclerView rvEntrants;

    /** TextView showing the total count of entrants. */
    private TextView txtCount;

    /**
     * Called when the activity is created.
     * Sets up UI components, initializes Firebase, and loads entrants from Firestore.
     *
     * @param savedInstanceState Saved instance state for restoring previous configurations.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_manager);

        // Set up the top bar and system UI
        TopBarWiring.attachProfileSheet(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        // Initialize lists and views
        allEntrants = new ArrayList<>();
        entrants = new ArrayList<>();
        initializeViews();

        // Set up back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupRecyclerView();
        loadEntrantsFromFirestore();
    }

    /**
     * Initializes view references from the layout.
     */
    private void initializeViews() {
        rvEntrants = findViewById(R.id.rvEntrants);
        txtCount = findViewById(R.id.txtCount);
    }

    /**
     * Sets up the RecyclerView to display entrants.
     * Configures the adapter and item click behavior.
     */
    private void setupRecyclerView() {
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserManagerAdapter(entrants, false); // Use the 'entrants' list
        adapter.setOnItemClickListener(user -> {
            ProfileSheet.newInstance(user, true, false, "", false)
                    .show(getSupportFragmentManager(), "ProfileSheet");
        });
        rvEntrants.setAdapter(adapter);
    }

    /**
     * Updates the TextView showing the total number of entrants.
     */
    private void updateCountDisplay() {
        if (txtCount != null) {
            txtCount.setText("Total Attendees: " + entrants.size());
        }
    }

    /**
     * Loads entrant data from Firestore, filters users by account type "Entrant",
     * and updates the RecyclerView with the results.
     */
    private void loadEntrantsFromFirestore() {
        usersRef.get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    allEntrants.clear();
                    for (DocumentSnapshot ds : querySnapshot.getDocuments()) {
                        User user = ds.toObject(User.class);
                        // Safely check accountType to prevent crashes
                        if (user != null && "Entrant".equals(user.getAccountType())) {
                            allEntrants.add(user);
                        } else if (user == null) {
                            Log.w("Firestore", "User doc exists? " + ds.exists() + " but toObject() returned null");
                        }
                    }
                    entrants.clear();
                    entrants.addAll(allEntrants);
                    adapter.notifyDataSetChanged();
                    updateCountDisplay();
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantManager", "Error loading attendees from Firestore", e);
                    Toast.makeText(this, "Error loading attendees.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Called when a user profile is deleted through the ProfileSheet interface.
     * Removes the user from both local lists and updates the UI.
     *
     * @param email The email of the deleted user.
     */
    @Override
    public void onProfileDeleted(String email) {
        // Treat the parameter as EMAIL here
        deleteUserByEmail(email);
    }

    /**
     * Not used for entrants, since they do not have event management options.
     *
     * @param userId The ID of the user whose event button was clicked.
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        // Not applicable to entrants
    }

    private void deleteUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Invalid email.", Toast.LENGTH_SHORT).show();
            return;
        }

        functions
                .getHttpsCallable("deleteUserByEmail")
                .call(Collections.singletonMap("email", email))
                .addOnSuccessListener((HttpsCallableResult result) -> {
                    Toast.makeText(this, "Entrant successfully deleted.", Toast.LENGTH_SHORT).show();
                    removeFromLocalListsByEmail(email);
                    updateCountDisplay();
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantManager", "Cloud Function delete failed", e);
                    Toast.makeText(this, "Failed to delete entrant: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void removeFromLocalListsByEmail(String email) {
        // visible list
        for (int i = entrants.size() - 1; i >= 0; i--) {
            User u = entrants.get(i);
            if (u != null && email.equalsIgnoreCase(u.getEmail())) {
                entrants.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
        // full list
        for (int i = allEntrants.size() - 1; i >= 0; i--) {
            User u = allEntrants.get(i);
            if (u != null && email.equalsIgnoreCase(u.getEmail())) {
                allEntrants.remove(i);
            }
        }
    }
}
