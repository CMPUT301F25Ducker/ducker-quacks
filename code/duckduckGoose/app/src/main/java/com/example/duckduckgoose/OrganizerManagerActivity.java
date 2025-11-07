/**
 * @file OrganizerManagerActivity.java
 * @brief Activity for managing and displaying a list of organizer users in the DuckDuckGoose app.
 *
 * Loads all users from Firestore, filters by accountType == "Organizer", and displays them
 * in a RecyclerView. Provides profile viewing via ProfileSheet, supports deleting organizers
 * from the local list when the profile sheet reports a deletion, and shows a total count.
 *
 * @author
 *      DuckDuckGoose Development Team
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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * @class OrganizerManagerActivity
 * @brief Manages and displays organizer users retrieved from Firestore.
 *
 * Mirrors EntrantManagerActivity behavior: initializes Firestore, loads organizers,
 * shows them in a RecyclerView, keeps a running count, and opens ProfileSheet on item tap.
 */
public class OrganizerManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    /** List currently shown in the RecyclerView (filtered subset). */
    private List<User> organizers;

    /** Full list of organizers loaded from Firestore (baseline for filtering if needed). */
    private List<User> allOrganizers;

    /** RecyclerView adapter for organizer items. */
    private UserManagerAdapter adapter;

    /** Firestore database reference. */
    private FirebaseFirestore db;

    /** Firestore "users" collection reference. */
    private CollectionReference usersRef;

    /** RecyclerView for organizer items. */
    private RecyclerView rvOrganizers;

    /** TextView showing total organizers count. */
    private TextView txtCount;

    /**
     * @brief Standard activity creation. Sets up UI, Firestore, adapter, and loads data.
     *
     * @param savedInstanceState Saved instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manager);

        // Light status bar (when supported) to match EntrantManagerActivity behavior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        // Top bar with profile sheet wiring
        TopBarWiring.attachProfileSheet(this);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        // Initialize lists and views
        allOrganizers = new ArrayList<>();
        organizers = new ArrayList<>();
        initializeViews();

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Set up RecyclerView and adapter (hide checkboxes for managers)
        setupRecyclerView();

        // Load organizers
        loadOrganizersFromFirestore();
    }

    /**
     * @brief View binding for RecyclerView and count TextView.
     */
    private void initializeViews() {
        rvOrganizers = findViewById(R.id.rvOrganizers);
        txtCount = findViewById(R.id.txtCount);
    }

    /**
     * @brief Configures the RecyclerView and item click behavior.
     */
    private void setupRecyclerView() {
        if (rvOrganizers == null) return;
        rvOrganizers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserManagerAdapter(organizers, false); // false = hide checkboxes
        adapter.setOnItemClickListener(user -> {
            // For organizers, show the Events button (true). We can pass an empty string for eventCount/status.
            ProfileSheet
                    .newInstance(user, true, true, "", false)
                    .show(getSupportFragmentManager(), "ProfileSheet");
        });
        rvOrganizers.setAdapter(adapter);
    }

    /**
     * @brief Updates the on-screen count of organizers.
     */
    private void updateCountDisplay() {
        if (txtCount != null) {
            txtCount.setText("Total Organizers: " + organizers.size());
        }
    }

    /**
     * @brief Loads all users from Firestore, filters to accountType == "Organizer",
     * updates adapter data set, and refreshes the count.
     */
    private void loadOrganizersFromFirestore() {
        usersRef.get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    allOrganizers.clear();
                    for (DocumentSnapshot ds : querySnapshot.getDocuments()) {
                        User user = ds.toObject(User.class);
                        if (user != null && "Organizer".equals(user.getAccountType())) {
                            allOrganizers.add(user);
                        }
                    }
                    organizers.clear();
                    organizers.addAll(allOrganizers);
                    adapter.notifyDataSetChanged();
                    updateCountDisplay();
                })
                .addOnFailureListener(e -> {
                    Log.e("OrganizerManager", "Failed to load users from Firestore", e);
                    Toast.makeText(this, "Error loading organizers.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * @brief Called when a profile deletion is confirmed via ProfileSheet.
     * Removes the organizer locally and updates the count and list UI.
     *
     * @param email Email of the unique user to remove.
     */
    @Override
    public void onProfileDeleted(String email) {
        deleteUserByEmail(email);
    }

    private void deleteUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Invalid email.", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = email.trim();

        usersRef.whereEqualTo("email", key)
                .limit(1) // emails are unique
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "No organizer found for that email.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String docId = snap.getDocuments().get(0).getId();
                    usersRef.document(docId).delete()
                            .addOnSuccessListener(v -> {
                                removeFromLocalListsByEmail(key);
                                updateCountDisplay();
                                Toast.makeText(this, "Organizer deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("OrganizerManager", "Delete failed for docId=" + docId, e);
                                Toast.makeText(this, "Failed to delete organizer.", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("OrganizerManager", "Query by email failed", e);
                    Toast.makeText(this, "Error searching organizer.", Toast.LENGTH_LONG).show();
                });
    }

    /** Removes organizer entry with the given email from both lists and updates the adapter. */
    private void removeFromLocalListsByEmail(String email) {
        // visible list
        for (int i = organizers.size() - 1; i >= 0; i--) {
            User u = organizers.get(i);
            if (u != null && email.equalsIgnoreCase(u.getEmail())) {
                organizers.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
        // full baseline list
        for (int i = allOrganizers.size() - 1; i >= 0; i--) {
            User u = allOrganizers.get(i);
            if (u != null && email.equalsIgnoreCase(u.getEmail())) {
                allOrganizers.remove(i);
            }
        }
    }

    /**
     * @brief Handles "Events" button from the profile sheet.
     * Navigates to the EventManagerActivity (no change from your original behavior).
     *
     * @param userId The organizer's userId (not used here, but kept for parity).
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        startActivity(new android.content.Intent(this, EventManagerActivity.class));
    }
}
