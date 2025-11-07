/**
 * @file OrganizerManagerActivity.java
 *  Activity for managing and displaying a list of organizer users in the DuckDuckGoose app.
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

import com.example.duckduckgoose.user.Organizer;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  OrganizerManagerActivity
 *  Manages and displays organizer users retrieved from Firestore.
 *
 * Mirrors EntrantManagerActivity behavior: initializes Firestore, loads organizers,
 * shows them in a RecyclerView, keeps a running count, and opens ProfileSheet on item tap.
 */
public class OrganizerManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    /** List currently shown in the RecyclerView (filtered subset). */
    private List<Organizer> organizers;

    /** Full list of organizers loaded from Firestore (baseline for filtering if needed). */
    private List<Organizer> allOrganizers;

    /** RecyclerView adapter for organizer items. */
    private UserManagerAdapter adapter;

    /** Firestore database reference. */
    private FirebaseFirestore db;

    /** Reference to the Firestore functions instance. */
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");

    /** Firestore collection references. */
    private CollectionReference usersRef;
    private CollectionReference eventsRef;

    /** RecyclerView for organizer items. */
    private RecyclerView rvOrganizers;

    /** TextView showing total organizers count. */
    private TextView txtCount;

    /**
     *  Standard activity creation. Sets up UI, Firestore, adapter, and loads data.
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
        eventsRef = db.collection("events");

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
     *  View binding for RecyclerView and count TextView.
     */
    private void initializeViews() {
        rvOrganizers = findViewById(R.id.rvOrganizers);
        txtCount = findViewById(R.id.txtCount);
    }

    /**
     *  Configures the RecyclerView and item click behavior.
     */
    private void setupRecyclerView() {
        if (rvOrganizers == null) return;
        rvOrganizers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserManagerAdapter(organizers, false); // false = hide checkboxes
        adapter.setOnItemClickListener(user -> {
            // Fetch event count for the selected organizer
            eventsRef.whereEqualTo("organizerId", user.getUserId()).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int eventCount = queryDocumentSnapshots.size();
                        // For organizers, show the Events button (true) and pass the event count.
                        ProfileSheet
                                .newInstance(user, true, true, String.valueOf(eventCount), false)
                                .show(getSupportFragmentManager(), "ProfileSheet");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OrganizerManager", "Failed to fetch event count for user: " + user.getFullName(), e); // we need a better unqiue identifier
                        // Show the profile sheet anyway, just without the count
                        ProfileSheet
                                .newInstance(user, true, true, "N/A", false)
                                .show(getSupportFragmentManager(), "ProfileSheet");
                    });
        });
        rvOrganizers.setAdapter(adapter);
    }

    /**
     *  Updates the on-screen count of organizers.
     */
    private void updateCountDisplay() {
        if (txtCount != null) {
            txtCount.setText("Total Organizers: " + organizers.size());
        }
    }

    /**
     *  Loads all users from Firestore, filters to accountType == "Organizer",
     * updates adapter data set, and refreshes the count.
     */
    private void loadOrganizersFromFirestore() {
        usersRef.get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    allOrganizers.clear();
                    for (DocumentSnapshot ds : querySnapshot.getDocuments()) {
                        Organizer user = ds.toObject(Organizer.class);
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
     *  Called when a profile deletion is confirmed via ProfileSheet.
     * Removes the organizer locally and updates the count and list UI.
     *
     * @param email Email of the unique user to remove.
     */
    @Override
    public void onProfileDeleted(String email) {
        deleteUserByEmail(email);
    }

    /**
     *  Handles "Events" button from the profile sheet.
     * Navigates to the EventManagerActivity (no change from your original behavior).
     *
     * @param userId The organizer's userId (not used here, but kept for parity).
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        startActivity(new android.content.Intent(this, EventManagerActivity.class));
    }

    private void deleteUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Invalid email.", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = email.trim();

        functions
                .getHttpsCallable("deleteUserByEmail")
                .call(Collections.singletonMap("email", email))
                .addOnSuccessListener((HttpsCallableResult result) -> {
                    Toast.makeText(this, "Organizer successfully deleted.", Toast.LENGTH_SHORT).show();
                    removeFromLocalListsByEmail(email);
                    updateCountDisplay();
                })
                .addOnFailureListener(e -> {
                    Log.e("OrganizerManager", "Cloud Function delete failed", e);
                    Toast.makeText(this, "Failed to delete organizer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Removes organizer entry with the given email from both lists and updates the adapter. */
    private void removeFromLocalListsByEmail(String email) {
        // visible list
        for (int i = organizers.size() - 1; i >= 0; i--) {
            Organizer u = organizers.get(i);
            if (u != null && email.equalsIgnoreCase(u.getEmail())) {
                organizers.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
        // full baseline list
        for (int i = allOrganizers.size() - 1; i >= 0; i--) {
            Organizer u = allOrganizers.get(i);
            if (u != null && email.equalsIgnoreCase(u.getEmail())) {
                allOrganizers.remove(i);
            }
        }
    }
}
