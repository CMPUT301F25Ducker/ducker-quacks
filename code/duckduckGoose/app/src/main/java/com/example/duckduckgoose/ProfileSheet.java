package com.example.duckduckgoose;

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

    public static ProfileSheet newInstance() {
        return new ProfileSheet();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_profile, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        // Populate with fake data
        TextView txtUserId = v.findViewById(R.id.txtUserId);
        TextView txtFullName = v.findViewById(R.id.txtFullName);
        TextView txtAge = v.findViewById(R.id.txtAge);
        TextView txtEmail = v.findViewById(R.id.txtEmail);
        TextView txtPhone = v.findViewById(R.id.txtPhone);
        TextView txtAccountType = v.findViewById(R.id.txtAccountType);

        if (txtUserId != null) txtUserId.setText("User ID: 123456");
        if (txtFullName != null) txtFullName.setText("Full Name: Jane Duckerson");
        if (txtAge != null) txtAge.setText("Age: 22");
        if (txtEmail != null) txtEmail.setText("Email: jane@example.com");
        if (txtPhone != null) txtPhone.setText("Phone Number: (780) 555-0123");
        if (txtAccountType != null) txtAccountType.setText("Account Type: Entrant");

        // Edit button
        MaterialButton btnEdit = v.findViewById(R.id.btnEdit);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(x -> {
                EditProfileSheet.newInstance().show(getParentFragmentManager(), "EditProfileSheet");
            });
        }

        // Logout button
        MaterialButton btnLogout = v.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(x -> {
                // Dismiss the sheet first
                dismiss();

                // Navigate to login screen
                if (getActivity() != null) {
                    android.content.Intent intent = new android.content.Intent(getActivity(), MainActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("startOn", "LOGIN");
                    startActivity(intent);
                    getActivity().finish();
                }
            });
        }

        // Close button
        ImageButton btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(x -> dismiss());
        }
    }
}