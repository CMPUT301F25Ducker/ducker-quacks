package com.example.duckduckgoose;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.duckduckgoose.waitlist.WaitlistEntry;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

/**
 * Detail screen for a single event.
 *
 * <p>Displays event metadata such as title, description, dates, cost, spots, and poster.
 * Provides actions such as joining or leaving the waitlist and organizer controls.</p>
 *
 * <p><b>Author:</b> DuckDuckGoose Development Team</p>
 */
public class EventDetailActivity extends AppCompatActivity {

    /** Possible UI/participation states for the current user relative to the event. */
    enum State { UNDECIDED, NOT_IN_CIRCLE, LEAVE_CIRCLE, DUCK, GOOSE, WAITING_LIST, LEAVE_WAITING_LIST }

    private State currentState = State.UNDECIDED;
    private boolean isOrganizerMode = false;
    private String eventId;
    private FirebaseFirestore db;

    private Event currentEvent;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    /**
     * Initializes the layout, reads {@link Intent} extras, and wires button actions.
     *
     * @param savedInstanceState saved activity state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        WindowInsetsController controller = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller = getWindow().getInsetsController();
        }
        if (controller != null) {
            controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        locatejoin();
                    } else {
                        Toast.makeText(this, "location permission not granted \uD83D\uDC4E", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Determine mode (organizer or entrant)
        isOrganizerMode = AppConfig.LOGIN_MODE.equals("ORGANIZER");

        TopBarWiring.attachProfileSheet(this);

        // Top bar navigation to "My Events"
        View tbMy = findViewById(R.id.btnMyEvents);
        if (tbMy != null) {
            tbMy.setOnClickListener(v -> {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra("startOn", "MY_EVENTS");
                startActivity(i);
            });
        }

        // Read incoming data
        Intent i = getIntent();
        String title    = i.getStringExtra("title");
        String dateText = i.getStringExtra("dateText");
        long   open     = i.getLongExtra("open", 0L);
        long   deadline = i.getLongExtra("deadline", 0L);
        String cost     = i.getStringExtra("cost");
        String spots    = i.getStringExtra("spots");
        int    poster   = i.getIntExtra("posterRes", R.drawable.poolphoto);
        int    stateInt = i.getIntExtra("state", -1);
        this.eventId    = i.getStringExtra("eventId");

        // Firestore
        db = FirebaseFirestore.getInstance();
        if (this.eventId != null) {
            loadEventDetails();
        }

        // Bind text views
        TextView tvTitle     = findViewById(R.id.txtEventTitle);
        TextView tvDesc      = findViewById(R.id.txtDescription);
        TextView tvDates     = findViewById(R.id.txtDates);
        TextView tvOpen      = findViewById(R.id.txtOpen);
        TextView tvDeadline  = findViewById(R.id.txtDeadline);
        TextView tvCost      = findViewById(R.id.txtCost);
        TextView tvSpots     = findViewById(R.id.txtSpots);

        if (tvTitle != null)    tvTitle.setText(title != null ? title : "Event");
        if (tvDesc != null)     tvDesc.setText("Event details will be loaded from backend later.");
        if (tvDates != null)    tvDates.setText(dateText != null ? dateText : "TBD");
        if (tvOpen != null)     tvOpen.setText("Registration Opens: " + (open == 0 ? "TBD" : new java.util.Date(open)));
        if (tvDeadline != null) tvDeadline.setText("Registration Deadline: " + (deadline == 0 ? "TBD" : new java.util.Date(deadline)));
        if (tvCost != null)     tvCost.setText("Cost: " + (cost == null ? "—" : cost));
        if (tvSpots != null)    tvSpots.setText("Spots: " + (spots == null ? "—" : spots));

        // Image gallery
        LinearLayout gallery = findViewById(R.id.imageGallery);
        if (gallery != null) {
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int heightPx = (int) (280 * getResources().getDisplayMetrics().density); // ~280dp

            int[] photos = { poster, R.drawable.poolphoto, R.drawable.poolphoto };
            for (int res : photos) {
                ImageView img = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(screenW, heightPx);
                lp.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
                img.setLayoutParams(lp);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setImageResource(res);
                gallery.addView(img);
            }
        }

        // Determine initial state
        currentState = (stateInt >= 0 && stateInt < State.values().length)
                ? State.values()[stateInt]
                : pickStateFromTitle(title);

        // Show entrant guidelines popup (lottery odds)
        if (!isOrganizerMode) {
            showLotteryGuidelines(spots);
        }

        if (isOrganizerMode) {
            setupOrganizerButtons(title, dateText, open, deadline, cost, spots);
        } else {
            applyState(currentState);
            setupEntrantButtons();
        }
    }

    /**
     * Displays a popup dialog explaining the lottery registration system.
     *
     * @param spots string describing available spots (used to estimate odds)
     */
    private void showLotteryGuidelines(String spots) {
        try {
            String oddsText = "Odds: unknown";
            if (spots != null) {
                String digits = spots.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    try {
                        int spotsCount = Integer.parseInt(digits);
                        if (spotsCount > 0) {
                            double pct = 100.0 / (double) spotsCount;
                            String pctStr = String.format(java.util.Locale.getDefault(), "%.2f%%", pct);
                            oddsText = "Odds of winning: " + pctStr + " (1 in " + spotsCount + ")";
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }

            String message = "Events use a lottery system. The odds of winning depend on the number of applicants. " +
                    "By registering you acknowledge you have been informed of the lottery process and odds.\n\n" + oddsText;

            new AlertDialog.Builder(this)
                    .setTitle("Guidelines for Lottery")
                    .setMessage(message)
                    .setPositiveButton("OK, I understand", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        } catch (Exception ignored) { }
    }

    /** Loads event details from Firestore and populates the UI. */
    private void loadEventDetails() {
        if (db == null || eventId == null) return;

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        this.currentEvent = doc.toObject(Event.class);
                        if (currentEvent != null) {
                            if (currentEvent.getEventId() == null) currentEvent.setEventId(eventId);
                            TextView tvTitle     = findViewById(R.id.txtEventTitle);
                            TextView tvDesc      = findViewById(R.id.txtDescription);
                            TextView tvDates     = findViewById(R.id.txtDates);
                            TextView tvOpen      = findViewById(R.id.txtOpen);
                            TextView tvDeadline  = findViewById(R.id.txtDeadline);
                            TextView tvCost      = findViewById(R.id.txtCost);
                            TextView tvSpots     = findViewById(R.id.txtSpots);

                            if (tvTitle != null)    tvTitle.setText(currentEvent.getName() != null ? currentEvent.getName() : "Event");
                            if (tvDesc != null)     tvDesc.setText("Event details loaded from backend.");
                            if (tvDates != null)    tvDates.setText(currentEvent.getEventDate() != null ? currentEvent.getEventDate() : "TBD");
                            if (tvOpen != null)     tvOpen.setText(currentEvent.getRegistrationOpens() != null ? currentEvent.getRegistrationOpens() : "TBD");
                            if (tvDeadline != null) tvDeadline.setText(currentEvent.getRegistrationCloses() != null ? currentEvent.getRegistrationCloses() : "TBD");
                            if (tvCost != null)     tvCost.setText("Cost: " + (currentEvent.getCost() != null ? currentEvent.getCost() : "—"));
                            if (tvSpots != null)    tvSpots.setText("Spots: " + (currentEvent.getMaxSpots() != null ? currentEvent.getMaxSpots() : "—"));

                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null && currentEvent.isOnWaitingList(currentUser.getUid())) {
                                currentState = State.LEAVE_WAITING_LIST;
                            } else {
                                currentState = State.WAITING_LIST;
                            }
                            applyState(currentState);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show());
    }

    /**
     * Configures organizer-only buttons and their actions.
     *
     * @param title     event title
     * @param dateText  event date text
     * @param open      registration open time (epoch millis)
     * @param deadline  registration deadline (epoch millis)
     * @param cost      event cost string
     * @param spots     spots string
     */
    private void setupOrganizerButtons(String title, String dateText, long open, long deadline, String cost, String spots) {
        // Hide entrant buttons
        View areaButtons = findViewById(R.id.areaButtons);
        View singleArea = findViewById(R.id.singleCtaArea);
        View organizerArea = findViewById(R.id.organizerButtonsArea);

        if (areaButtons != null) areaButtons.setVisibility(View.GONE);
        if (singleArea != null) singleArea.setVisibility(View.GONE);
        if (organizerArea != null) organizerArea.setVisibility(View.VISIBLE);

        MaterialButton btnEdit = findViewById(R.id.btnEditEventDetail);
        MaterialButton btnAttendeeManager = findViewById(R.id.btnAttendeeManagerDetail);
        MaterialButton btnDelete = findViewById(R.id.btnDeleteEventDetail);

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, EventEditActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("title", title);
                intent.putExtra("date", dateText);
                intent.putExtra("cost", cost);
                intent.putExtra("spots", spots);
                startActivity(intent);
            });
        }

        if (btnAttendeeManager != null) {
            btnAttendeeManager.setOnClickListener(v -> {
                Intent intent = new Intent(this, AttendeeManagerActivity.class);
                intent.putExtra("eventTitle", title);
                if (eventId != null) intent.putExtra("eventId", eventId);
                startActivity(intent);
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                // In production: confirm deletion
                finish();
            });
        }
    }

    /**
     * Wires entrant-facing buttons (accept/decline and single CTA) and their behavior.
     */
    private void setupEntrantButtons() {
        // Hide organizer buttons
        View organizerArea = findViewById(R.id.organizerButtonsArea);
        if (organizerArea != null) organizerArea.setVisibility(View.GONE);

        // Accept/Decline pair
        View areaButtons = findViewById(R.id.areaButtons);
        if (areaButtons != null) {
            MaterialButton btnDecline = findViewById(R.id.btnLeft);
            MaterialButton btnAccept  = findViewById(R.id.btnRight);

            if (btnDecline != null) {
                btnDecline.setOnClickListener(v ->
                        animateTap(v, this::performDecline)
//                        animateTap(v, () -> {
//                            currentState = State.DUCK; // Decline -> DUCK (per your UI metaphor)
//                            applyState(currentState);
//                        })
                );
            }
            if (btnAccept != null) {
                btnAccept.setOnClickListener(v ->
                        animateTap(v, this::performAcceptFromWaitlist)
                );
            }
        }

        // Single CTA
        MaterialButton btnSingle = findViewById(R.id.btnSingleCta);
        View singleArea = findViewById(R.id.singleCtaArea);
        if (btnSingle != null && singleArea != null) {
            btnSingle.setOnClickListener(v ->
                    animateTap(v, () -> {
                        if (currentState == State.NOT_IN_CIRCLE) currentState = State.LEAVE_CIRCLE;
                        else if (currentState == State.LEAVE_CIRCLE) currentState = State.NOT_IN_CIRCLE;
                        else if (currentState == State.WAITING_LIST) {
                            joinclick();
                            return;
                        } else if (currentState == State.LEAVE_WAITING_LIST) {
                            leaveWaitingList();
                            return;
                        }
                        applyState(currentState);
                    })
            );
        }
    }

    private void joinclick() {
        if (currentEvent.isGeolocationEnabled()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locatejoin();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            performJoin(FirebaseAuth.getInstance().getUid(), eventId, null, null);
        }
    }


    private void locatejoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    String uid = FirebaseAuth.getInstance().getUid();
                    if (location != null) {
                        performJoin(uid, eventId, location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(this, "join without location", Toast.LENGTH_SHORT).show();
                        performJoin(uid, eventId, null, null);
                    }
                });
    }

    private void performJoin(String uid, String eid, Double lat, Double lon) {
        if (currentEvent == null) return;
        currentEvent.addToWaitingList(uid, lat, lon);
        Toast.makeText(this, "join waiting list", Toast.LENGTH_SHORT).show();
        currentState = State.LEAVE_WAITING_LIST;
        applyState(currentState);
    }

    /**
     * Removes the current user from the waiting list for this event.
     */
    private void leaveWaitingList() {
        if (db == null || eventId == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        if (currentEvent != null) {
            currentEvent.removeFromWaitingList(currentUser.getUid());
        } else {
            WriteBatch batch = db.batch();
            batch.update(db.collection("events").document(eventId), "waitingList", FieldValue.arrayRemove(currentUser.getUid()));
            batch.update(db.collection("users").document(currentUser.getUid()), "waitlistedEventIds", FieldValue.arrayRemove(eventId));
            batch.delete(db.collection("waitlist").document(currentUser.getUid() + "_" + eventId));
            batch.commit();
        }
        Toast.makeText(this, "left waiting list", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Accepts an event from the waiting list for the current user, updating all relevant
     * Firestore collections/documents.
     */
    private void performAcceptFromWaitlist() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();
        String eid = this.eventId;
        if (eid == null) {
            Toast.makeText(this, "Event identifier not available", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestore = db;
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    final String userName = userDoc.getString("fullName");
                    final String eventName = (currentEvent != null) ? currentEvent.getName() : "Event";

                    WriteBatch batch = firestore.batch();

                    // Update WaitlistEntry
                    DocumentReference waitRef = firestore.collection("waitlist").document(uid + "_" + eid);

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("status", "accepted");
                    updates.put("acceptedAt", com.google.firebase.Timestamp.now());
                    if (userName != null) updates.put("userName", userName);
                    if (eventName != null) updates.put("eventName", eventName);

                    batch.update(waitRef, updates);

                    // 2. Update event arrays
                    DocumentReference eventRef = firestore.collection("events").document(eid);
                    batch.update(eventRef,
                            "waitingList", FieldValue.arrayRemove(uid),
                            "acceptedFromWaitlist", FieldValue.arrayUnion(uid)
                    );

                    // 3. Update user arrays
                    DocumentReference userRef = firestore.collection("users").document(uid);
                    batch.update(userRef,
                            "waitlistedEventIds", FieldValue.arrayRemove(eid),
                            "acceptedEventIds", FieldValue.arrayUnion(eid)
                    );

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Successfully accepted the event!", Toast.LENGTH_SHORT).show();
                                currentState = State.GOOSE;
                                applyState(currentState);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to accept event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show());
    }

    /**
     * Applies the given state to the UI, toggling visibility and button styles.
     *
     * @param s new state to apply
     */
    private void applyState(State s) {
        View twoBtns = findViewById(R.id.areaButtons);
        View single  = findViewById(R.id.singleCtaArea);
        MaterialButton btn = findViewById(R.id.btnSingleCta);
        if (twoBtns == null || single == null || btn == null) return;

        int charcoal = ContextCompat.getColor(this, R.color.fg_charcoal);
        int white    = ContextCompat.getColor(this, R.color.fg_white);

        switch (s) {
            case UNDECIDED:
                twoBtns.setVisibility(View.VISIBLE);
                single.setVisibility(View.GONE);
                break;

            case NOT_IN_CIRCLE:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Enter Registration Circle");
                btn.setBackgroundResource(R.drawable.btn_primary_green);
                btn.setTextColor(charcoal);
                break;

            case LEAVE_CIRCLE:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Leave Registration Circle");
                btn.setBackgroundResource(R.drawable.btn_secondary_silver);
                btn.setTextColor(charcoal);
                break;

            case DUCK:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Duck");
                btn.setBackgroundResource(R.drawable.btn_filled_brick);
                btn.setTextColor(white);
                break;

            case GOOSE:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Goose");
                btn.setBackgroundResource(R.drawable.btn_primary_green);
                btn.setTextColor(charcoal);
                break;

            case WAITING_LIST:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Join Waiting List");
                btn.setBackgroundResource(R.drawable.btn_primary_green);
                btn.setTextColor(charcoal);
                break;

            case LEAVE_WAITING_LIST:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Leave Waiting List");
                btn.setBackgroundResource(R.drawable.btn_secondary_silver);
                btn.setTextColor(charcoal);
                break;
        }
    }

    /**
     * Small button press animation, then runs the given action.
     *
     * @param v     view to animate
     * @param after action to run after the animation completes
     */
    private void animateTap(View v, Runnable after) {
        v.animate()
                .scaleX(0.96f).scaleY(0.96f).setDuration(80)
                .withEndAction(() ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(110).withEndAction(after).start()
                ).start();
    }

    /**
     * Picks an initial state based on keywords in the event title.
     *
     * @param title event title (nullable)
     * @return inferred state or {@link State#UNDECIDED} if unknown
     */
    private State pickStateFromTitle(String title) {
        if (title == null) return State.UNDECIDED;
        String t = title.toLowerCase(java.util.Locale.ROOT);
        if (t.contains("duck"))  return State.DUCK;
        if (t.contains("goose")) return State.GOOSE;
        if (t.contains("enter")) return State.NOT_IN_CIRCLE;
        if (t.contains("leave")) return State.LEAVE_CIRCLE;
        return State.UNDECIDED;
    }

    private void performDecline() {
        if (currentEvent == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String eid = currentEvent.getEventId();

        // firestore: change status to declined
        // we do NOT remove the document, because we want to keep a record that they declined
        db.collection("waitlist").document(uid + "_" + eid)
                .update("status", "declined")
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "You have declined the invitation.", Toast.LENGTH_SHORT).show();

                    WriteBatch batch = db.batch();
                    batch.update(db.collection("events").document(eid),
                            "waitingList", FieldValue.arrayRemove(uid));
                    batch.update(db.collection("users").document(uid),
                            "waitlistedEventIds", FieldValue.arrayRemove(eid));
                    batch.commit();

                    currentState = State.DUCK;
                    applyState(currentState);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to decline: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
