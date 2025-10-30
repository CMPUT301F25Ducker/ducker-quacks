package com.example.duckduckgoose;

import android.content.Context;
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
    }

    private OnProfileInteractionListener mListener;

    public static ProfileSheet newInstance() {
        return new ProfileSheet();
    }

    public static ProfileSheet newInstance(String name, String userId) {
        return newInstance(name, userId, false);
    }

    public static ProfileSheet newInstance(String name, String userId, boolean isViewingOther) {
        ProfileSheet fragment = new ProfileSheet();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("userId", userId);
        args.putBoolean("isViewingOther", isViewingOther);
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

        MaterialButton btnEdit = v.findViewById(R.id.btnEdit);
        MaterialButton btnLogout = v.findViewById(R.id.btnLogout);
        MaterialButton btnDelete = v.findViewById(R.id.btnDelete);

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
                btnDelete.setOnClickListener(x -> {
                    if (mListener != null) {
                        mListener.onProfileDeleted(userId);
                    }
                    dismiss();
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        } else {
            if (txtUserId != null) txtUserId.setText("User ID: 123456");
            if (txtFullName != null) txtFullName.setText("Full Name: Jane Duckerson");
            if (txtAge != null) txtAge.setText("Age: 22");
            if (txtEmail != null) txtEmail.setText("Email: jane@example.com");
            if (txtPhone != null) txtPhone.setText("Phone Number: (780) 555-0123");
            if (txtAccountType != null) txtAccountType.setText("Account Type: Entrant");
            btnDelete.setVisibility(View.GONE);
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

        ImageButton btnClose = v.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(x -> dismiss());
        }
    }
}
