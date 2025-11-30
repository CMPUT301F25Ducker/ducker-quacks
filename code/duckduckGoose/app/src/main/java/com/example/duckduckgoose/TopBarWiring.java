/**
 * Helper for wiring top bar actions (for example, profile sheet).
 *
 * Provides a static method to attach the profile bottom sheet to the
 * top-bar profile button in any activity.
 *
 * @author DuckDuckGoose Development Team
 */
package com.example.duckduckgoose;

import android.app.Activity;
import android.view.View;

/**
 * Utility class for attaching top-bar interactions.
 *
 * Contains static helpers only and is not intended to be instantiated.
 */
public final class TopBarWiring {

    /** Prevents instantiation. */
    private TopBarWiring() {}

    /**
     * Attaches a click listener to open the profile sheet from the top bar.
     *
     * Looks up the view with id btnProfile and, if present, opens the profile
     * bottom sheet using the activity's fragment manager.
     *
     * @param activity - Host activity containing a view with id btnProfile
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