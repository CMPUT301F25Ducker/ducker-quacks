/**
 * Handles creation and editing of events by organizers.
 *
 * This activity lets organizers create new events or edit existing ones. It
 * provides form fields for event details such as name, cost, registration
 * dates, geolocation, and associated images. It also performs Firestore CRUD
 * operations for storing and updating event data.
 *
 * UI includes: Event name, spots, cost, event date, registration
 * open/close dates, image management, geolocation toggle, and action buttons
 * (save, cancel, delete).
 *
 * @author DuckDuckGoose Development Team
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity for organizers to create or edit events stored in Firestore.
 *
 * Features include creating new events, editing existing events, attaching
 * event images using a gallery picker, managing registration and geolocation
 * settings, and launching related screens such as the attendee manager.
 */
public class EventEditActivity extends AppCompatActivity {

    /** Request code for image selection from the gallery. */
    private static final int PICK_IMAGE_REQUEST = 1;

    /** Mode of operation ("create" or "edit"). */
    private String mode;

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /** Reference to the Firestore events collection. */
    private CollectionReference eventsRef;

    /** Firestore document ID for the event being edited. */
    private String eventId;

    /** Text input field for event name. */
    private EditText edtEventName;

    /** Text input field for event description. */
    private EditText edtDescription;

    /** Text input field for maximum number of spots. */
    private EditText edtSpots;

    /** Text input field for event cost. */
    private EditText edtCost;

    /** Text field for displaying/selecting the event date. */
    private EditText txtEventDate;

    /** Text field for displaying/selecting when registration opens. */
    private EditText txtRegOpens;

    /** Text field for displaying/selecting when registration closes. */
    private EditText txtRegCloses;

    /** Checkbox to toggle geolocation requirement for the event. */
    private CheckBox chkGeolocation;

    /** Container layout for displaying selected images. */
    private LinearLayout imageContainer;

    /** List of image URIs or file paths associated with the event. */
    private List<String> imagePaths = new ArrayList<>();

    /** Bottom sheet card view for selecting images. */
    private CardView sheetImageSelect;

    /** Activity result launcher for the image picker. */
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    /** Button to save changes to an existing event. */
    private MaterialButton btnSaveChanges;

    /** Button to create a new event. */
    private MaterialButton btnCreateEvent;

    /** Button to cancel editing and close the activity. */
    private MaterialButton btnCancel;

    /** Button to delete the current event. */
    private MaterialButton btnDeleteEvent;

    /** Button to open the attendee manager. */
    private MaterialButton btnAttendeeManager;

    /** Button to add a new image to the event. */
    private MaterialButton btnAddImage;

    /** Calendar instance for the event date. */
    private Calendar eventDate = Calendar.getInstance();

    /** Calendar instance for when registration opens. */
    private Calendar regOpensDate = Calendar.getInstance();

    /** Calendar instance for when registration closes. */
    private Calendar regClosesDate = Calendar.getInstance();

    /** Date formatter for displaying dates in MM/dd/yy format. */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy", Locale.US);

    /** Firebase Storage reference for uploading images. */
    private StorageReference storageRef;

    /**
     * Initializes the activity and sets up UI components.
     *
     * @param savedInstanceState - Saved activity state, if any
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

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize image picker launcher for selecting images from the gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageToStorage(imageUri);
                        }
                    }
                }
        );

        // Determine if we are creating or editing an event
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "create";

        initializeViews();
        setupClickListeners();

        // Update top bar title and toggle visible buttons depending on mode
        TextView txtTopBarTitle = findViewById(R.id.txtTopBarTitle);

        if (mode.equals("edit")) {
            if (txtTopBarTitle != null) txtTopBarTitle.setText("Edit Event");
            String incomingId = getIntent().getStringExtra("eventId");
            if (incomingId != null) eventId = incomingId;
            loadEventData();
            btnCreateEvent.setVisibility(View.GONE);
            btnSaveChanges.setVisibility(View.VISIBLE);
            btnDeleteEvent.setVisibility(View.GONE);
            btnAttendeeManager.setVisibility(View.GONE);
        } else {
            if (txtTopBarTitle != null) txtTopBarTitle.setText("New Event");
            btnSaveChanges.setVisibility(View.GONE);
            btnCreateEvent.setVisibility(View.VISIBLE);
            btnDeleteEvent.setVisibility(View.GONE);
            btnAttendeeManager.setVisibility(View.GONE);
        }

        // Back button closes the activity
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    /**
     * Initializes all form fields and button references.
     */
    private void initializeViews() {
        edtEventName = findViewById(R.id.edtEventName);
        edtDescription = findViewById(R.id.edtDescription);
        edtSpots = findViewById(R.id.edtSpots);
        edtCost = findViewById(R.id.edtCost);
        txtEventDate = findViewById(R.id.txtEventDate);
        txtRegOpens = findViewById(R.id.txtRegOpens);
        txtRegCloses = findViewById(R.id.txtRegCloses);
        chkGeolocation = findViewById(R.id.chkGeolocation);

        imageContainer = findViewById(R.id.imageContainer);
        sheetImageSelect = findViewById(R.id.sheetImageSelect);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnCancel = findViewById(R.id.btnCancel);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        btnAttendeeManager = findViewById(R.id.btnAttendeeManager);
        btnAddImage = findViewById(R.id.btnAddImage);
    }

    /**
     * Wires up button and field click listeners for UI actions.
     */
    private void setupClickListeners() {
        txtEventDate.setOnClickListener(v -> showDatePicker(eventDate, txtEventDate, "Event Date"));
        txtRegOpens.setOnClickListener(v -> showDatePicker(regOpensDate, txtRegOpens, "Registration Opens"));
        txtRegCloses.setOnClickListener(v -> showDatePicker(regClosesDate, txtRegCloses, "Registration Closes"));

        btnAddImage.setOnClickListener(v -> openImagePicker());
        btnSaveChanges.setOnClickListener(v -> saveEvent());
        btnCreateEvent.setOnClickListener(v -> createEvent());
        btnCancel.setOnClickListener(v -> finish());
        btnDeleteEvent.setOnClickListener(v -> deleteEvent());
        btnAttendeeManager.setOnClickListener(v -> openAttendeeManager());
    }

    /**
     * Opens an image picker for selecting images from the gallery.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * Displays a DatePickerDialog for selecting event or registration dates.
     *
     * @param calendar - The calendar instance to update
     * @param targetView - The target text field to display the selected date
     * @param title - Dialog title indicating which date is being selected
     */
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

    /**
     * Displays the bottom sheet for selecting images (demo mode).
     */
    private void showImageSelectSheet() {
        if (sheetImageSelect != null) {
            sheetImageSelect.setVisibility(View.VISIBLE);

            View btnCloseSheet = findViewById(R.id.btnCloseImageSheet);
            if (btnCloseSheet != null) {
                btnCloseSheet.setOnClickListener(v -> sheetImageSelect.setVisibility(View.GONE));
            }

            MaterialButton btnSelectImage = findViewById(R.id.btnSelectImagePoster);
            if (btnSelectImage != null) {
                btnSelectImage.setOnClickListener(v -> {
                    addImageToContainer("Sample Image " + (imagePaths.size() + 1));
                    sheetImageSelect.setVisibility(View.GONE);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    /**
     * Adds an image path to the event's image container and updates the display.
     *
     * @param imagePath - The URI or file path of the image to add
     */
    private void addImageToContainer(String imagePath) {
        imagePaths.add(imagePath);
        updateImageDisplay();
    }

    /**
     * Uploads an image to Firebase Storage and adds the download URL to the event.
     *
     * @param fileUri - The URI of the image file to upload
     */
    private void uploadImageToStorage(Uri fileUri) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        String filename = "events/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = storageRef.child(filename);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        imagePaths.add(downloadUrl);
                        updateImageDisplay();
                        Toast.makeText(EventEditActivity.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventEditActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the visible list of selected images in the layout.
     */
    private void updateImageDisplay() {
        imageContainer.removeAllViews();

        for (int i = 0; i < imagePaths.size(); i++) {
            View imageItem = getLayoutInflater().inflate(R.layout.item_image, imageContainer, false);

            ImageView imgPreview = imageItem.findViewById(R.id.imgPreview);
            TextView txtImageLabel = imageItem.findViewById(R.id.txtImageLabel);
            MaterialButton btnDeleteImage = imageItem.findViewById(R.id.btnDeleteImage);

            String imageUrl = imagePaths.get(i);

            txtImageLabel.setText("Image " + (i + 1));

            // USE GLIDE TO LOAD IMAGE
            if (imgPreview != null) {
                Glide.with(this)
                        .load(imageUrl)
                        .centerCrop()
                        .into(imgPreview);
            }

            final int index = i;
            btnDeleteImage.setOnClickListener(v -> {
                imagePaths.remove(index);
                //  storageRef.child(...).delete()
                updateImageDisplay();
            });

            imageContainer.addView(imageItem);
        }
    }

    /**
     * Loads existing event data from Firestore or intent extras for editing.
     */
    private void loadEventData() {
        Intent intent = getIntent();

        if (eventId != null) {
            eventsRef.document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                if (event.getName() != null)
                                    edtEventName.setText(event.getName());
                                if (event.getDescription() != null)
                                    edtDescription.setText(event.getDescription());
                                if (event.getMaxSpots() != null)
                                    edtSpots.setText(event.getMaxSpots());
                                if (event.getCost() != null) {
                                    String cost = event.getCost().replace("$", "").trim();
                                    if (!cost.equals("Free") && !cost.equals("—")) edtCost.setText(cost);
                                }
                                if (event.getEventDate() != null) txtEventDate.setText(event.getEventDate());
                                if (event.getRegistrationOpens() != null) txtRegOpens.setText(event.getRegistrationOpens());
                                if (event.getRegistrationCloses() != null) txtRegCloses.setText(event.getRegistrationCloses());
                                chkGeolocation.setChecked(event.isGeolocationEnabled());

                                if (event.getImagePaths() != null) {
                                    imagePaths.clear();
                                    imagePaths.addAll(event.getImagePaths());
                                }
                                updateImageDisplay();
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_LONG).show());
            return;
        }

        // Legacy fallback if no Firestore ID was provided
        String title = intent.getStringExtra("title");
        String spots = intent.getStringExtra("spots");
        String cost = intent.getStringExtra("cost");

        if (title != null) edtEventName.setText(title);
        if (spots != null) {
            String[] parts = spots.split("/");
            if (parts.length > 1) edtSpots.setText(parts[1]);
        }
        if (cost != null) {
            cost = cost.replace("$", "").trim();
            if (!cost.equals("Free") && !cost.equals("—")) edtCost.setText(cost);
        }

        imagePaths.add("Sample Image 1");
        imagePaths.add("Sample Image 2");
        imagePaths.add("Sample Image 3");
        updateImageDisplay();
    }

    /**
     * Creates a new event after validating the form.
     */
    private void createEvent() {
        if (validateForm()) {
            storeInDB();
        }
    }

    /**
     * Saves a new event to Firestore and notifies the caller.
     */
    private void storeInDB() {
        String name = edtEventName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String spots = edtSpots.getText().toString().trim();
        String cost = edtCost.getText().toString().trim();
        String eventDateStr = txtEventDate.getText().toString().trim();
        String regOpensStr = txtRegOpens.getText().toString().trim();
        String regClosesStr = txtRegCloses.getText().toString().trim();
        boolean geolocation = chkGeolocation.isChecked();

        // Generate new event ID
        String newEventId = eventsRef.document().getId();

        // Build Event object
        Event newEvent = new Event(
                newEventId, name, description, eventDateStr, regOpensStr, regClosesStr, spots, cost, geolocation, imagePaths
        );

        // Attach organizerId to event if user is logged in
        FirebaseUser fu = null;
        try {
            fu = FirebaseAuth.getInstance().getCurrentUser();
            if (fu != null) {
                newEvent.setOrganizerId(fu.getUid());
            }
        } catch (Exception ex) {
            // swallow – we'll still create the event even if organizerId is missing
        }

        // Use a batch so event creation + ownedEvents update happen together
        WriteBatch batch = db.batch();

        DocumentReference eventRef = eventsRef.document(newEventId);
        batch.set(eventRef, newEvent);

        // If we know who the organizer is, add this eventId under their ownedEvents
        if (fu != null) {
            DocumentReference userRef = db.collection("users").document(fu.getUid());
            batch.update(userRef, "ownedEvents", FieldValue.arrayUnion(newEventId));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Intent result = new Intent();
                    result.putExtra("eventId", newEventId);
                    setResult(RESULT_OK, result);
                    Toast.makeText(this,
                            "Event \"" + name + "\" created successfully!",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error: Failed to create event. " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("EventEditActivity", "Error writing document", e);
                });
    }

    /**
     * Updates an existing Firestore event, if eventId is available.
     */
    private void saveEvent() {
        if (validateForm()) {
            if (eventId != null) {
                String name = edtEventName.getText().toString().trim();
                String description = edtDescription.getText().toString().trim();
                String spots = edtSpots.getText().toString().trim();
                String cost = edtCost.getText().toString().trim();
                String eventDateStr = txtEventDate.getText().toString().trim();
                String regOpensStr = txtRegOpens.getText().toString().trim();
                String regClosesStr = txtRegCloses.getText().toString().trim();
                boolean geolocation = chkGeolocation.isChecked();

                Event updated = new Event(
                        eventId, name, description, eventDateStr, regOpensStr, regClosesStr, spots, cost, geolocation, imagePaths
                );

                try {
                    com.google.firebase.auth.FirebaseUser fu =
                            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (fu != null) updated.setOrganizerId(fu.getUid());
                } catch (Exception ex) { }

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
                                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                Toast.makeText(this, "No event id provided; cannot save edits.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Deletes the current event document from Firestore.
     */
    private void deleteEvent() {
        if (eventId != null) {
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
                            Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Opens the attendee management screen for this event.
     */
    private void openAttendeeManager() {
        Intent intent = new Intent(this, AttendeeManagerActivity.class);
        if (eventId != null) intent.putExtra("eventId", eventId);
        else intent.putExtra("eventTitle", edtEventName.getText().toString());
        startActivity(intent);
    }

    /**
     * Validates user input to ensure required fields are filled.
     *
     * @return true if valid; false otherwise
     */
    private boolean validateForm() {
        // instantiations for the fields themselves
        String eventName = edtEventName.getText().toString().trim();
        String spots = edtSpots.getText().toString().trim();
        String cost = edtCost.getText().toString().trim();
        String eventDateStr = txtEventDate.getText().toString().trim();
        String regOpensStr = txtRegOpens.getText().toString().trim();
        String regClosesStr = txtRegCloses.getText().toString().trim();

        // verify that the Event Name field is not empty
        if (eventName.isEmpty()) {
            Toast.makeText(this, "Please enter an event name", Toast.LENGTH_SHORT).show();
            edtEventName.requestFocus();
            return false;
        }

        // verify that the Spots field is not empty
        if (spots.isEmpty()) {
            Toast.makeText(this, "Please enter the maximum number of spots", Toast.LENGTH_SHORT).show();
            edtSpots.requestFocus();
            return false;
        } else if (Integer.parseInt(spots) < 1) {
            Toast.makeText(this, "Maximum # of spots must be at least 1", Toast.LENGTH_SHORT).show();
            edtSpots.requestFocus();
            return false;
        }

        // verify that the Cost field is not empty
        if (cost.isEmpty()) {
            Toast.makeText(this, "Please enter the event cost (or enter 0 for free)", Toast.LENGTH_SHORT).show();
            edtCost.requestFocus();
            return false;
        } else if (Double.parseDouble(cost) < 0) { // casted to avoid CasteException
            Toast.makeText(this, "Cost cannot be negative", Toast.LENGTH_SHORT).show();
            edtCost.requestFocus();
            return false;
        }


        // ====== using compareTo() so here's the quick debug logic ======
        // returns negative integer if the first date is before the second
        // returns 0 if dates are equal
        // returns positive integer if the first date is after the second

        // verify that registration opens before registration closes
        if (regOpensDate.compareTo(regClosesDate) >= 0) { // >= means "greater than or equal to" (not before)
            Toast.makeText(this, "Registration opens must be before registration closes", Toast.LENGTH_SHORT).show();
            txtRegOpens.requestFocus();
            return false;
        }

        // verify that registration closes before or on the event date
        if (regClosesDate.compareTo(eventDate) > 0) { // means "greater than" (after)
            Toast.makeText(this, "Registration deadline must be before or on the event date", Toast.LENGTH_SHORT).show();
            txtRegCloses.requestFocus();
            return false;
        }

        // verify that registration opens before the event date
        if (regOpensDate.compareTo(eventDate) >= 0) { // means "less than" (before)
            Toast.makeText(this, "Registration opens must be before the event date", Toast.LENGTH_SHORT).show();
            txtRegOpens.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Handles back navigation and hides the image sheet if it is open.
     */
    @Override
    public void onBackPressed() { // only Allah knows what this is really doing
        if (sheetImageSelect != null && sheetImageSelect.getVisibility() == View.VISIBLE) {
            sheetImageSelect.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}