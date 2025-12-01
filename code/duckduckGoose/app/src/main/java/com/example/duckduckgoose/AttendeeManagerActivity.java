/**
 * Activity for managing event attendees and waitlist operations.
 *
 * Displays a list of attendees for a specific event with filtering options.
 * Allows organizers to perform lottery draws, send notifications, export data,
 * and manage waitlist entries. Includes map visualization of attendee locations.
 *
 * @author DuckDuckGoose Development Team
 */
package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;
import android.content.Intent;

import com.example.duckduckgoose.waitlist.WaitlistEntry;
import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.api.IMapController;

/**
 * Main activity for attendee and waitlist management.
 *
 * Provides comprehensive tools for managing event attendees including
 * lottery selection, batch messaging, CSV export, and geographic visualization.
 */
public class AttendeeManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {
    /** Event ID for which attendees are being managed. */
    private String eventId;

    /** Filtered list of attendees currently displayed. */
    private List<User> attendees;

    /** Complete list of all attendees for the event. */
    private List<User> allAttendees;

    /** Adapter for binding attendee data to the RecyclerView. */
    private UserManagerAdapter adapter;

    /** RecyclerView displaying the attendee list. */
    private RecyclerView rvAttendees;

    /** Card view container for the map popup. */
    private CardView mapPopup;

    /** Background overlay for the map popup. */
    private View mapPopupBackground;

    /** TextView displaying the count of attendees. */
    private TextView txtCount;

    /** TextView displaying the count of active participants. */
    private TextView txtInCircle;

    /** Button to export attendee data as CSV. */
    private MaterialButton btnExportCSV;

    /** Button to revoke tickets for cancelled entrants. */
    private MaterialButton btnRevokeTicket;

    /** Button to send messages to attendees. */
    private MaterialButton btnSendMessage;

    /** Button to display the world map of attendee locations. */
    private MaterialButton btnWorldMap;

    /** Button to initiate random selection lottery. */
    private MaterialButton btnSelectRandom;

    /** Button to redraw for declined positions. */
    private MaterialButton btnRedrawDucks;

    /** Dropdown menu for filtering attendees by status. */
    private AutoCompleteTextView dropFilterAttendees;

    /** Map view for displaying attendee geographic locations. */
    private MapView map;

    /** Controller for managing map operations. */
    private IMapController mapController;

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /** Flag indicating if the current user is the organizer. */
    private boolean isOrganizer = false;

    /** Map of user IDs to their waitlist document IDs. */
    private Map<String, String> entrantDocIds = new HashMap<>();

    /** Map of user IDs to their current waitlist status. */
    private Map<String, String> entrantStatusMap = new HashMap<>();

    private FirebaseUser currentUser;
    /**
     * Initializes the attendee manager screen and sets up all components.
     *
     * @param savedInstanceState - Saved activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        eventId = getIntent().getStringExtra("eventId");
        EdgeToEdge.enable(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_manager);

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        // Attach profile sheet to top bar
        TopBarWiring.attachProfileSheet(this);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        initializeViews();
        setupMap();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        setupDropdownFilter();
        setupButtonListeners();
        setupRecyclerView();

        loadWaitlistEntrants();
    }

    /**
     * Initializes and configures the map view.
     */
    private void setupMap() {
        map = findViewById(R.id.mapView);
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            mapController = map.getController();
            mapController.setZoom(17.0);
            GeoPoint startPoint = new GeoPoint(53.52505537879172, -113.5255503277704);
            mapController.setCenter(startPoint);
        }
    }

    /**
     * Resumes the map view when activity resumes.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    /**
     * Pauses the map view when activity pauses.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    /**
     * Initializes all UI view references.
     */
    private void initializeViews() {
        rvAttendees = findViewById(R.id.rvAttendees);
        mapPopup = findViewById(R.id.mapPopup);
        mapPopupBackground = findViewById(R.id.mapPopupBackground);
        txtCount = findViewById(R.id.txtCount);
        txtInCircle = findViewById(R.id.txtInCircle);
        btnExportCSV = findViewById(R.id.btnExportCSV);
        btnRevokeTicket = findViewById(R.id.btnRevokeTicket);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnWorldMap = findViewById(R.id.btnWorldMap);
        btnSelectRandom = findViewById(R.id.btnSelectRandom);
        btnRedrawDucks = findViewById(R.id.btnRedrawDucks);
        dropFilterAttendees = findViewById(R.id.dropFilterAttendees);
    }

    /**
     * Sets up the attendee filter dropdown menu.
     */
    private void setupDropdownFilter() {
        if (dropFilterAttendees != null) {
            String[] filters = {"Selected/Waiting", "Not Selected", "Duck", "Goose"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filters);
            dropFilterAttendees.setAdapter(adapter);
            dropFilterAttendees.setOnItemClickListener((parent, view, position, id) -> applyFilter(filters[position]));
        }
    }

    /**
     * Sets up click listeners for all action buttons.
     */
    private void setupButtonListeners() {
        if (btnExportCSV != null) {
            btnExportCSV.setOnClickListener(v -> {
                if (attendees == null || attendees.isEmpty()) {
                    Toast.makeText(this, "No attendees to export", Toast.LENGTH_SHORT).show();
                    return;
                }
                StringBuilder csv = new StringBuilder();
                csv.append("UserId,FullName,Email,AccountType\n");
                for (User u : attendees) {
                    csv.append(u.getUserId() != null ? u.getUserId() : "").append(",")
                            .append(u.getFullName() != null ? u.getFullName() : "").append(",")
                            .append(u.getEmail() != null ? u.getEmail() : "").append(",")
                            .append(u.getAccountType() != null ? u.getAccountType() : "").append("\n");
                }
                try {
                    String fileName = "attendees_" + (eventId != null ? eventId : "event") + ".csv";
                    java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    java.io.File file = new java.io.File(downloads, fileName);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                    fos.write(csv.toString().getBytes());
                    fos.close();
                    Toast.makeText(this, "CSV exported to Downloads/" + fileName, Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(this, "Failed to export CSV: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        if (btnRevokeTicket != null) {
            btnRevokeTicket.setOnClickListener(v -> notifyCancelledEntrants());
        }

        // --- UPDATED SEND MESSAGE ---
        if (btnSendMessage != null) {
            btnSendMessage.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null || !isOrganizer) {
                    Toast.makeText(this, "Only the organizer can send messages", Toast.LENGTH_SHORT).show();
                    return;
                }

                final android.widget.EditText input = new android.widget.EditText(this);
                input.setHint("Enter message...");

                new AlertDialog.Builder(this)
                        .setTitle("Send Notification")
                        .setView(input)
                        //
                        .setPositiveButton("Send to Geese", (dialog, which) -> {
                            String msg = "Message for GOOSE: " + input.getText().toString().trim();
                            if (!msg.isEmpty()) sendBatchMessage(msg, "GOOSE");
                        })
                        //
                        .setNeutralButton("Send to Ducks", (dialog, which) -> {
                            String msg = "Message for DUCK: " + input.getText().toString().trim();
                            if (!msg.isEmpty()) sendBatchMessage(msg, "DUCK");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnWorldMap != null) btnWorldMap.setOnClickListener(v -> showMapPopup());
        if (btnSelectRandom != null) btnSelectRandom.setOnClickListener(v -> selectRandomAttendees());
        if (btnRedrawDucks != null) btnRedrawDucks.setOnClickListener(v -> redrawDucks());
        View btnCloseMap = findViewById(R.id.btnCloseMap);
        if (btnCloseMap != null) btnCloseMap.setOnClickListener(v -> hideMapPopup());
    }

    /**
     * Sends a batch notification message to a targeted group of attendees.
     *
     * @param message - The message content to send
     * @param targetGroup - The target group ("GOOSE" for selected/accepted or "DUCK" for waiting)
     */
    private void sendBatchMessage(String message, String targetGroup) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        int count = 0;
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        boolean hasUpdates = false;

        for (User u : allAttendees) {
            String uid = u.getUserId();
            if (uid == null) continue;

            String status = entrantStatusMap.get(uid);
            if (status == null) status = "waiting";

            boolean isGoose = status.equals("selected") || status.equals("accepted");
            boolean shouldSend = false;

            if (targetGroup.equals("GOOSE") && isGoose) {
                shouldSend = true;
            } else if (targetGroup.equals("DUCK") && !isGoose) {
                shouldSend = true;
            }

            if (shouldSend) {
                Map<String, Object> notif = new HashMap<>();
                notif.put("userId", uid);
                notif.put("message", message);
                notif.put("eventId", eventId);
                notif.put("sentBy", currentUser.getUid());
                notif.put("timestamp", com.google.firebase.Timestamp.now());

                batch.set(db.collection("notifications").document(), notif);
                hasUpdates = true;
                count++;
            }
        }

        if (hasUpdates) {
            int finalCount = count;
            batch.commit()
                    .addOnSuccessListener(v -> Toast.makeText(this, "sent to " + finalCount + " people", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error sending: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No users found in that group.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets up the RecyclerView for displaying attendees.
     */
    private void setupRecyclerView() {
        if (rvAttendees != null && eventId != null) {
            rvAttendees.setLayoutManager(new LinearLayoutManager(this));
            allAttendees = new ArrayList<>();
            attendees = new ArrayList<>(allAttendees);
            adapter = new UserManagerAdapter(attendees);
            adapter.setOnItemClickListener(user -> {
                String status = user.getAccountType();
                ProfileSheet.newInstance(user, true, false, status, true)
                        .show(getSupportFragmentManager(), "ProfileSheet");
            });
            rvAttendees.setAdapter(adapter);
        }
    }

    /**
     * Loads waitlist entrants from Firestore and sets up real-time updates.
     */
    private void loadWaitlistEntrants() {
        if (db == null) db = FirebaseFirestore.getInstance();
        if (eventId == null || eventId.isEmpty()) return;

        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                String organizerId = doc.getString("organizerId");
                FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
                isOrganizer = (cur != null && organizerId != null && organizerId.equals(cur.getUid()));
                if (btnSendMessage != null) btnSendMessage.setEnabled(isOrganizer);
            }
        });

        // Listen for live updates to waitlist entries for this event so organizer sees accept/decline in real time
        db.collection("waitlist")
                .whereEqualTo("eventId", eventId)
                .addSnapshotListener((QuerySnapshot snapshot, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error loading entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allAttendees.clear();
                    attendees.clear();
                    entrantDocIds.clear();
                    entrantStatusMap.clear();

                    if (adapter != null) adapter.notifyDataSetChanged();

                    if (snapshot != null && !snapshot.isEmpty()) {
                        for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
                            String uid = entryDoc.getString("userId");
                            String rawStatus = entryDoc.getString("status");

                            if (uid == null) continue;

                            entrantDocIds.put(uid, entryDoc.getId());
                            entrantStatusMap.put(uid, rawStatus != null ? rawStatus.toLowerCase() : "waiting");

                            // Load user profile and add to lists if not already present
                            db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                                User u = userDoc != null ? userDoc.toObject(User.class) : null;
                                if (u == null) u = new User();
                                u.setUserId(uid);

                                // Replace or add user in allAttendees
                                if (!containsUser(allAttendees, uid)) {
                                    allAttendees.add(u);
                                }

                                String currentFilter = dropFilterAttendees != null ? dropFilterAttendees.getText().toString() : "Value";
                                // Re-evaluate whether the user should be shown under current filter
                                if (shouldShowUser(u, currentFilter)) {
                                    // avoid duplicates in attendees
                                    if (!containsUser(attendees, uid)) {
                                        attendees.add(u);
                                    }
                                } else {
                                    // remove if present but no longer matches
                                    final String removeId = uid;
                                    attendees.removeIf(x -> x.getUserId() != null && x.getUserId().equals(removeId));
                                }

                                if (adapter != null) adapter.notifyDataSetChanged();
                                updateCountDisplay();
                            });
                        }
                    } else {
                        updateCountDisplay();
                    }
                });
    }

    /**
     * Determines if a user should be displayed based on the current filter.
     *
     * @param user - The user to check
     * @param filter - The current filter string
     * @return true if the user matches the filter criteria, false otherwise
     */
    private boolean shouldShowUser(User user, String filter) {
        if (filter == null || filter.equals("Value") || filter.isEmpty()) return true;

        // Get status from our local map
        String status = entrantStatusMap.get(user.getUserId());
        if (status == null) status = "waiting";

        switch (filter) {
            case "Selected/Waiting":
                return status.equals("selected") || status.equals("waiting");

            case "Not Selected":
                return status.equals("cancelled") || status.equals("declined");

            case "Goose":
                return status.equals("selected") || status.equals("accepted");

            case "Duck":
                return !status.equals("selected") && !status.equals("accepted");

            default:
                return true;
        }
    }

    /**
     * Checks if a user list contains a specific user ID.
     *
     * @param list - The list of users to search
     * @param userId - The user ID to find
     * @return true if the user is in the list, false otherwise
     */
    private boolean containsUser(List<User> list, String userId) {
        for (User u : list) {
            if (u.getUserId() != null && u.getUserId().equals(userId)) return true;
        }
        return false;
    }

    /**
     * Applies a filter to the attendee list.
     *
     * @param filter - The filter criteria to apply
     */
    private void applyFilter(String filter) {
        attendees.clear();
        for (User user : allAttendees) {
            if (shouldShowUser(user, filter)) {
                attendees.add(user);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        updateCountDisplay();
    }

    /**
     * Updates the count displays showing attendee statistics.
     */
    private void updateCountDisplay() {
        if (txtCount != null) txtCount.setText("Count: " + attendees.size() + "/Spots");

        if (txtInCircle != null) {
            // Count Geese (Selected/Accepted) vs Ducks (Waiting/Rest)
            long activeCount = 0;
            for (User user : attendees) {
                String s = entrantStatusMap.get(user.getUserId());
                if (s != null && (s.equals("waiting") || s.equals("selected") || s.equals("accepted"))) {
                    activeCount++;
                }
            }
            txtInCircle.setText("In Circle: " + activeCount);
        }
    }

    /**
     * Prompts the organizer to select a random number of attendees from the waiting pool.
     */
    private void selectRandomAttendees() {
        List<User> waitingPool = new ArrayList<>(allAttendees);
        if (waitingPool.isEmpty()) {
            Toast.makeText(this, "No entrants in waiting pool", Toast.LENGTH_SHORT).show();
            return;
        }
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Number of spots (Max: " + waitingPool.size() + ")");

        new AlertDialog.Builder(this)
                .setTitle("Draw Lottery")
                .setMessage("Enter number of entrants to select:")
                .setView(input)
                .setPositiveButton("Draw", (dialog, which) -> {
                    String str = input.getText().toString();
                    if (str.isEmpty()) return;
                    int count = Integer.parseInt(str);
                    if (count > waitingPool.size()) count = waitingPool.size();
                    lotterydraw(waitingPool, count);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Redraws for declined positions. Gets the redrawCount from the event,
     * selects that many users from the waiting pool, and resets the count to 0.
     */
    private void redrawDucks() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // First, fetch the current redrawCount from the event document
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Long redrawCountLong = doc.getLong("redrawCount");
                    int redrawCount = (redrawCountLong != null) ? redrawCountLong.intValue() : 0;

                    if (redrawCount <= 0) {
                        Toast.makeText(this, "No declined positions to redraw", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get all users with "waiting" status (still in the pool, not yet selected)
                    List<User> waitingPool = new ArrayList<>();
                    for (User u : allAttendees) {
                        String status = entrantStatusMap.get(u.getUserId());
                        if (status != null && status.equals("waiting")) {
                            waitingPool.add(u);
                        }
                    }

                    if (waitingPool.isEmpty()) {
                        Toast.makeText(this, "No waiting entrants available for redraw", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Confirm with the organizer before proceeding
                    int actualRedrawCount = Math.min(redrawCount, waitingPool.size());
                    new AlertDialog.Builder(this)
                            .setTitle("Redraw Ducks")
                            .setMessage("There are " + redrawCount + " declined position(s). " +
                                    "This will select " + actualRedrawCount + " new entrant(s) from " +
                                    waitingPool.size() + " waiting. Proceed?")
                            .setPositiveButton("Redraw", (dialog, which) -> {
                                performRedraw(waitingPool, actualRedrawCount);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Performs the actual redraw lottery and resets redrawCount to 0.
     *
     * @param pool - List of users eligible for redraw
     * @param count - Number of users to select
     */
    private void performRedraw(List<User> pool, int count) {
        if (pool.isEmpty() || count <= 0) return;

        java.util.Collections.shuffle(pool);
        List<User> winners = pool.subList(0, Math.min(count, pool.size()));

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        boolean hasUpdates = false;

        // Process winners - update their status to "selected"
        for (User winner : winners) {
            String docId = entrantDocIds.get(winner.getUserId());

            if (docId != null) {
                batch.update(db.collection("waitlist").document(docId), "status", "selected");
                hasUpdates = true;

                Map<String, Object> notif = new HashMap<>();
                notif.put("userId", winner.getUserId());
                notif.put("eventId", eventId);
                String title = getIntent().getStringExtra("eventTitle");
                notif.put("message", "Congratulations! You have been selected in a redraw for " + (title != null ? title : "an event"));
                notif.put("sentBy", currentUser.getUid());
                notif.put("timestamp", com.google.firebase.Timestamp.now());
                notif.put("type", "selected");
                batch.set(db.collection("notifications").document(), notif);
            }
        }

        // Reset redrawCount to 0
        batch.update(db.collection("events").document(eventId), "redrawCount", 0);

        if (hasUpdates) {
            batch.commit()
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Redraw complete! " + winners.size() + " new entrant(s) selected.", Toast.LENGTH_LONG).show();
                        loadWaitlistEntrants();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Redraw failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            Toast.makeText(this, "No valid entrants found to update", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Performs a lottery draw to randomly select winners from the pool.
     *
     * @param pool - List of users in the lottery pool
     * @param count - Number of winners to select
     */
    private void lotterydraw(List<User> pool, int count) {
        if (pool.isEmpty() || count <= 0) return;

        java.util.Collections.shuffle(pool);
        List<User> winners = pool.subList(0, count);
        List<User> losers = pool.subList(count, pool.size());

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        boolean hasUpdates = false;

        // process winner winner chicken dinners
        for (User winner : winners) {
            String docId = entrantDocIds.get(winner.getUserId());

            if (docId != null) {
                batch.update(db.collection("waitlist").document(docId), "status", "selected");
                hasUpdates = true;

                Map<String, Object> notif = new HashMap<>();
                notif.put("userId", winner.getUserId());
                notif.put("eventId", eventId);
                String title = getIntent().getStringExtra("eventTitle");
                notif.put("message", "congratulation! you are selected for " + (title != null ? title : "an event"));
                notif.put("sentBy", currentUser.getUid());
                notif.put("timestamp", com.google.firebase.Timestamp.now());
                notif.put("type", "selected");
                batch.set(db.collection("notifications").document(), notif);
            }
        }

        // process LOSERS
        for (User loser : losers) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("userId", loser.getUserId());
            notif.put("eventId", eventId);
            String title = getIntent().getStringExtra("eventTitle");
            notif.put("message",(title != null ? title : "event") + ": L you were not selected");
            notif.put("sentBy", currentUser.getUid());
            notif.put("timestamp", com.google.firebase.Timestamp.now());
            batch.set(db.collection("notifications").document(), notif);
            hasUpdates = true;
        }

        if (hasUpdates) {
            batch.commit()
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "lottery complete and notifications sent", Toast.LENGTH_LONG).show();
                        loadWaitlistEntrants();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "lottery failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            Toast.makeText(this, "no valid entrants found to update", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sends notifications to all cancelled or declined entrants.
     */
    private void notifyCancelledEntrants() {
        db.collection("waitlist")
                .whereEqualTo("eventId", eventId)
                .whereIn("status", java.util.Arrays.asList("cancelled", "declined"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, "No cancelled entrants found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String uid = doc.getString("userId");
                        Map<String, Object> notif = new HashMap<>();
                        notif.put("userId", uid);
                        notif.put("eventId", eventId);
                        String title = getIntent().getStringExtra("eventTitle");
                        notif.put("message", (title != null ? title : "Event") + ": your spot has been cancelled or declined");
                        notif.put("sentBy", currentUser.getUid());
                        notif.put("timestamp", com.google.firebase.Timestamp.now());
                        batch.set(db.collection("notifications").document(), notif);
                    }
                    batch.commit().addOnSuccessListener(v ->
                            Toast.makeText(this, "notifications sent to cancelled entrants", Toast.LENGTH_SHORT).show()
                    );
                });
    }

    /**
     * Displays the map popup showing attendee locations.
     */
    private void showMapPopup() {
        if (mapPopup != null) mapPopup.setVisibility(View.VISIBLE);
        if (mapPopupBackground != null) mapPopupBackground.setVisibility(View.VISIBLE);
        loadMapMarkers();
    }

    /**
     * Hides the map popup.
     */
    private void hideMapPopup() {
        if (mapPopup != null) mapPopup.setVisibility(View.GONE);
        if (mapPopupBackground != null) mapPopupBackground.setVisibility(View.GONE);
    }

    /**
     * Loads and displays markers on the map for all attendee locations.
     */
    private void loadMapMarkers() {
        if (map == null || eventId == null) return;
        map.getOverlays().clear();
        db.collection("waitlist")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
                    boolean hasPoints = false;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        WaitlistEntry entry = doc.toObject(WaitlistEntry.class);
                        if (entry != null && entry.getLatitude() != null && entry.getLongitude() != null) {
                            double lat = entry.getLatitude();
                            double lon = entry.getLongitude();
                            String uid = entry.getUserId();

                            if (lat < minLat) minLat = lat;
                            if (lat > maxLat) maxLat = lat;
                            if (lon < minLon) minLon = lon;
                            if (lon > maxLon) maxLon = lon;
                            hasPoints = true;

                            db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                                User u = userDoc.toObject(User.class);
                                String realName = (u != null && u.getFullName() != null) ? u.getFullName() : "Entrant";
                                GeoPoint point = new GeoPoint(lat, lon);
                                Marker marker = new Marker(map);
                                marker.setPosition(point);
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                marker.setTitle(realName);
                                marker.setSnippet("Status: " + entry.getStatus());
                                map.getOverlays().add(marker);
                                map.invalidate();
                            });
                        }
                    }
                    if (hasPoints) {
                        if (minLat == maxLat && minLon == maxLon) {
                            mapController.setCenter(new GeoPoint(minLat, minLon));
                            mapController.setZoom(15.0);
                        } else {
                            mapController.setCenter(new GeoPoint((minLat + maxLat)/2, (minLon + maxLon)/2));
                            mapController.setZoom(10.0);
                        }
                    }
                });
    }

    /**
     * Handles profile deletion by removing the user from the event waitlist.
     *
     * @param identifier - User ID or email of the user to remove
     */
    @Override
    public void onProfileDeleted(String identifier) {
        if (identifier == null || eventId == null) return;

        if (identifier.contains("@")) {
            db.collection("users")
                    .whereEqualTo("email", identifier)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(userSnap -> {
                        if (!userSnap.isEmpty()) {
                            String realUserId = userSnap.getDocuments().get(0).getId();
                            performKickByUserId(realUserId, identifier);
                        } else {
                            Toast.makeText(this, "Could not find user with email: " + identifier, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error finding user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
        else {
            performKickByUserId(identifier, identifier);
        }
    }

    /**
     * Removes a user from the event waitlist by their user ID.
     *
     * @param userId - The user ID to remove
     * @param displayId - The display identifier for user feedback
     */
    private void performKickByUserId(String userId, String displayId) {
        db.collection("waitlist")
                .whereEqualTo("userId", userId)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(ticketSnap -> {
                    if (ticketSnap.isEmpty()) {
                        Toast.makeText(this, "Ticket not found in DB.", Toast.LENGTH_SHORT).show();
                        removeFromLocalList(userId);
                        return;
                    }

                    DocumentSnapshot ticketDoc = ticketSnap.getDocuments().get(0);
                    String ticketDocId = ticketDoc.getId();
                    String currentStatus = ticketDoc.getString("status");
                    if (currentStatus == null) currentStatus = "waiting";
                    currentStatus = currentStatus.toLowerCase();

                    // Check if user was selected or accepted (had choice to be duck/goose or chose goose)
                    boolean wasSelectedOrAccepted = currentStatus.equals("selected") || currentStatus.equals("accepted");

                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    batch.update(db.collection("waitlist").document(ticketDocId), "status", "cancelled");

                    batch.update(db.collection("events").document(eventId),
                            "waitingList", com.google.firebase.firestore.FieldValue.arrayRemove(userId));

                    // If they were selected or accepted, increment redrawCount so organizer can redraw for this spot
                    if (wasSelectedOrAccepted) {
                        batch.update(db.collection("events").document(eventId),
                                "redrawCount", com.google.firebase.firestore.FieldValue.increment(1));
                    }

                    batch.update(db.collection("users").document(userId),
                            "waitlistedEventIds", com.google.firebase.firestore.FieldValue.arrayRemove(eventId));

                    java.util.Map<String, Object> notif = new java.util.HashMap<>();
                    notif.put("userId", userId);
                    notif.put("eventId", eventId);
                    String title = getIntent().getStringExtra("eventTitle");
                    notif.put("message", "You have been removed from " + (title != null ? title : "the event") + " by the organizer.");
                    notif.put("sentBy", currentUser.getUid());
                    notif.put("timestamp", com.google.firebase.Timestamp.now());

                    batch.set(db.collection("notifications").document(), notif);

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                removeFromLocalList(userId);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Kick failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Removes a user from the local attendee lists and updates the display.
     *
     * @param userId - The user ID to remove from local lists
     */
    private void removeFromLocalList(String userId) {
        for (int i = 0; i < attendees.size(); i++) {
            if (attendees.get(i).getUserId().equals(userId)) {
                String name = attendees.get(i).getFullName();
                attendees.remove(i);
                if (adapter != null) adapter.notifyItemRemoved(i);

                Toast.makeText(this, (name != null ? name : "User") + " has been kicked", Toast.LENGTH_SHORT).show();
                break;
            }
        }
        for (int i = 0; i < allAttendees.size(); i++) {
            if (allAttendees.get(i).getUserId().equals(userId)) {
                allAttendees.remove(i);
                break;
            }
        }
        updateCountDisplay();
    }

    /**
     * No-op for attendees; events button is not used here.
     *
     * @param userId - Target user ID
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        // Not used for attendees
    }
}