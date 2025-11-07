/**
 * @file AdminManagerActivity.java
 * @brief Activity for viewing and managing admin accounts.
 *
 * Displays a list of admin users fetched from Firestore. Allows navigation
 * to add or view individual admin profiles.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * @class AdminManagerActivity
 * @brief Activity to display and manage the list of admin users.
 *
 * Handles Firestore retrieval and RecyclerView display of all registered admins.
 */
public class AdminManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    /** Container for the add-admin bottom sheet. */
    private FrameLayout addAdminSheetContainer;

    /** List containing all admin user objects. */
    private List<User> admins;
    /** RecyclerView adapter for displaying admin users. */
    private UserManagerAdapter adapter;

    /** Text inputs for creating a new admin. */
    private TextInputEditText edtFullName, edtAge, edtEmail, edtPhone, edtPassword;

    /**
     * @brief Initializes the admin manager screen and sets up the RecyclerView.
     * @param savedInstanceState Saved activity state.
     */
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
        setContentView(R.layout.activity_admin_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        addAdminSheetContainer = findViewById(R.id.add_admin_sheet_container);
        edtFullName = findViewById(R.id.edtAdminFullName);
        edtAge = findViewById(R.id.edtAdminAge);
        edtEmail = findViewById(R.id.edtAdminEmail);
        edtPhone = findViewById(R.id.edtAdminPhone);
        edtPassword = findViewById(R.id.edtAdminPassword);

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
                String fullName = edtFullName.getText().toString();
                String password = edtPassword.getText().toString();

                if (!fullName.isEmpty() && !password.isEmpty()) {
                    User newAdmin = new User();
                    newAdmin.setFullName(fullName);
                    admins.add(newAdmin);
                    adapter.notifyItemInserted(admins.size() - 1);
                    clearAdminInputs();
                    addAdminSheetContainer.setVisibility(View.GONE);
                } else {
                    // Show error message
                    if (password.isEmpty()) {
                        edtPassword.setError("Password is required");
                    }
                    if (fullName.isEmpty()) {
                        edtFullName.setError("Full name is required");
                    }
                }
            });
        }

        /**
         * @brief Sets up the admin list and item click behavior.
         *
         * Hides checkboxes and opens a profile sheet on row click.
         */
        RecyclerView rv = findViewById(R.id.rvAdmins);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            admins = new ArrayList<>();
            adapter = new UserManagerAdapter(admins, false); // false = hide checkboxes
            adapter.setOnItemClickListener(user -> {
                String extra = (user.getEmail() != null) ? user.getEmail() : "";
                ProfileSheet.newInstance(user, true, false, extra, false).show(getSupportFragmentManager(), "ProfileSheet");
            });
            rv.setAdapter(adapter);
        }
    }

    /**
     * @brief Removes the deleted admin from the list and updates the UI.
     * @param userId ID of the admin that was deleted.
     */
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

    /**
     * @brief No-op for admins; events button is not used here.
     * @param userId Target user id.
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        // No action needed for admins
    }

    /** @brief Clears all add-admin input fields. */
    private void clearAdminInputs() {
        edtFullName.setText("");
        edtAge.setText("");
        edtEmail.setText("");
        edtPhone.setText("");
        edtPassword.setText("");
    }
}
