package io.lettuce.scenario;

/**
 * Interface for capturing Redis Enterprise maintenance event notifications. This allows different capture implementations to
 * work with the same push notification monitoring utility.
 */
public interface MaintenanceNotificationCapture {

    /**
     * Captures a maintenance notification in RESP3 format
     * 
     * @param notification the notification string in RESP3 format
     */
    void captureNotification(String notification);

}
