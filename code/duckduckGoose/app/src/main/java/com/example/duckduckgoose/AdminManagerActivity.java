package com.example.duckduckgoose;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.Arrays;
import java.util.List;

public class AdminManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnAddAdmin = findViewById(R.id.btnAddAdmin);
        if (btnAddAdmin != null) {
            btnAddAdmin.setOnClickListener(v -> {
                // TODO: Show dialog to add new admin
            });
        }

        RecyclerView rv = findViewById(R.id.rvAdmins);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            List<UserItem> admins = Arrays.asList(
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null)
            );
            rv.setAdapter(new UserManagerAdapter(admins));
        }
    }

    static class UserItem implements UserManagerAdapter.BaseUserItem {
        String name, userId, extra;
        UserItem(String n, String u, String e) { name = n; userId = u; extra = e; }

        @Override public String getName() { return name; }
        @Override public String getUserId() { return userId; }
        @Override public String getExtra() { return extra; }
    }
}