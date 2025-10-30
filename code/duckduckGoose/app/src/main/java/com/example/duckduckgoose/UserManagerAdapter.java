package com.example.duckduckgoose;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserManagerAdapter extends RecyclerView.Adapter<UserManagerAdapter.ViewHolder> {

    private final List<? extends BaseUserItem> users;
    private OnItemClickListener onItemClickListener;
    private final boolean showCheckboxes;

    public UserManagerAdapter(List<? extends BaseUserItem> users) {
        this(users, true); // Default: show checkboxes
    }

    public UserManagerAdapter(List<? extends BaseUserItem> users, boolean showCheckboxes) {
        this.users = users;
        this.showCheckboxes = showCheckboxes;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_manager, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BaseUserItem user = users.get(position);
        holder.txtName.setText(user.getName());
        holder.txtUserId.setText(user.getUserId());

        // Show/hide checkbox based on adapter configuration
        if (holder.checkboxAttendee != null) {
            holder.checkboxAttendee.setVisibility(showCheckboxes ? View.VISIBLE : View.GONE);
        }

        if (user.getExtra() != null && !user.getExtra().isEmpty()) {
            holder.txtExtra.setVisibility(View.VISIBLE);
            holder.txtExtra.setText(user.getExtra());
        } else {
            holder.txtExtra.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(users.get(currentPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtUserId, txtExtra;
        ImageButton btnDelete;
        android.widget.CheckBox checkboxAttendee;

        ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtUserName);
            txtUserId = itemView.findViewById(R.id.txtUserId);
            txtExtra = itemView.findViewById(R.id.txtUserExtra);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            checkboxAttendee = itemView.findViewById(R.id.checkboxAttendee);
        }
    }

    interface BaseUserItem {
        String getName();
        String getUserId();
        String getExtra();
    }

    public interface OnItemClickListener {
        void onItemClick(BaseUserItem user);
    }
}