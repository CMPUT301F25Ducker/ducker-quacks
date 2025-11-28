package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rv;
    private AutoCompleteTextView dropSort;
    private NotifAdapter adapter;
    private List<NotificationItem> fullList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_logs);

        TopBarWiring.attachProfileSheet(this);

        rv = findViewById(R.id.recyclerNotifications);
        dropSort = findViewById(R.id.dropSort);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotifAdapter(fullList);
        rv.setAdapter(adapter);

        setupSorting();

        loadNotifications();
    }

    private void setupSorting() {
        if (dropSort == null) return;

        String[] sortOptions = {"Newest First", "Oldest First"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sortOptions);
        dropSort.setAdapter(sortAdapter);
        dropSort.setText(sortOptions[0], false);

        dropSort.setOnItemClickListener((parent, view, position, id) -> {
            String selected = sortAdapter.getItem(position);
            sortList(selected);
        });
    }

    private void sortList(String criterion) {
        if (fullList.isEmpty()) return;

        if ("Oldest First".equals(criterion)) {
            Collections.sort(fullList, Comparator.comparing(NotificationItem::getDate));
        } else {
            // Newest First
            Collections.sort(fullList, (a, b) -> b.getDate().compareTo(a.getDate()));
        }
        adapter.notifyDataSetChanged();
    }

    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    fullList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            String msg = doc.getString("message");
                            Date timestamp = doc.getDate("timestamp");
                            if (timestamp == null) timestamp = new Date();

                            fullList.add(new NotificationItem(msg, timestamp));
                        }
                    }
                    sortList(dropSort.getText().toString());
                });
    }

    private static class NotificationItem {
        private String message;
        private Date date;

        NotificationItem(String m, Date d) {
            this.message = m;
            this.date = d;
        }
        public String getMessage() { return message; }
        public Date getDate() { return date; }
    }

    private class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.Vh> {
        private List<NotificationItem> data;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        NotifAdapter(List<NotificationItem> d) { data = d; }

        @NonNull @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_notification, parent, false);
            return new Vh(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh holder, int position) {
            NotificationItem item = data.get(position);

            holder.txtTitle.setText(item.getMessage());

            // map date to organizer field
            holder.txtOrganizer.setText(sdf.format(item.getDate()));
        }

        @Override
        public int getItemCount() { return data.size(); }

        class Vh extends RecyclerView.ViewHolder {
            TextView txtTitle, txtOrganizer;

            Vh(View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txtNotifTitle);
                txtOrganizer = itemView.findViewById(R.id.txtOrganizer);
            }
        }
    }
}
