package com.example.duckduckgoose;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class EditProfileSheet extends BottomSheetDialogFragment {

    public static EditProfileSheet newInstance() { return new EditProfileSheet(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_profile_edit, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        // Update button
        View btnUpdate = v.findViewById(R.id.btnUpdate);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(x -> {
                // TODO: Save profile changes
                dismiss();
            });
        }

        // Cancel button
        View btnCancel = v.findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(x -> dismiss());
        }

        // Close button
        View btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(x -> dismiss());
        }
    }
}