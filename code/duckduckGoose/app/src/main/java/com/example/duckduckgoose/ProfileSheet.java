/**
 * Bottom sheet for viewing and managing a user's profile in DuckDuckGoose.
 *
 * This fragment renders profile details (name, age, contact info, account type),
 * supports viewing another user's profile (with admin actions like kick/delete),
 * and provides self-service actions for the signed-in user such as editing the
 * profile, logging out, or deleting the account. It can optionally display event
 * metadata and an "Events" button that delegates to a listener.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.duckduckgoose.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;

/**
 * A Material BottomSheet that shows a user's profile and actions.
 *
 * Usage modes:
 *  - **Self profile**: shows edit, logout, and delete account actions.
 *  - **Viewing other user**: shows delete/kick and optional Events button.
 * Optional arguments enable attendee/event info sections.
 */
public class ProfileSheet extends BottomSheetDialogFragment {

    /**
     * Listener interface to notify host activities/fragments about
     * destructive/profile-related actions initiated from the sheet.
     */
    public interface OnProfileInteractionListener {
        /**
         * Called when the admin deletes/kicks an entrant from the sheet.
         * @param userId The unique ID of the user being removed.
         */
        void onProfileDeleted(String userId);

        /**
         * Called when the Events button is tapped for a user.
         * @param userId The unique ID of the user whose events are requested.
         */
        void onEventsButtonClicked(String userId);
    }

    /**
     * Attach single/double tap handlers to the notification bell.
     * Single tap: open notification logs. Double tap: toggle `receive_notifications` in Firestore.
     */
    private void attachNotificationHandler(ImageButton btnNotification, boolean hasNewNotifications) {
        if (btnNotification == null) return;

        // initial icon state
        if (hasNewNotifications) {
            btnNotification.setImageResource(R.drawable.notifications_unread_24px);
        } else {
            btnNotification.setImageResource(R.drawable.notifications_24px);
        }

        // Set visual alpha based on current Firestore preference (if available)
        FirebaseUser curUser = FirebaseAuth.getInstance().getCurrentUser();
        if (curUser != null && db != null) {
            db.collection("users").document(curUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Boolean opted = doc.getBoolean("receive_notifications");
                        if (opted != null && !opted) {
                            btnNotification.setAlpha(0.4f);
                        } else {
                            btnNotification.setAlpha(1.0f);
                        }
                    }
                });
        }

        GestureDetector detector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // single tap: open notification logs
                if (getActivity() != null) {
                    dismiss();
                    Intent intent = new Intent(getActivity(), NotificationLogsActivity.class);
                    startActivity(intent);
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // double tap: toggle the receive_notifications flag in Firestore
                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                if (u == null) {
                    Toast.makeText(getContext(), "Please sign in to change notification settings.", Toast.LENGTH_SHORT).show();
                    return true;
                }

                String uid = u.getUid();
                if (db == null) db = FirebaseFirestore.getInstance();

                db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        boolean current = true; // default
                        if (doc != null && doc.exists()) {
                            Boolean b = doc.getBoolean("receive_notifications");
                            if (b != null) current = b;
                        }
                        boolean next = !current;
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("receive_notifications", next);
                        if (!next) {
                            updates.put("opt_out_updated_at", com.google.firebase.Timestamp.now());
                        } else {
                            updates.put("opt_out_updated_at", FieldValue.delete());
                        }

                        db.collection("users").document(uid).update(updates)
                                .addOnSuccessListener(v -> {
                                    btnNotification.setAlpha(next ? 1.0f : 0.4f);
                                    Toast.makeText(getContext(), next ? "Notifications unmuted" : "Notifications muted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(err -> Toast.makeText(getContext(), "Failed to update preference: " + err.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(err -> Toast.makeText(getContext(), "Failed to read preference: " + err.getMessage(), Toast.LENGTH_LONG).show());

                return true;
            }
        });

        btnNotification.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });
    }

    /** Callback target implemented by the hosting context. */
    private OnProfileInteractionListener mListener;

    /** Firestore instance for profile document reads/deletes. */
    private FirebaseFirestore db;

    /** FirebaseAuth instance used for logout and account deletion. */
    private FirebaseAuth auth;

    /**
     * Factory method for an empty sheet that loads the current user.
     *
     * @return New instance of ProfileSheet.
     */
    public static ProfileSheet newInstance() {
        return new ProfileSheet();
    }

    /**
     * Factory for a sheet bound to a specific user (simple mode).
     *
     * @param user The user whose profile should be displayed.
     * @return New instance of ProfileSheet preloaded with user arguments.
     */
    public static ProfileSheet newInstance(User user) {
        return newInstance(user, false, false, null, false);
    }

    /**
     * Full-featured factory with display and control flags.
     *
     * @param user The user to display.
     * @param isViewingOther True if another user's profile is being viewed.
     * @param showEventsButton True to show the Events action button.
     * @param eventCount Optional text to display as event count/status.
     * @param showAttendeeInfo True to reveal attendee-specific info sections.
     * @return New instance of ProfileSheet with bundled arguments.
     */
    public static ProfileSheet newInstance(User user, boolean isViewingOther, boolean showEventsButton, String eventCount, boolean showAttendeeInfo) {
        ProfileSheet fragment = new ProfileSheet();
        Bundle args = new Bundle();
        args.putString("name", user.getFullName());
        args.putString("userId", user.getUserId());
        args.putLong("age", user.getAge());
        args.putString("email", user.getEmail());
        args.putString("phone", user.getPhone());
        args.putString("accountType", user.getAccountType());
        args.putBoolean("isViewingOther", isViewingOther);
        args.putBoolean("showEventsButton", showEventsButton);
        args.putString("eventCount", eventCount);
        args.putBoolean("showAttendeeInfo", showAttendeeInfo);
        args.putBoolean("new_notifications", user.getNew_notifications());
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Captures the host callback if it implements the listener.
     *
     * @param context Hosting context.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnProfileInteractionListener) {
            mListener = (OnProfileInteractionListener) context;
        }
    }

    /**
     * Inflates the sheet layout.
     *
     * @param inflater Layout inflater.
     * @param container Optional parent view.
     * @param savedInstanceState Previous state or null.
     * @return The inflated sheet view.
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_profile, container, false);
    }

    /**
     * Binds UI and wires actions after the view is created.
     * Handles both argument-driven profiles and the current user's profile.
     *
     * @param v Root view.
     * @param s Saved instance state.
     */
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v,s);
        TextView txtUserId = v.findViewById(R.id.txtUserId);
        TextView txtFullName = v.findViewById(R.id.txtFullName);
        TextView txtAge = v.findViewById(R.id.txtAge);
        TextView txtEmail = v.findViewById(R.id.txtEmail);
        TextView txtPhone = v.findViewById(R.id.txtPhone);
        TextView txtAccountType = v.findViewById(R.id.txtAccountType);
        TextView txtEventCount = v.findViewById(R.id.txtEventCount);
        TextView txtPastEvents = v.findViewById(R.id.txtPastEvents);
        TextView txtPooledEvents = v.findViewById(R.id.txtPooledEvents);

        MaterialButton btnEdit = v.findViewById(R.id.btnEdit);
        MaterialButton btnLogout = v.findViewById(R.id.btnLogout);
        MaterialButton btnDelete = v.findViewById(R.id.btnDelete);
        MaterialButton btnEvents = v.findViewById(R.id.btnEvents);
        ImageButton btnNotification = v.findViewById(R.id.btnNotification);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Bundle arguments = getArguments();
        if (arguments != null) {
            // Populate from provided args (viewing some user's profile).
            String userId = arguments.getString("userId", "N/A");
            txtUserId.setText("User ID: " + userId);
            txtFullName.setText("Full Name: " + arguments.getString("name", "N/A"));

            if (arguments.containsKey("age")) {
                txtAge.setText("Age: " + arguments.getLong("age"));
            } else {
                txtAge.setVisibility(View.GONE);
            }

            String email = arguments.getString("email");
            if (email != null && !email.isEmpty()) {
                txtEmail.setText("Email: " + email);
            } else {
                txtEmail.setVisibility(View.GONE);
            }

            String phone = arguments.getString("phone");
            if (phone != null && !phone.isEmpty()) {
                txtPhone.setText("Phone Number: " + phone);
            } else {
                txtPhone.setVisibility(View.GONE);
            }

            String accountType = arguments.getString("accountType");
            if (accountType != null && !accountType.isEmpty()) {
                txtAccountType.setText("Account Type: " + accountType);
            } else {
                txtAccountType.setVisibility(View.GONE);
            }

            if (arguments.getBoolean("isViewingOther")) {
                // Admin view of another user
                btnEdit.setVisibility(View.GONE);
                btnLogout.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
                // Hide notification icon when viewing another user
                if (btnNotification != null) {
                    btnNotification.setVisibility(View.GONE);
                }

                if (arguments.getBoolean("showAttendeeInfo")) {
                    btnDelete.setText("Kick");
                }

                btnDelete.setOnClickListener(x -> {
                    if (mListener != null) {
                        mListener.onProfileDeleted(email);
                    }
                    dismiss();
                });
            } else {
                // Self view with delete account
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setText("Delete Account");
                btnDelete.setOnClickListener(x -> confirmDeleteSelf());

                // Show notification icon for entrants only
                if (btnNotification != null) {
                    // Only show notification button for entrants
                    if (accountType != null && accountType.equalsIgnoreCase("Entrant")) {
                        btnNotification.setVisibility(View.VISIBLE);
                        boolean hasNewNotifications = arguments.getBoolean("new_notifications", false);
                        // attach a handler that supports single-tap (open logs) and double-tap (mute/unmute)
                        attachNotificationHandler(btnNotification, hasNewNotifications);
                    } else {
                        // Hide notification button for non-entrants
                        btnNotification.setVisibility(View.GONE);
                    }
                }
            }


            if (arguments.getBoolean("showEventsButton")) {
                btnEvents.setVisibility(View.VISIBLE);
                btnEvents.setOnClickListener(x -> {
                    if (mListener != null) {
                        mListener.onEventsButtonClicked(email);
                    }
                    dismiss();
                });
                String eventCount = arguments.getString("eventCount");
                if (eventCount != null) {
                    txtEventCount.setText("Events: " + eventCount);
                    txtEventCount.setVisibility(View.VISIBLE);
                } else {
                    txtEventCount.setVisibility(View.GONE);
                }
            } else {
                btnEvents.setVisibility(View.GONE);
                txtEventCount.setVisibility(View.GONE);
            }

            if (arguments.getBoolean("showAttendeeInfo")) {
                String status = arguments.getString("eventCount");
                if (status != null) {
                    txtEventCount.setText("Status: " + status);
                    txtEventCount.setVisibility(View.VISIBLE);
                }
                txtPastEvents.setVisibility(View.VISIBLE);
                txtPooledEvents.setVisibility(View.VISIBLE);
            } else {
                txtPastEvents.setVisibility(View.GONE);
                txtPooledEvents.setVisibility(View.GONE);
            }
        } else {
            // No args: fetch and display the current user's profile.
            if (auth.getCurrentUser() != null) {
                db.collection("users").document(auth.getCurrentUser().getUid()).get()
                        .addOnSuccessListener(ds -> {
                            User me = ds.toObject(User.class);
                            if (me != null) {
                                bindSelfProfile(v, me);
                                // Update notification icon based on new_notifications field
                                if (btnNotification != null) {
                                    // Only show notification button for entrants
                                    String accountType = me.getAccountType();
                                    if (accountType != null && accountType.equalsIgnoreCase("Entrant")) {
                                        btnNotification.setVisibility(View.VISIBLE);
                                        // set up single/double tap behavior on the bell
                                        attachNotificationHandler(btnNotification, me.getNew_notifications());
                                    } else {
                                        // Hide notification button for non-entrants
                                        btnNotification.setVisibility(View.GONE);
                                    }
                                }
                            } else {
                                android.util.Log.w("Firestore", "User doc exists? " + ds.exists() + " but toObject() returned null");
                                Toast.makeText(getContext(), "Could not load profile.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("Firestore", "error fetching user", e);
                            Toast.makeText(getContext(), "Error fetching profile.", Toast.LENGTH_SHORT).show();
                        });
            }
            if (btnDelete != null) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setText("Delete Account");
                btnDelete.setOnClickListener(x -> confirmDeleteSelf());
            }
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(x -> {
                EditProfileSheet.newInstance().show(getParentFragmentManager(), "EditProfileSheet");
                dismiss();
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(x -> {
                dismiss();
                auth.signOut();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("startOn", "LOGIN");
                    startActivity(intent);
                    getActivity().finish();
                }
            });
        }

        ImageButton btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(x -> dismiss());
        }
    }

    /**
     * Binds current user's profile fields to the UI.
     *
     * @param v Root view for lookups.
     * @param me The current signed-in user object.
     */
    private void bindSelfProfile(View v, User me) {
        TextView txtUserId = v.findViewById(R.id.txtUserId);
        TextView txtFullName = v.findViewById(R.id.txtFullName);
        TextView txtAge = v.findViewById(R.id.txtAge);
        TextView txtEmail = v.findViewById(R.id.txtEmail);
        TextView txtPhone = v.findViewById(R.id.txtPhone);
        TextView txtAccountType = v.findViewById(R.id.txtAccountType);

        txtUserId.setText("User ID: " + me.getUserId());
        txtFullName.setText("Full Name: " + me.getFullName());

        if (me.getAge() != null) {
            txtAge.setText("Age: " + me.getAge());
        } else {
            txtAge.setVisibility(View.GONE);
        }

        if (me.getEmail() != null && !me.getEmail().isEmpty()) {
            txtEmail.setText("Email: " + me.getEmail());
        } else {
            txtEmail.setVisibility(View.GONE);
        }

        if (me.getPhone() != null && !me.getPhone().isEmpty()) {
            txtPhone.setText("Phone Number: " + me.getPhone());
        } else {
            txtPhone.setVisibility(View.GONE);
        }

        if (me.getAccountType() != null && !me.getAccountType().isEmpty()) {
            txtAccountType.setText("Account Type: " + me.getAccountType());
        } else {
            txtAccountType.setVisibility(View.GONE);
        }
    }

    /**
     * Shows a confirmation dialog before deleting the current account.
     * Presents a destructive action with clear, irreversible consequence.
     */
    private void confirmDeleteSelf() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete account?")
                .setMessage("This will permanently remove your profile and sign you out. This cannot be undone.")
                .setPositiveButton("DELETE", (d, w) -> deleteSelf())
                .setNegativeButton("Cancel", null)
                .show();
    }



    /**
     *  Deletes the current user's Firestore document and Auth account.
     *
     * Order of operations:
     * 1) Delete Firestore /users/{uid} document.
     * 2) Delete FirebaseAuth user.
     * 3) Sign out and navigate to LoginActivity.
     *
     * Handles the recent-login requirement by notifying the user.
     */
    private void deleteSelf() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not signed in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).delete()
                .addOnSuccessListener(v -> auth.getCurrentUser().delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Account deleted :(", Toast.LENGTH_SHORT).show();
                            dismiss();
                            auth.signOut();
                            if (getActivity() != null) {
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.putExtra("startOn", "LOGIN");
                                startActivity(intent);
                                getActivity().finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                                Toast.makeText(requireContext(), "Please re-login to delete your account.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(), "Failed to delete auth account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            android.util.Log.e("Auth", "delete user failed", e);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to delete profile data.", Toast.LENGTH_LONG).show();
                    android.util.Log.e("Firestore", "delete user firestore failed", e);
                });
    }
}
