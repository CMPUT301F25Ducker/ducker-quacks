package com.example.duckduckgoose;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    private FrameLayout addAdminSheetContainer;
    private List<UserItem> admins;
    private UserManagerAdapter adapter;

    private TextInputEditText edtUserId, edtFullName, edtAge, edtEmail, edtPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        addAdminSheetContainer = findViewById(R.id.add_admin_sheet_container);
        edtUserId = findViewById(R.id.edtAdminUserId);
        edtFullName = findViewById(R.id.edtAdminFullName);
        edtAge = findViewById(R.id.edtAdminAge);
        edtEmail = findViewById(R.id.edtAdminEmail);
        edtPhone = findViewById(R.id.edtAdminPhone);

        MaterialButton btnAddAdmin = findViewById(R.id.btnAddAdmin);
        if (btnAddAdmin != null) {
            btnAddAdmin.setOnClickListener(v -> addAdminSheetContainer.setVisibility(View.VISIBLE));
        }

        MaterialButton btnAdminCancel = findViewById(R.id.btnAdminCancel);
        if (btnAdminCancel != null) {
            btnAdminCancel.setOnClickListener(v -> {
                clearAdminInputs();
                addAdminSheetContainer.setVisibility(View.GONE);
            });
        }

        MaterialButton btnAdminAdd = findViewById(R.id.btnAdminAdd);
        if (btnAdminAdd != null) {
            btnAdminAdd.setOnClickListener(v -> {
                String userId = edtUserId.getText().toString();
                String fullName = edtFullName.getText().toString();

                if (!userId.isEmpty() && !fullName.isEmpty()) {
                    admins.add(new UserItem(fullName, userId, null));
                    adapter.notifyItemInserted(admins.size() - 1);
                    clearAdminInputs();
                    addAdminSheetContainer.setVisibility(View.GONE);
                }
            });
        }

        RecyclerView rv = findViewById(R.id.rvAdmins);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            admins = new ArrayList<>(Arrays.asList(
                    new UserItem("Admin 1", "admin001", null),
                    new UserItem("Admin 2", "admin002", null),
                    new UserItem("Admin 3", "admin003", null)
            ));
            adapter = new UserManagerAdapter(admins);
            adapter.setOnItemClickListener(user -> ProfileSheet.newInstance(user.getName(), user.getUserId(), true).show(getSupportFragmentManager(), "ProfileSheet"));
            rv.setAdapter(adapter);
        }
    }

    @Override
    public void onProfileDeleted(String userId) {
        for (int i = 0; i < admins.size(); i++) {
            if (admins.get(i).getUserId().equals(userId)) {
                admins.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    private void clearAdminInputs() {
        edtUserId.setText("");
        edtFullName.setText("");
        edtAge.setText("");
        edtEmail.setText("");
        edtPhone.setText("");
    }

    static class UserItem implements UserManagerAdapter.BaseUserItem {
        String name, userId, extra;
        UserItem(String n, String u, String e) { name = n; userId = u; extra = e; }

        @Override public String getName() { return name; }
        @Override public String getUserId() { return userId; }
        @Override public String getExtra() { return extra; }
    }
}
