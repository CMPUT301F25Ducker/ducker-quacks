/**
 * RecyclerView adapter for displaying admin event log items.
 *
 * Binds event log data (title, organizer, and recipients) to card-based list items.
 * Provides functionality to show a dialog with recipients list when the button is clicked.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for binding EventLogItem objects to RecyclerView rows.
 */
public class AdminEventLogAdapter extends RecyclerView.Adapter<AdminEventLogAdapter.ViewHolder> {

    /** Context for accessing resources and creating dialogs. */
    private final Context context;

    /** List of event logs to display. */
    private final List<AdminEventLogsActivity.EventLogItem> eventLogs;

    /**
     * Constructs the adapter with a context and event log list.
     *
     * @param context - Application context
     * @param eventLogs - List of EventLogItem objects
     */
    public AdminEventLogAdapter(Context context, List<AdminEventLogsActivity.EventLogItem> eventLogs) {
        this.context = context;
        this.eventLogs = eventLogs;
    }

    /**
     * Inflates the event log item layout.
     *
     * @param parent - Parent ViewGroup
     * @param viewType - View type (unused)
     * @return New ViewHolder for an event log row
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event_log, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds event log data to a row's views.
     *
     * @param holder - ViewHolder for the row
     * @param position - Position in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminEventLogsActivity.EventLogItem eventLog = eventLogs.get(position);
        holder.txtNotifTitle.setText(eventLog.getTitle());
        holder.txtOrganizer.setText(eventLog.getOrganizer());

        // Set up Recipients button click listener
        holder.btnRecipients.setOnClickListener(v -> showRecipientsDialog(eventLog.getRecipients()));
    }

    /**
     * Returns the total number of event logs.
     *
     * @return Number of items in the list
     */
    @Override
    public int getItemCount() {
        return eventLogs.size();
    }

    /**
     * Shows a dialog displaying the list of recipients.
     *
     * @param recipientIds - List of recipient user IDs
     */
    private void showRecipientsDialog(List<String> recipientIds) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_recipients);

        // Set dialog dimensions
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set up RecyclerView in dialog
        RecyclerView recyclerRecipients = dialog.findViewById(R.id.recyclerRecipients);
        recyclerRecipients.setLayoutManager(new LinearLayoutManager(context));
        
        // Fetch recipient names from Firestore
        List<String> recipientNames = new java.util.ArrayList<>();
        RecipientAdapter recipientAdapter = new RecipientAdapter(recipientNames);
        recyclerRecipients.setAdapter(recipientAdapter);
        
        // Load user IDs for each recipient
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        for (String userId : recipientIds) {
            android.util.Log.d("AdminEventLogs", "Loading recipient: " + userId);
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String userIdField = userDoc.getString("userId");
                            android.util.Log.d("AdminEventLogs", "Recipient found: " + userIdField);
                            recipientNames.add(userIdField != null && !userIdField.isEmpty() ? userIdField : userId);
                        } else {
                            android.util.Log.d("AdminEventLogs", "Recipient user not found");
                            recipientNames.add(userId);
                        }
                        recipientAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("AdminEventLogs", "Error loading recipient", e);
                        recipientNames.add(userId);
                        recipientAdapter.notifyDataSetChanged();
                    });
        }

        // Set up close button
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseDialog);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * ViewHolder for event log item views.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNotifTitle;
        TextView txtOrganizer;
        Button btnRecipients;

        /**
         * Initializes view references.
         *
         * @param itemView - Root view of the event log item
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNotifTitle = itemView.findViewById(R.id.txtNotifTitle);
            txtOrganizer = itemView.findViewById(R.id.txtOrganizer);
            btnRecipients = itemView.findViewById(R.id.btnRecipients);
        }
    }

    /**
     * Simple adapter for displaying recipients in the dialog.
     */
    private static class RecipientAdapter extends RecyclerView.Adapter<RecipientAdapter.RecipientViewHolder> {

        /** List of recipient names to display. */
        private final List<String> recipients;

        /**
         * Constructs the adapter with a list of recipients.
         *
         * @param recipients - List of recipient names
         */
        RecipientAdapter(List<String> recipients) {
            this.recipients = recipients;
        }

        /**
         * Inflates the recipient item layout.
         *
         * @param parent - Parent ViewGroup
         * @param viewType - View type (unused)
         * @return New RecipientViewHolder for a recipient row
         */
        @NonNull
        @Override
        public RecipientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recipient, parent, false);
            return new RecipientViewHolder(view);
        }

        /**
         * Binds recipient name to a row's TextView.
         *
         * @param holder - ViewHolder for the row
         * @param position - Position in the list
         */
        @Override
        public void onBindViewHolder(@NonNull RecipientViewHolder holder, int position) {
            holder.txtRecipientName.setText(recipients.get(position));
        }

        /**
         * Returns the total number of recipients.
         *
         * @return Number of recipients in the list
         */
        @Override
        public int getItemCount() {
            return recipients.size();
        }

        /**
         * ViewHolder for recipient item views.
         */
        static class RecipientViewHolder extends RecyclerView.ViewHolder {
            /** TextView displaying the recipient's name. */
            TextView txtRecipientName;

            /**
             * Initializes view references.
             *
             * @param itemView - Root view of the recipient item
             */
            RecipientViewHolder(@NonNull View itemView) {
                super(itemView);
                txtRecipientName = itemView.findViewById(R.id.txtRecipientName);
            }
        }
    }
}