package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class EventDetailsAdminActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_event_details_admin);

        TopBarWiring.attachProfileSheet(this);

        TextView eventTitle = findViewById(R.id.txtEventTitle);

        Intent intent = getIntent();
        String title = intent.getStringExtra("eventTitle");
        String eventId = intent.getStringExtra("eventId");

        if (title != null) {
            eventTitle.setText(title);
        }

        MaterialButton deleteButton = findViewById(R.id.delete_event_button);
        deleteButton.setOnClickListener(v -> {
            // Ensure admin is signed in
            FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
            if (cur == null) {
                Toast.makeText(this, "Please sign in as administrator to perform this action", Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirm
            new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event? This action will notify entrants.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (eventId == null || eventId.isEmpty()) {
                        Toast.makeText(this, "Event identifier not available; cannot delete", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    performAdminDelete(eventId, title, cur.getUid());
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        MaterialButton eventLogsButton = findViewById(R.id.event_logs_button);
        eventLogsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Event logs not implemented yet", Toast.LENGTH_SHORT).show();
        });

        MaterialButton imagePosterButton = findViewById(R.id.image_poster_button);
        imagePosterButton.setOnClickListener(v -> {
            Intent imageManagerIntent = new Intent(EventDetailsAdminActivity.this, ImageManagerActivity.class);
            startActivity(imageManagerIntent);
        });
    }

    public void goBack(View view) {
        finish();
    }

    private void performAdminDelete(String eventId, String title, String adminUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch event to gather entrant lists
        db.collection("events").document(eventId).get()
            .addOnSuccessListener(doc -> {
                if (doc == null || !doc.exists()) {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> recipients = new ArrayList<>();
                Object regObj = doc.get("registeredUsers");
                Object waitObj = doc.get("waitingList");
                Object accObj = doc.get("acceptedFromWaitlist");

                if (regObj instanceof List) {
                    for (Object o : (List) regObj) if (o != null) recipients.add(o.toString());
                }
                if (waitObj instanceof List) {
                    for (Object o : (List) waitObj) if (o != null) recipients.add(o.toString());
                }
                if (accObj instanceof List) {
                    for (Object o : (List) accObj) if (o != null) recipients.add(o.toString());
                }

                // dedupe
                List<String> unique = new ArrayList<>();
                for (String u : recipients) if (!unique.contains(u)) unique.add(u);

                final int total = unique.size();
                final int[] done = {0};

                String message = "The event '" + (title != null ? title : "(untitled)") + "' has been deleted by an administrator.";

                if (total == 0) {
                    // No recipients; proceed to delete
                    deleteEventDoc(db, eventId, title);
                    return;
                }

                for (String uid : unique) {
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("userId", uid);
                    notif.put("message", message);
                    notif.put("eventId", eventId);
                    notif.put("sentBy", adminUid);
                    notif.put("timestamp", com.google.firebase.Timestamp.now());

                    db.collection("notifications").add(notif)
                        .addOnSuccessListener(r -> {
                            done[0]++;
                            if (done[0] >= total) {
                                // After notifications created, delete the event
                                deleteEventDoc(db, eventId, title);
                            }
                        })
                        .addOnFailureListener(e -> {
                            done[0]++;
                            // even on failure continue; once all attempted, delete event
                            if (done[0] >= total) {
                                deleteEventDoc(db, eventId, title);
                            }
                        });
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteEventDoc(FirebaseFirestore db, String eventId, String title) {
        db.collection("events").document(eventId).delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Event deleted and entrants notified", Toast.LENGTH_SHORT).show();
                Intent result = new Intent();
                result.putExtra("eventId", eventId);
                result.putExtra("eventTitleToDelete", title);
                result.putExtra("deleted", true);
                setResult(RESULT_OK, result);
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete event: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
