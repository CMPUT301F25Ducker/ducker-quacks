/**
 * @file EventEditActivity.java
 * @brief Organizer UI to create or edit events (name, dates, cost, spots, images, geolocation).
 *
 * Modes:
 *  - "create": creates a new Firestore document in `events/`.
 *  - "edit":   loads an existing event by `eventId` and updates it.
 *
 * Notes:
 *  - Dates are stored as "MM/dd/yy" strings to match the rest of the app.
 *  - Images are kept as a simple list of URIs/paths (strings) for now.
 *  - OrganizerId is attached from the current FirebaseAuth user when available.
 * 
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * @class EventEditActivity
 * @brief Activity for creating and editing events with images and dates.
 *
 * Handles both "create" and "edit" modes, binds form fields, wires date pickers
 * and image selection, validates input, and persists changes to Firestore.
 */
public class EventEditActivity extends AppCompatActivity {

    private static final String TAG = "EventEditActivity";

    // --- Mode & data sources ---
    private String mode; // "create" or "edit"
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private @Nullable String eventId; // Firestore document id when editing

    // --- Form fields ---
    private EditText edtEventName;
    private EditText edtSpots;
    private EditText edtCost;
    private EditText txtEventDate;
    private EditText txtRegOpens;
    private EditText txtRegCloses;
    private CheckBox chkGeolocation;

    // --- Images ---
    private LinearLayout imageContainer;
    private final List<String> imagePaths = new ArrayList<>();
    private CardView sheetImageSelect;
    private MaterialButton btnAddImage;

    // --- Buttons ---
    private MaterialButton btnSaveChanges;
    private MaterialButton btnCreateEvent;
    private MaterialButton btnCancel;
    private MaterialButton btnDeleteEvent;
    private MaterialButton btnAttendeeManager;

    // --- Date pickers ---
    private final Calendar eventDate = Calendar.getInstance();
    private final Calendar regOpensDate = Calendar.getInstance();
    private final Calendar regClosesDate = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy", Locale.US);

    // --- Image picker ---
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    /**
     * @brief Initializes the editor screen and wires all UI, pickers, and actions.
     *
     * Sets up edge-to-edge display and status bar appearance, binds views,
     * registers the gallery picker, determines mode ("create" or "edit"), loads
     * existing event data when applicable, and configures button visibility and
     * handlers.
     *
     * @param savedInstanceState Previously saved instance state; may be null.
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
        setContentView(R.layout.activity_event_edit);

        // Firestore
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        // Image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            addImageToContainer(imageUri.toString());
                            Toast.makeText(this, "Image added", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Mode
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "create";

        // If editing, prefer an explicit eventId from the caller
        eventId = getIntent().getStringExtra("eventId");

        initializeViews();
        setupClickListeners();

        // Title & button visibility by mode
        TextView txtTopBarTitle = findViewById(R.id.txtTopBarTitle);
        if ("edit".equalsIgnoreCase(mode)) {
            if (txtTopBarTitle != null) txtTopBarTitle.setText("Edit Event");
            btnCreateEvent.setVisibility(View.GONE);
            btnSaveChanges.setVisibility(View.VISIBLE);
            btnDeleteEvent.setVisibility(View.VISIBLE);
            btnAttendeeManager.setVisibility(View.VISIBLE);
            loadEventData();
        } else {
            if (txtTopBarTitle != null) txtTopBarTitle.setText("New Event");
            btnSaveChanges.setVisibility(View.GONE);
            btnCreateEvent.setVisibility(View.VISIBLE);
            btnDeleteEvent.setVisibility(View.GONE);
            btnAttendeeManager.setVisibility(View.GONE);
        }

        // Back
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    /** Bind views. */
    private void initializeViews() {
        edtEventName = findViewById(R.id.edtEventName);
        edtSpots     = findViewById(R.id.edtSpots);
        edtCost      = findViewById(R.id.edtCost);
        txtEventDate = findViewById(R.id.txtEventDate);
        txtRegOpens  = findViewById(R.id.txtRegOpens);
        txtRegCloses = findViewById(R.id.txtRegCloses);
        chkGeolocation = findViewById(R.id.chkGeolocation);

        imageContainer   = findViewById(R.id.imageContainer);
        sheetImageSelect = findViewById(R.id.sheetImageSelect);

        btnSaveChanges     = findViewById(R.id.btnSaveChanges);
        btnCreateEvent     = findViewById(R.id.btnCreateEvent);
        btnCancel          = findViewById(R.id.btnCancel);
        btnDeleteEvent     = findViewById(R.id.btnDeleteEvent);
        btnAttendeeManager = findViewById(R.id.btnAttendeeManager);
        btnAddImage        = findViewById(R.id.btnAddImage);
    }

    /** Wire listeners for date pickers, images, and actions. */
    private void setupClickListeners() {
        // Dates
        txtEventDate.setOnClickListener(v -> showDatePicker(eventDate, txtEventDate, "Event Date"));
        txtRegOpens.setOnClickListener(v -> showDatePicker(regOpensDate, txtRegOpens, "Registration Opens"));
        txtRegCloses.setOnClickListener(v -> showDatePicker(regClosesDate, txtRegCloses, "Registration Closes"));

        // Images
        if (btnAddImage != null) btnAddImage.setOnClickListener(v -> openImagePicker());

        // Actions
        if (btnSaveChanges != null)   btnSaveChanges.setOnClickListener(v -> saveEvent());
        if (btnCreateEvent != null)   btnCreateEvent.setOnClickListener(v -> createEvent());
        if (btnCancel != null)        btnCancel.setOnClickListener(v -> finish());
        if (btnDeleteEvent != null)   btnDeleteEvent.setOnClickListener(v -> deleteEvent());
        if (btnAttendeeManager != null) btnAttendeeManager.setOnClickListener(v -> openAttendeeManager());

        // Optional image sheet close / demo select (if you ever show the sheet)
        View btnCloseSheet = findViewById(R.id.btnCloseImageSheet);
        if (btnCloseSheet != null) btnCloseSheet.setOnClickListener(v -> sheetImageSelect.setVisibility(View.GONE));

        MaterialButton btnSelectImage = findViewById(R.id.btnSelectImagePoster);
        if (btnSelectImage != null) btnSelectImage.setOnClickListener(v -> {
            // Demo add — your real flow uses the gallery picker above
            addImageToContainer("Sample Image " + (imagePaths.size() + 1));
            sheetImageSelect.setVisibility(View.GONE);
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
        });
    }

    /** Launch gallery. */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /** Generic date picker helper. */
    private void showDatePicker(Calendar calendar, EditText targetView, String title) {
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    targetView.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.setTitle(title);
        picker.show();
    }

    /** Update mini gallery under the form (max 3 items for now). */
    private void updateImageDisplay() {
        imageContainer.removeAllViews();
        int max = Math.min(imagePaths.size(), 3);
        for (int i = 0; i < max; i++) {
            View imageItem = getLayoutInflater().inflate(R.layout.item_image, imageContainer, false);
            TextView lbl = imageItem.findViewById(R.id.txtImageLabel);
            MaterialButton btnDelete = imageItem.findViewById(R.id.btnDeleteImage);

            final int idx = i;
            lbl.setText("Image " + (i + 1));
            btnDelete.setOnClickListener(v -> {
                imagePaths.remove(idx);
                updateImageDisplay();
            });

            imageContainer.addView(imageItem);
        }
    }

    /** Append an image path/URI and refresh. */
    private void addImageToContainer(String imagePath) {
        imagePaths.add(imagePath);
        updateImageDisplay();
    }

    /** Load the event doc if editing; else fall back to extras for legacy flows. */
    private void loadEventData() {
        // Preferred: load from Firestore when we have an id
        if (eventId != null && !eventId.isEmpty()) {
            eventsRef.document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            Event event = doc.toObject(Event.class);
                            if (event == null) return;

                            if (event.getName() != null) edtEventName.setText(event.getName());
                            if (event.getMaxSpots() != null) edtSpots.setText(event.getMaxSpots());
                            if (event.getCost() != null) {
                                String cost = event.getCost().replace("$", "").trim();
                                if (!"Free".equalsIgnoreCase(cost) && !"—".equals(cost)) {
                                    edtCost.setText(cost);
                                }
                            }
                            if (event.getEventDate() != null)        txtEventDate.setText(event.getEventDate());
                            if (event.getRegistrationOpens() != null) txtRegOpens.setText(event.getRegistrationOpens());
                            if (event.getRegistrationCloses() != null)txtRegCloses.setText(event.getRegistrationCloses());
                            chkGeolocation.setChecked(event.isGeolocationEnabled());

                            if (event.getImagePaths() != null) {
                                imagePaths.clear();
                                imagePaths.addAll(event.getImagePaths());
                            }
                            updateImageDisplay();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
            return;
        }

        // Fallback: legacy extras (no Firestore fetch)
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String spots = intent.getStringExtra("spots");
        String cost  = intent.getStringExtra("cost");

        if (title != null) edtEventName.setText(title);
        if (spots != null) {
            String[] parts = spots.split("/");
            if (parts.length > 1) edtSpots.setText(parts[1]);
        }
        if (cost != null) {
            String c = cost.replace("$", "").trim();
            if (!"Free".equalsIgnoreCase(c) && !"—".equals(c)) edtCost.setText(c);
        }

        // Demo images when no Firestore record
        imagePaths.clear();
        imagePaths.add("Sample Image 1");
        imagePaths.add("Sample Image 2");
        imagePaths.add("Sample Image 3");
        updateImageDisplay();
    }

    /** Create a brand new event. */
    private void createEvent() {
        if (!validateForm()) return;
        storeInDB();
    }

    /** Persist a new event to Firestore. */
    private void storeInDB() {
        String name         = safeText(edtEventName);
        String spots        = safeText(edtSpots);
        String cost         = safeText(edtCost);
        String eventDateStr = safeText(txtEventDate);
        String regOpensStr  = safeText(txtRegOpens);
        String regClosesStr = safeText(txtRegCloses);
        boolean geolocation = chkGeolocation.isChecked();

        String newEventId = eventsRef.document().getId();
        Event newEvent = new Event(
                newEventId,
                name,
                eventDateStr,
                regOpensStr,
                regClosesStr,
                spots,
                cost,
                geolocation,
                new ArrayList<>(imagePaths) // copy
        );

        // Attach organizer id if available
        try {
            com.google.firebase.auth.FirebaseUser fu = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (fu != null) newEvent.setOrganizerId(fu.getUid());
        } catch (Exception ignored) {}

        eventsRef.document(newEventId)
                .set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Intent result = new Intent();
                    result.putExtra("eventId", newEventId);
                    setResult(RESULT_OK, result);
                    Toast.makeText(this, "Event \"" + name + "\" created successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: Failed to create event. " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error writing document", e);
                });
    }

    /** Save edits back to Firestore (requires eventId). */
    private void saveEvent() {
        if (!validateForm()) return;

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event id provided; cannot save edits.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name         = safeText(edtEventName);
        String spots        = safeText(edtSpots);
        String cost         = safeText(edtCost);
        String eventDateStr = safeText(txtEventDate);
        String regOpensStr  = safeText(txtRegOpens);
        String regClosesStr = safeText(txtRegCloses);
        boolean geolocation = chkGeolocation.isChecked();

        Event updated = new Event(
                eventId,
                name,
                eventDateStr,
                regOpensStr,
                regClosesStr,
                spots,
                cost,
                geolocation,
                new ArrayList<>(imagePaths)
        );

        try {
            com.google.firebase.auth.FirebaseUser fu = com.google.firebase.auth.FirebaseAuth.getCurrentUser();
            if (fu != null) updated.setOrganizerId(fu.getUid());
        } catch (Exception ignored) {}

        eventsRef.document(eventId)
                .set(updated)
                .addOnSuccessListener(aVoid -> {
                    Intent result = new Intent();
                    result.putExtra("eventId", eventId);
                    setResult(RESULT_OK, result);
                    Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /** Delete event (with minimal confirmation UX; refine in production). */
    private void deleteEvent() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "No event id provided; nothing to delete.", Toast.LENGTH_SHORT).show();
            return;
        }
        eventsRef.document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    Intent result = new Intent();
                    result.putExtra("eventId", eventId);
                    result.putExtra("deleted", true);
                    setResult(RESULT_OK, result);
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /** Open attendee manager for this event (passes eventId if known). */
    private void openAttendeeManager() {
        Intent intent = new Intent(this, AttendeeManagerActivity.class);
        if (eventId != null && !eventId.isEmpty()) {
            intent.putExtra("eventId", eventId);
        } else {
            intent.putExtra("eventTitle", safeText(edtEventName));
        }
        startActivity(intent);
    }

    /** Minimal validation; keep UX consistent with your other screens. */
    private boolean validateForm() {
        String eventName = safeText(edtEventName);
        if (eventName.isEmpty()) {
            edtEventName.setError("Please enter an event name");
            edtEventName.requestFocus();
            return false;
        }
        // Optional: light sanity for spots/cost (silent if empty)
        String spots = safeText(edtSpots);
        if (!spots.isEmpty()) {
            try {
                int s = Integer.parseInt(spots);
                if (s < 0) {
                    edtSpots.setError("Enter a non-negative number");
                    edtSpots.requestFocus();
                    return false;
                }
            } catch (NumberFormatException nfe) {
                edtSpots.setError("Enter a valid number");
                edtSpots.requestFocus();
                return false;
            }
        }
        String cost = safeText(edtCost);
        if (!cost.isEmpty()) {
            try {
                double c = Double.parseDouble(cost);
                if (c < 0) {
                    edtCost.setError("Enter a non-negative amount");
                    edtCost.requestFocus();
                    return false;
                }
            } catch (NumberFormatException nfe) {
                edtCost.setError("Enter a valid amount (e.g., 15 or 15.00)");
                edtCost.requestFocus();
                return false;
            }
        }
        return true;
    }

    private static String safeText(@Nullable EditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    /**
     * @brief Handles system back presses, closing the image sheet first if visible.
     *
     * If the image selection sheet is open, it is dismissed; otherwise, the
     * default back-press behavior is delegated to the superclass.
     */
    @Override
    public void onBackPressed() {
        if (sheetImageSelect != null && sheetImageSelect.getVisibility() == View.VISIBLE) {
            sheetImageSelect.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
