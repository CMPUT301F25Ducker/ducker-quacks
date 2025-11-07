/**
 * @file AppConfig.java
 * @brief Application-wide configuration settings.
 *
 * Contains static fields and methods for managing global configuration,
 * including the current login mode across all activities.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

/**
 * @class AppConfig
 * @brief Central configuration class for global app settings.
 *
 * Provides a static login mode that controls which user type (Admin,
 * Entrant, or Organizer) is active across the entire application.
 */
public class AppConfig {
    
    /**
     * @brief Controls the global login mode across all activities.
     *
     * Possible values: "ADMIN", "ENTRANT", or "ORGANIZER".
     * Modify this field to switch between user types.
     */
    public static String LOGIN_MODE = "ENTRANT";

    /**
     * @brief Updates the login mode.
     * @param mode The new login mode ("ADMIN", "ENTRANT", or "ORGANIZER").
     */
    public static void setLoginMode(String mode) {
        LOGIN_MODE = mode.toUpperCase();
    }
}
