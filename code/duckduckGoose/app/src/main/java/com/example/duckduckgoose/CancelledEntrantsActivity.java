package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a list of entrants who cancelled their application for an event.
 * Accessible to the organizer from the Event admin screen.
 */
public class CancelledEntrantsActivity extends AppCompatActivity
        implements ProfileSheet.OnProfileInteractionListener {

    private RecyclerView recycler;
    private UserManagerAdapter adapter;
    private final List<User> users = new ArrayList<>();
    private FirebaseFirestore db;
    private android.widget.TextView txtEmptyPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            android.util.Log.d("CancelledEntrants", "onCreate started");

            android.util.Log.d("CancelledEntrants", "Setting content view");
            setContentView(R.layout.activity_cancelled_entrants);
            android.util.Log.d("CancelledEntrants", "Content view set successfully");

            // Light status bar (must be after setContentView)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            }

            android.util.Log.d("CancelledEntrants", "Initializing Firestore");
            db = FirebaseFirestore.getInstance();

            android.util.Log.d("CancelledEntrants", "Finding views");
            recycler = findViewById(R.id.recyclerCancelled);
            txtEmptyPlaceholder = findViewById(R.id.txtEmptyPlaceholder);
            android.util.Log.d("CancelledEntrants", "recycler=" + recycler + ", placeholder=" + txtEmptyPlaceholder);

            // Use the same style of adapter as other screens
            android.util.Log.d("CancelledEntrants", "Creating adapter");
            adapter = new UserManagerAdapter(users, false);
            if (recycler != null) {
                android.util.Log.d("CancelledEntrants", "Setting up RecyclerView");
                recycler.setLayoutManager(new LinearLayoutManager(this));
                recycler.setAdapter(adapter);
            } else {
                android.util.Log.w("CancelledEntrants", "RecyclerView not found; activity will show placeholder only.");
            }

            // When you tap a cancelled entrant, show their profile in a sheet
            android.util.Log.d("CancelledEntrants", "Setting item click listener");
            adapter.setOnItemClickListener(user -> {
                try {
                    if (user == null) return;
                    // status is "cancelled" for this screen
                    String status = "cancelled";
                    ProfileSheet.newInstance(user, true, false, status, true)
                            .show(getSupportFragmentManager(), "ProfileSheet");
                } catch (Exception e) {
                    android.util.Log.e("CancelledEntrants", "Failed to show profile sheet", e);
                    Toast.makeText(this, "Failed to open profile", Toast.LENGTH_SHORT).show();
                }
            });

            // Get eventId from the intent
            android.util.Log.d("CancelledEntrants", "Getting eventId from intent");
            String eventId = getIntent() != null ? getIntent().getStringExtra("eventId") : null;
            android.util.Log.d("CancelledEntrants", "eventId=" + eventId);
            
            updateEmptyState();
            
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(this, "Event not specified — showing empty list", Toast.LENGTH_SHORT).show();
                android.util.Log.d("CancelledEntrants", "onCreate completed (no eventId)");
                return;
            }

            android.util.Log.d("CancelledEntrants", "Loading cancelled entrants");
            loadCancelledEntrants(eventId);
            android.util.Log.d("CancelledEntrants", "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e("CancelledEntrants", "FATAL: onCreate crashed", e);
            Toast.makeText(this, "Failed to initialize: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Don't crash the app - finish gracefully
            finish();
        }
    }

    private void loadCancelledEntrants(String eventId) {
        if (db == null) db = FirebaseFirestore.getInstance();

        db.collection("waitlist")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "cancelled")
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    users.clear();

                    if (snap == null || snap.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "No cancelled entrants found", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String uid = doc.getString("userId");
                        if (uid == null) continue;

                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    User u = userDoc != null ? userDoc.toObject(User.class) : null;
                                    if (u == null) u = new User();
                                    u.setUserId(uid);

                                    users.add(u);
                                    adapter.notifyDataSetChanged();
                                    updateEmptyState();
                                })
                                .addOnFailureListener(err -> {
                                    android.util.Log.e("CancelledEntrants", "Failed to load user " + uid, err);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CancelledEntrants", "Failed to load cancelled entrants", e);
                    Toast.makeText(this,
                            "Failed to load cancelled entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void updateEmptyState() {
        boolean empty = users.isEmpty();
        if (txtEmptyPlaceholder != null) {
            txtEmptyPlaceholder.setVisibility(empty ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        if (recycler != null) {
            recycler.setVisibility(empty ? android.view.View.GONE : android.view.View.VISIBLE);
        }
    }

    // Required by ProfileSheet.OnProfileInteractionListener

    @Override
    public void onProfileDeleted(String identifier) {
        // If you ever allow kicking from this screen, you could implement it here.
        // For now, just refresh or ignore.
        Toast.makeText(this, "Profile action not supported on this screen.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEventsButtonClicked(String userId) {

    }
}
