package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;

import java.util.ArrayList;
import java.util.List;

public class OrganizerManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    private List<User> organizers;
    private UserManagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        WindowInsetsController controller = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller = getWindow().getInsetsController();
        }
        if (controller != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        AutoCompleteTextView dropFilter = findViewById(R.id.dropFilterOrganizers);
        if (dropFilter != null) {
            String[] counts = {"Any", "1-5", "6-10", "11+"};
            ArrayAdapter<String> stringAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, counts);
            dropFilter.setAdapter(stringAdapter);
        }

        RecyclerView rv = findViewById(R.id.rvOrganizers);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            organizers = new ArrayList<>();

            adapter = new UserManagerAdapter(organizers, false); // false = hide checkboxes
            adapter.setOnItemClickListener(user -> {
                String extra = (user.getEmail() != null) ? user.getEmail() : "";
                ProfileSheet.newInstance(user, true, true, extra, false).show(getSupportFragmentManager(), "ProfileSheet");
            });
            rv.setAdapter(adapter);
        }
    }

    @Override
    public void onProfileDeleted(String userId) {
        for (int i = 0; i < organizers.size(); i++) {
            if (organizers.get(i).getUserId().equals(userId)) {
                organizers.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    @Override
    public void onEventsButtonClicked(String userId) {
        startActivity(new Intent(this, EventManagerActivity.class));
    }
}
