/**
 * @file EventDetailActivity.java
 * @brief Detail screen for a single event.
 *
 * Displays event metadata (title, description, dates, cost, spots, poster) and
 * provides actions such as join/waitlist, open map, or contact organizer.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.content.ClipboardManager;
import android.content.ClipData;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.example.duckduckgoose.waitlist.WaitlistEntry;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * @class EventDetailActivity
 * @brief Activity to show event details and related actions.
 *
 * Reads Intent extras for event fields and updates the UI accordingly.
 */
public class EventDetailActivity extends AppCompatActivity {

    enum State { UNDECIDED, NOT_IN_CIRCLE, LEAVE_CIRCLE, DUCK, GOOSE, WAITING_LIST, LEAVE_WAITING_LIST }

    private State currentState = State.UNDECIDED;
    private boolean isOrganizerMode = false;
    private String eventId;
    private FirebaseFirestore db;

    /**
     * @brief Initializes the layout, reads Intent extras, and wires actions.
     * @param savedInstanceState Saved activity state.
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

        // MUST inflate layout first so views exist
        setContentView(R.layout.activity_event_detail);

        // Check if organizer mode
        isOrganizerMode = AppConfig.LOGIN_MODE.equals("ORGANIZER");

        TopBarWiring.attachProfileSheet(this);

        // --- Top bar nav (My Events / Events) ---
        View tbMy = findViewById(R.id.btnMyEvents);
        if (tbMy != null) {
            tbMy.setOnClickListener(v -> {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra("startOn", "MY_EVENTS");
                startActivity(i);
            });
        }

        // ---- read extras ----
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

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Load event details if we have an eventId
        if (this.eventId != null) {
            loadEventDetails();
        }

        // ---- bind text views ----
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

        // Wire QR share button
        com.google.android.material.button.MaterialButton btnShowQr = findViewById(R.id.btnShowQr);
        if (btnShowQr != null) {
            btnShowQr.setOnClickListener(v -> showEventQrDialog());
        }

        // ---- optional image gallery ----
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

        // ---- determine initial state & apply ----
        currentState = (stateInt >= 0 && stateInt < State.values().length)
                ? State.values()[stateInt]
                : pickStateFromTitle(title);
        
        // if this user is an entrant (not an organizer), show lottery-info popup before they register
        // TODO -- also might wanna consider adding a check here for administrators with like "isAdministratorMode" to get this confirmed (as admins need not see terms and conditions type stuff yk)
        if (!isOrganizerMode) {
            try {
                // compute odds from provided spots string if possible
                String oddsText = "Odds: unknown";
                if (spots != null) {
                    // extract digits from spots (e.g. "10" or "10 spots")
                    String digits = spots.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        try {
                            int spotsCount = Integer.parseInt(digits);
                            if (spotsCount > 0) {
                                double pct = 100.0 / (double) spotsCount; // 1 / spots -> percent
                                String pctStr = String.format(java.util.Locale.getDefault(), "%.2f%%", pct);
                                oddsText = "Odds of winning: " + pctStr + " (1 in " + spotsCount + ")";
                            }
                        } catch (NumberFormatException nfe) {
                            // leave oddsText as unknown
                        }
                    }
                }

                // this is the old standard message that Dhruv was hating on smh.
                String message = "Events use a lottery system. The odds of winning depend on the number of applicants. By registering you acknowledge you have been informed of the lottery process and odds.\n\n" + oddsText;

                // who thought StringBuilder was the wise choice for this?
                new AlertDialog.Builder(this)
                        .setTitle("Guidelines for Lottery")
                        .setMessage(message) // this is the pop up bluf from string builder
                        .setPositiveButton("OK, I understand", (dialog, which) -> dialog.dismiss())
                        .setCancelable(true)
                        .show();
            } catch (Exception ex) {
                // defensive: if dialog cannot be shown, ignore
            }
        }

        if (isOrganizerMode) {
            setupOrganizerButtons(title, dateText, open, deadline, cost, spots);
        } else {
            applyState(currentState);
            setupEntrantButtons();
        }
    }

    private void setupOrganizerButtons(String title, String dateText, long open, long deadline, String cost, String spots) {
        // Hide entrant buttons
        View areaButtons = findViewById(R.id.areaButtons);
        View singleArea = findViewById(R.id.singleCtaArea);
        View organizerArea = findViewById(R.id.organizerButtonsArea);

        if (areaButtons != null) areaButtons.setVisibility(View.GONE);
        if (singleArea != null) singleArea.setVisibility(View.GONE);
        if (organizerArea != null) organizerArea.setVisibility(View.VISIBLE);

        // Setup organizer buttons
        com.google.android.material.button.MaterialButton btnEdit = findViewById(R.id.btnEditEventDetail);
        com.google.android.material.button.MaterialButton btnAttendeeManager = findViewById(R.id.btnAttendeeManagerDetail);
        com.google.android.material.button.MaterialButton btnDelete = findViewById(R.id.btnDeleteEventDetail);

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
                startActivity(intent);
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                // In production, show confirmation dialog
                finish();
            });
        }
    }

    private void loadEventDetails() {
        if (db == null || eventId == null) return;

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            // Fill all event info fields from db
                            TextView tvTitle     = findViewById(R.id.txtEventTitle);
                            TextView tvDesc      = findViewById(R.id.txtDescription);
                            TextView tvDates     = findViewById(R.id.txtDates);
                            TextView tvOpen      = findViewById(R.id.txtOpen);
                            TextView tvDeadline  = findViewById(R.id.txtDeadline);
                            TextView tvCost      = findViewById(R.id.txtCost);
                            TextView tvSpots     = findViewById(R.id.txtSpots);

                            if (tvTitle != null)    tvTitle.setText(event.getName() != null ? event.getName() : "Event");
                            if (tvDesc != null)     tvDesc.setText("Event details loaded from backend.");
                            if (tvDates != null)    tvDates.setText(event.getEventDate() != null ? event.getEventDate() : "TBD");
                            if (tvOpen != null)     tvOpen.setText("Registration Opens: " + (event.getRegistrationOpens() != null ? event.getRegistrationOpens() : "TBD"));
                            if (tvDeadline != null) tvDeadline.setText("Registration Deadline: " + (event.getRegistrationCloses() != null ? event.getRegistrationCloses() : "TBD"));
                            if (tvCost != null)     tvCost.setText("Cost: " + (event.getCost() != null ? event.getCost() : "—"));
                            if (tvSpots != null)    tvSpots.setText("Spots: " + (event.getMaxSpots() != null ? event.getMaxSpots() : "—"));

                            // Check if current user is on waiting list
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null && event.isOnWaitingList(currentUser.getUid())) {
                                currentState = State.LEAVE_WAITING_LIST;
                            } else {
                                currentState = State.WAITING_LIST;
                            }
                            applyState(currentState);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
                });
    }

    private void joinWaitingList() {
        if (db == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        // If we don't have an eventId, try to resolve it from the provided title (best-effort)
        if (eventId == null) {
            String title = getIntent().getStringExtra("title");
            if (title == null) {
                Toast.makeText(this, "Event identifier not available", Toast.LENGTH_SHORT).show();
                return;
            }
            // Query Firestore for an event with this name
            db.collection("events").whereEqualTo("name", title).limit(1).get()
                    .addOnSuccessListener(qs -> {
                        if (qs != null && !qs.isEmpty()) {
                            String resolvedId = qs.getDocuments().get(0).getId();
                            // set and proceed
                            this.eventId = resolvedId;
                            performJoin(uid, resolvedId);
                        } else {
                            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to resolve event", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        // We have an eventId — perform join
        performJoin(uid, eventId);
    }

    private void performJoin(String uid, String resolvedEventId) {
        WaitlistEntry entry = new WaitlistEntry(uid, resolvedEventId);
        FirebaseFirestore firestore = db;
        WriteBatch batch = firestore.batch();

        DocumentReference waitRef = firestore.collection("waitlist").document(uid + "_" + resolvedEventId);
        batch.set(waitRef, entry);

        DocumentReference eventRef = firestore.collection("events").document(resolvedEventId);
        batch.update(eventRef, "waitingList", FieldValue.arrayUnion(uid));

        DocumentReference userRef = firestore.collection("users").document(uid);
        batch.update(userRef, "waitlistedEventIds", FieldValue.arrayUnion(resolvedEventId));

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully joined waiting list", Toast.LENGTH_SHORT).show();
                    currentState = State.LEAVE_WAITING_LIST;
                    applyState(currentState);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to join waiting list", Toast.LENGTH_SHORT).show();
                });
    }

    private void leaveWaitingList() {
        if (db == null || eventId == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        WriteBatch batch = db.batch();

        // Remove user from event's waitingList
        DocumentReference eventRef = db.collection("events").document(eventId);
        batch.update(eventRef, "waitingList", FieldValue.arrayRemove(uid));

        // Remove event from user's waitlistedEventIds
        DocumentReference userRef = db.collection("users").document(uid);
        batch.update(userRef, "waitlistedEventIds", FieldValue.arrayRemove(eventId));

        // Delete waitlist entry
        DocumentReference waitlistRef = db.collection("waitlist").document(uid + "_" + eventId);
        batch.delete(waitlistRef);

        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Successfully left waiting list", Toast.LENGTH_SHORT).show();
                finish(); // Return to My Events page
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to leave waiting list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void setupEntrantButtons() {
        // Hide organizer buttons
        View organizerArea = findViewById(R.id.organizerButtonsArea);
        if (organizerArea != null) organizerArea.setVisibility(View.GONE);

        // ---- wire Accept/Decline ----
        View areaButtons = findViewById(R.id.areaButtons);
        if (areaButtons != null && areaButtons.getVisibility() == View.VISIBLE) {
            com.google.android.material.button.MaterialButton btnDecline = findViewById(R.id.btnLeft);
            com.google.android.material.button.MaterialButton btnAccept  = findViewById(R.id.btnRight);

            if (btnDecline != null) {
                btnDecline.setOnClickListener(v ->
                        animateTap(v, () -> {
                            currentState = State.DUCK;      // Decline -> DUCK
                            applyState(currentState);
                        })
                );
            }
            if (btnAccept != null) {
                btnAccept.setOnClickListener(v ->
                        animateTap(v, () -> {
                            // Accept: update waitlist collection for this user/event
                            performAcceptFromWaitlist();
                        })
                );
            }
        }

        // Single CTA (optional toggle)
        com.google.android.material.button.MaterialButton btnSingle = findViewById(R.id.btnSingleCta);
        View singleArea = findViewById(R.id.singleCtaArea);
        if (btnSingle != null && singleArea != null && singleArea.getVisibility() == View.VISIBLE) {
            btnSingle.setOnClickListener(v ->
                    animateTap(v, () -> {
                        if (currentState == State.NOT_IN_CIRCLE) currentState = State.LEAVE_CIRCLE;
                        else if (currentState == State.LEAVE_CIRCLE) currentState = State.NOT_IN_CIRCLE;
                        else if (currentState == State.WAITING_LIST) {
                            joinWaitingList();
                        } else if (currentState == State.LEAVE_WAITING_LIST) {
                            leaveWaitingList();
                        }
                        applyState(currentState);
                    })
            );
        }
    }

    // ✅ FIXED: moved outside of setupEntrantButtons
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

        // First fetch user and event details for the waitlist entry
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener(userDoc -> {
                final String userName = userDoc.getString("fullName");
                
                firestore.collection("events").document(eid).get()
                    .addOnSuccessListener(eventDoc -> {
                        final String eventName = eventDoc.getString("name");
                        
                        WriteBatch batch = firestore.batch();

                        // Create waitlist entry with names
                        WaitlistEntry entry = new WaitlistEntry(uid, eid);
                        entry.setStatus("accepted");
                        entry.setUserName(userName);
                        entry.setEventName(eventName);
                        DocumentReference waitRef = firestore.collection("waitlist").document(uid + "_" + eid);
                        batch.set(waitRef, entry);

                        // Update event's arrays
                        DocumentReference eventRef = firestore.collection("events").document(eid);
                        batch.update(eventRef, 
                            "waitingList", FieldValue.arrayUnion(uid),
                            "acceptedFromWaitlist", FieldValue.arrayUnion(uid)
                        );

                        // Update user's arrays
                        DocumentReference userRef = firestore.collection("users").document(uid);
                        batch.update(userRef,
                            "waitlistedEventIds", FieldValue.arrayUnion(eid),
                            "acceptedEventIds", FieldValue.arrayUnion(eid)
                        );

                        batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Successfully accepted the event!", Toast.LENGTH_SHORT).show();
                                currentState = State.GOOSE;
                                applyState(currentState);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to accept event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to fetch event details", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
            });
    }

    private void animateTap(View v, Runnable after) {
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).withEndAction(after).start()
        ).start();
    }

    private State pickStateFromTitle(String title) {
        if (title == null) return State.UNDECIDED;
        String t = title.toLowerCase(java.util.Locale.ROOT);
        if (t.contains("duck"))  return State.DUCK;
        if (t.contains("goose")) return State.GOOSE;
        if (t.contains("enter")) return State.NOT_IN_CIRCLE;
        if (t.contains("leave")) return State.LEAVE_CIRCLE;
        return State.UNDECIDED;
    }

    private void applyState(State s) {
        View twoBtns = findViewById(R.id.areaButtons);
        View single  = findViewById(R.id.singleCtaArea);
        com.google.android.material.button.MaterialButton btn = findViewById(R.id.btnSingleCta);
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
                twoBtns.setVisibility(View.VISIBLE);
                single.setVisibility(View.GONE);
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
     * Show a dialog with a QR code that encodes a deep link to this event.
     * Scanning the QR should allow another device to open the event (if app handles the scheme).
     */
    private void showEventQrDialog() {
        String link;
        if (this.eventId != null) {
            link = "duckduckgoose://event?eventId=" + this.eventId;
        } else {
            String title = getIntent().getStringExtra("title");
            try {
                link = "duckduckgoose://event?title=" + java.net.URLEncoder.encode(title != null ? title : "", "UTF-8");
            } catch (Exception ex) {
                link = "duckduckgoose://event";
            }
        }

        try {
            int size = 600;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(link, BarcodeFormat.QR_CODE, size, size);
            int width = bitMatrix.getWidth();
            Bitmap bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < width; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            int pad = (int) (12 * getResources().getDisplayMetrics().density);
            iv.setPadding(pad, pad, pad, pad);

            String finalLink = link;
            new AlertDialog.Builder(this)
                    .setTitle("Scan to open event")
                    .setView(iv)
                    .setNeutralButton("Copy Link", (d, w) -> {
                        try {
                            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData cd = ClipData.newPlainText("event-link", finalLink);
                            if (cm != null) cm.setPrimaryClip(cd);
                            Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
                        } catch (Exception ex) {
                            Toast.makeText(this, "Failed to copy link", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setPositiveButton("Close", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
