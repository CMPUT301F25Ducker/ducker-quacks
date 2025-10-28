package com.example.duckduckgoose;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnSignIn;
    private MaterialButton btnCreateAccount;
    private CardView sheetSignIn;
    private CardView sheetCreate;
    private TextView btnSheetCancel1;
    private TextView btnSheetCancel2;
    private MaterialAutoCompleteTextView edtAccountType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnSignIn = findViewById(R.id.btnSignIn);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        sheetSignIn = findViewById(R.id.sheetSignIn);
        sheetCreate = findViewById(R.id.sheetCreate);
        btnSheetCancel1 = findViewById(R.id.btnSheetCancel1);
        btnSheetCancel2 = findViewById(R.id.btnSheetCancel2);
        edtAccountType = findViewById(R.id.edtAccountType);

        if (edtAccountType != null) {
            String[] roles = getResources().getStringArray(R.array.account_types);
            edtAccountType.setSimpleItems(roles);
            edtAccountType.setOnClickListener(v -> edtAccountType.showDropDown());
            edtAccountType.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) edtAccountType.showDropDown();
            });
            edtAccountType.setInputType(0);
            edtAccountType.setKeyListener(null);
        }

        btnSignIn.setOnClickListener(v -> {
            sheetSignIn.setVisibility(View.VISIBLE);
            sheetCreate.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });

        btnCreateAccount.setOnClickListener(v -> {
            sheetCreate.setVisibility(View.VISIBLE);
            sheetSignIn.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });

        btnSheetCancel1.setOnClickListener(v -> {
            sheetSignIn.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.VISIBLE);
            btnCreateAccount.setVisibility(View.VISIBLE);
        });

        btnSheetCancel2.setOnClickListener(v -> {
            sheetCreate.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.VISIBLE);
            btnCreateAccount.setVisibility(View.VISIBLE);
        });
    }
}
