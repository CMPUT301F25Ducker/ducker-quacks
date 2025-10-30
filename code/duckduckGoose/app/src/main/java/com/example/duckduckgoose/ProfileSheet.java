package com.example.duckduckgoose;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class ProfileSheet extends BottomSheetDialogFragment {

    public interface OnProfileInteractionListener {
        void onProfileDeleted(String userId);
        void onEventsButtonClicked(String userId);
    }

    private OnProfileInteractionListener mListener;

    public static ProfileSheet newInstance() {
        return new ProfileSheet();
    }

    public static ProfileSheet newInstance(String name, String userId) {
        return newInstance(name, userId, false, false, null, false);
    }

    public static ProfileSheet newInstance(String name, String userId, boolean isViewingOther, boolean showEventsButton, String eventCount, boolean showAttendeeInfo) {
        ProfileSheet fragment = new ProfileSheet();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("userId", userId);
        args.putBoolean("isViewingOther", isViewingOther);
        args.putBoolean("showEventsButton", showEventsButton);
        args.putString("eventCount", eventCount);
        args.putBoolean("showAttendeeInfo", showAttendeeInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnProfileInteractionListener) {
            mListener = (OnProfileInteractionListener) context;
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_profile, container, false);
    }

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

        Bundle arguments = getArguments();
        if (arguments != null) {
            String userId = arguments.getString("userId");
            txtUserId.setText("User ID: " + userId);
            txtFullName.setText("Full Name: " + arguments.getString("name"));
            txtAge.setText("Age: HIDDEN");
            txtEmail.setText("Email: HIDDEN");
            txtPhone.setText("Phone Number: HIDDEN");
            txtAccountType.setText("Account Type: Admin");

            if (arguments.getBoolean("isViewingOther")) {
                btnEdit.setVisibility(View.GONE);
                btnLogout.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
                
                // Change button text to "Kick" if viewing attendee info
                if (arguments.getBoolean("showAttendeeInfo")) {
                    btnDelete.setText("Kick");
                }
                
                btnDelete.setOnClickListener(x -> {
                    if (mListener != null) {
                        mListener.onProfileDeleted(userId);
                    }
                    dismiss();
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            if (arguments.getBoolean("showEventsButton")) {
                btnEvents.setVisibility(View.VISIBLE);
                btnEvents.setOnClickListener(x -> {
                    if (mListener != null) {
                        mListener.onEventsButtonClicked(userId);
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
                // Show status instead of "Events"
                String status = arguments.getString("eventCount"); // Using eventCount param for status
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
            if (txtUserId != null) txtUserId.setText("User ID: 123456");
            if (txtFullName != null) txtFullName.setText("Full Name: Jane Duckerson");
            if (txtAge != null) txtAge.setText("Age: 22");
            if (txtEmail != null) txtEmail.setText("Email: jane@example.com");
            if (txtPhone != null) txtPhone.setText("Phone Number: (780) 555-0123");
            if (txtAccountType != null) txtAccountType.setText("Account Type: Entrant");
            btnDelete.setVisibility(View.GONE);
            btnEvents.setVisibility(View.GONE);
            txtEventCount.setVisibility(View.GONE);
            txtPastEvents.setVisibility(View.GONE);
            txtPooledEvents.setVisibility(View.GONE);
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(x -> {
                EditProfileSheet.newInstance().show(getParentFragmentManager(), "EditProfileSheet");
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(x -> {
                dismiss();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
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
}
