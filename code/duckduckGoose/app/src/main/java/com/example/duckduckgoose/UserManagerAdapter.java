/**
 * @file UserManagerAdapter.java
 * @brief RecyclerView adapter for displaying and interacting with User items.
 *
 * Binds a list of users into row views for the user manager screen. Supports
 * optional checkboxes (e.g., for attendee selection) and click callbacks.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;

import java.util.List;

/**
 * @class UserManagerAdapter
 * @brief Adapter for rendering a list of User items in a RecyclerView.
 *
 * Provides a click listener callback and an option to show/hide checkboxes
 * in each row (e.g., for attendee selection flows).
 */
public class UserManagerAdapter extends RecyclerView.Adapter<UserManagerAdapter.ViewHolder> {

    /** Immutable backing list of users to render. */
    private final List<? extends User> users;

    /** Optional item click listener for row taps. */
    private OnItemClickListener onItemClickListener;

    /** Whether to display attendee checkboxes in each row. */
    private final boolean showCheckboxes;

    /**
     * @brief Constructs an adapter with checkboxes visible by default.
     * @param users Non-null list of users to display.
     */
    public UserManagerAdapter(List<? extends User> users) {
        if (users == null) {
            throw new IllegalArgumentException("users cannot be null");
        }
        this.users = users;
        this.showCheckboxes = true;
    }

    /**
     * @brief Constructs an adapter with explicit checkbox visibility.
     * @param users Non-null list of users to display.
     * @param showCheckboxes True to show the attendee checkbox for each item.
     */
    public UserManagerAdapter(List<? extends User> users, boolean showCheckboxes) {
        if (users == null) {
            throw new IllegalArgumentException("users cannot be null");
        }
        this.users = users;
        this.showCheckboxes = showCheckboxes;
    }

    /**
     * @brief Registers a row click callback.
     * @param listener Listener invoked with the clicked User.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * @brief Inflates the item view and wraps it in a ViewHolder.
     * @param parent RecyclerView parent.
     * @param viewType Unused view type for this adapter.
     * @return Newly created ViewHolder.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_manager, parent, false);
        return new ViewHolder(view);
    }

    /**
     * @brief Binds the User at @p position to the given holder.
     * Safely handles missing/null user fields and toggles optional views.
     *
     * @param holder Target ViewHolder for binding.
     * @param position Adapter position of the item to bind.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // verify index is in range (adapter invariants should guarantee this).
        if (position < 0 || position >= users.size()) {
            return;
        }

        User user = users.get(position);
        if (user == null) {
            return;
        }

        if (holder.checkboxAttendee != null) {
            holder.checkboxAttendee.setVisibility(showCheckboxes ? View.VISIBLE : View.GONE);
        }

        holder.txtFullName.setText(user.getFullName() != null ? user.getFullName() : "(no name)");
        holder.txtUserId.setText(user.getUserId() != null ? user.getUserId() : "(no id)");

        if (holder.txtAccountType != null) {
            if (user.getAccountType() != null && !user.getAccountType().isEmpty()) {
                holder.txtAccountType.setVisibility(View.VISIBLE);
                holder.txtAccountType.setText(user.getAccountType());
            } else {
                holder.txtAccountType.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < users.size()) {
                    onItemClickListener.onItemClick(users.get(currentPosition));
                }
            }
        });
    }

    /**
     * @brief Returns the number of items in the adapter.
     * @return Size of the backing user list (0 if list is empty).
     */
    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    /**
     * @class ViewHolder
     * @brief Caches row view references for faster binding.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /** Full name text field. */
        TextView txtFullName;
        /** User ID text field. */
        TextView txtUserId;
        /** Account type text field. */
        TextView txtAccountType;
        /** Optional row delete button (may be unused in this adapter). */
        ImageButton btnDelete;
        /** Optional attendee checkbox. */
        android.widget.CheckBox checkboxAttendee;

        /**
         * @brief Binds layout child views to fields.
         * @param itemView The inflated row view.
         */
        ViewHolder(View itemView) {
            super(itemView);
            txtFullName = itemView.findViewById(R.id.txtUserName);
            txtUserId = itemView.findViewById(R.id.txtUserId);
            txtAccountType = itemView.findViewById(R.id.txtAccountType);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            checkboxAttendee = itemView.findViewById(R.id.checkboxAttendee);
        }
    }

    /**
     * @interface OnItemClickListener
     * @brief Callback for row tap events.
     */
    public interface OnItemClickListener {
        /**
         * @brief Invoked when a user row is tapped.
         * @param user The User represented by the tapped row.
         */
        void onItemClick(User user);
    }
}
