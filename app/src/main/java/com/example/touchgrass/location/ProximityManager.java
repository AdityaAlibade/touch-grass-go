package com.example.touchgrass.location;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Manages optional GPS geofencing, distance calculation via Haversine formula,
 * proximity verification, and user tolerance settings.
 */
public class ProximityManager {

    private static final String PREFS_NAME = "touch_grass_proximity_prefs";
    private static final String KEY_PROXIMITY_ENABLED = "proximity_verification_enabled";
    private static final String KEY_PROXIMITY_RADIUS_METERS = "proximity_radius_meters";

    public static final int DEFAULT_RADIUS_METERS = 500;
    public static final int RADIUS_TIGHT_METERS = 200;
    public static final int RADIUS_WIDE_METERS = 1000;

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private static ProximityManager instance;
    private final SharedPreferences preferences;

    public static class ProximityResult {
        public final boolean locationAvailable;
        public final double distanceMeters;
        public final boolean isWithinRadius;
        public final String formattedDistance;
        public final String statusMessage;

        public ProximityResult(boolean locationAvailable, double distanceMeters,
                               boolean isWithinRadius, String formattedDistance,
                               String statusMessage) {
            this.locationAvailable = locationAvailable;
            this.distanceMeters = distanceMeters;
            this.isWithinRadius = isWithinRadius;
            this.formattedDistance = formattedDistance;
            this.statusMessage = statusMessage;
        }
    }

    public ProximityManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public ProximityManager(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static synchronized ProximityManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProximityManager(context);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public boolean isProximityVerificationEnabled() {
        return preferences.getBoolean(KEY_PROXIMITY_ENABLED, false);
    }

    public void setProximityVerificationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_PROXIMITY_ENABLED, enabled).apply();
    }

    public int getProximityRadiusMeters() {
        return preferences.getInt(KEY_PROXIMITY_RADIUS_METERS, DEFAULT_RADIUS_METERS);
    }

    public void setProximityRadiusMeters(int radiusMeters) {
        preferences.edit().putInt(KEY_PROXIMITY_RADIUS_METERS, radiusMeters).apply();
    }

    /**
     * Calculates great-circle distance between two GPS coordinates using the Haversine formula.
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.cos(rLat1) * Math.cos(rLat2) *
                        Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Formats distance into a human-readable metric string ("350 m" or "2.4 km").
     */
    public static String formatDistance(double distanceMeters) {
        if (distanceMeters < 1000.0) {
            return Math.round(distanceMeters) + " m";
        } else {
            return String.format(Locale.ROOT, "%.1f km", distanceMeters / 1000.0);
        }
    }

    /**
     * Evaluates user proximity against a quest location.
     */
    public ProximityResult evaluateProximity(Double userLat, Double userLon, double questLat, double questLon) {
        if (userLat == null || userLon == null) {
            return new ProximityResult(
                    false,
                    -1.0,
                    false,
                    "Unknown distance",
                    "GPS location unavailable. Exploration allowed."
            );
        }

        double distance = calculateDistanceMeters(userLat, userLon, questLat, questLon);
        int allowedRadius = getProximityRadiusMeters();
        boolean withinRadius = distance <= allowedRadius;

        String formatted = formatDistance(distance);
        String message;
        if (withinRadius) {
            message = "Within exploration radius (" + formatted + " away). Ready for check-in!";
        } else {
            message = formatted + " away (Target radius: " + allowedRadius + " m).";
        }

        return new ProximityResult(true, distance, withinRadius, formatted, message);
    }
}
