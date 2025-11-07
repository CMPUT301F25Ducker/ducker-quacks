/**
 * @file TopBarWiring.java
 * @brief Helper for wiring top bar actions (e.g., profile sheet).
 *
 * Provides a static method to attach the profile bottom sheet to the
 * top-bar profile button in any activity.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.app.Activity;
import android.view.View;

/**
 * @class TopBarWiring
 * @brief Utility class for attaching top-bar interactions.
 *
 * Contains static helpers only; not intended to be instantiated.
 */
public final class TopBarWiring {

    /** @brief Prevents instantiation. */
    private TopBarWiring() {}

    /**
     * @brief Attaches a click listener to open the profile sheet from the top bar.
     * @param activity Host activity containing a view with id {@code R.id.btnProfile}.
     *
     * Looks up {@code R.id.btnProfile} and, if present, shows {@link ProfileSheet}
     * using the activity's {@link androidx.fragment.app.FragmentManager}.
     */
    public static void attachProfileSheet(Activity activity) {
        View avatar = activity.findViewById(R.id.btnProfile);
        if (avatar != null) {
            avatar.setOnClickListener(v ->
                    ProfileSheet.newInstance().show(
                            ((androidx.fragment.app.FragmentActivity) activity)
                                    .getSupportFragmentManager(),
                            "ProfileSheet"));
        }
    }
}
