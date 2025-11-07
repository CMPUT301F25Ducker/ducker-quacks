/**
 * Application-wide configuration settings.
 *
 * <p>Contains static fields and methods for managing global configuration,
 * including the current login mode across all activities.</p>
 *
 * <p><b>Author:</b> DuckDuckGoose Development Team</p>
 */
package com.example.duckduckgoose;

/**
 * Central configuration class for global app settings.
 *
 * <p>Provides a static login mode that controls which user type
 * (Admin, Entrant, or Organizer) is active across the entire application.</p>
 */
public class AppConfig {

    /**
     * Controls the global login mode across all activities.
     *
     * <p>Possible values: {@code "ADMIN"}, {@code "ENTRANT"}, or {@code "ORGANIZER"}.
     * Modify this field to switch between user types.</p>
     */
    public static String LOGIN_MODE = "ENTRANT";

    /**
     * Updates the login mode.
     *
     * @param mode the new login mode ({@code "ADMIN"}, {@code "ENTRANT"}, or {@code "ORGANIZER"})
     */
    public static void setLoginMode(String mode) {
        LOGIN_MODE = mode.toUpperCase();
    }
}
