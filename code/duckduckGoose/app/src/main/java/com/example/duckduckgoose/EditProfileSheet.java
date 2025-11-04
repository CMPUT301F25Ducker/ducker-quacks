package com.example.duckduckgoose;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentResultOwner;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfileSheet extends BottomSheetDialogFragment {

    // FragmentResult keys so the opener can refresh after save
    public static final String RESULT_KEY = "EditProfileResult";
    public static final String RESULT_SAVED = "saved";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputEditText inpName;
    private TextInputEditText inpAge;
    private TextInputEditText inpEmail;
    private TextInputEditText inpPhone;

    public static EditProfileSheet newInstance() { return new EditProfileSheet(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_profile_edit, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inpName  = v.findViewById(R.id.inpName);
        inpAge   = v.findViewById(R.id.inpAge);
        inpEmail = v.findViewById(R.id.inpEmail);
        inpPhone = v.findViewById(R.id.inpPhone);

        // load current values from firestore into the fields
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(ds -> {
                        if (ds.exists()) {

                            String fullName = ds.getString("fullName");
                            Long ageLong = ds.getLong("age");
                            String email = ds.getString("email");
                            String phone = ds.getString("phone");

                            if (inpName  != null && !TextUtils.isEmpty(fullName)) inpName.setText(fullName);
                            if (inpAge   != null && ageLong != null)            inpAge.setText(String.valueOf(ageLong));
                            if (inpEmail != null && !TextUtils.isEmpty(email))   inpEmail.setText(email);
                            if (inpPhone != null && !TextUtils.isEmpty(phone))   inpPhone.setText(phone);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }

        // Update button
        View btnUpdate = v.findViewById(R.id.btnUpdate);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(x -> saveChanges());
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

    private void saveChanges() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = textOf(inpName);
        String ageText  = textOf(inpAge);
        String email    = textOf(inpEmail);
        String phone    = textOf(inpPhone);

        if (TextUtils.isEmpty(fullName)) {
            inpName.setError("Required");
            inpName.requestFocus();
            return;
        }
        Integer age = null;
        if (!TextUtils.isEmpty(ageText)) {
            try {
                int parsed = Integer.parseInt(ageText.trim());
                if (parsed < 0 || parsed > 150) {
                    inpAge.setError("Enter a valid age");
                    inpAge.requestFocus();
                    return;
                }
                age = parsed;
            } catch (NumberFormatException nfe) {
                inpAge.setError("Enter a number");
                inpAge.requestFocus();
                return;
            }
        }


        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        if (age != null) updates.put("age", age);
        updates.put("email", TextUtils.isEmpty(email) ? null : email);
        updates.put("phone", TextUtils.isEmpty(phone) ? null : phone);

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();

                    // tell whoever opened us that we saved successfully
                    Bundle result = new Bundle();
                    result.putBoolean(RESULT_SAVED, true);
                    FragmentResultOwner owner = getParentFragmentManager();
                    owner.setFragmentResult(RESULT_KEY, result);

                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String textOf(@Nullable TextInputEditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }
}
