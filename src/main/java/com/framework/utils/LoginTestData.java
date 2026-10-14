package com.framework.utils;

/**
 * Immutable record of one login attempt's test data, written to the Allure report and to
 * {@code target/test-data/login-attempts.csv} by {@link LoginDataRecorder}.
 *
 * @param testName  the running test (class.method), or "unknown"
 * @param kind      FRESH (new random number) or EXISTING (fixed account with pre-existing data)
 * @param phone     phone number used
 * @param otp       OTP entered (staging default; not a secret)
 * @param name      signup name if a new-user signup was completed, else empty
 * @param email     signup email if a new-user signup was completed, else empty
 * @param timestamp ISO-8601 time of the attempt
 * @param outcome   LOGGED_IN, SIGNED_UP_AND_LOGGED_IN, LOGIN_REJECTED or FAILED
 */
public record LoginTestData(String testName, Kind kind, String phone, String otp, String name,
                            String email, String timestamp, String outcome) {

    public enum Kind { FRESH, EXISTING }

    public static final String LOGGED_IN = "LOGGED_IN";
    public static final String SIGNED_UP_AND_LOGGED_IN = "SIGNED_UP_AND_LOGGED_IN";
    public static final String LOGIN_REJECTED = "LOGIN_REJECTED";
    public static final String FAILED = "FAILED";

    /** True when the attempt ended with the user logged in (with or without a signup step). */
    public boolean succeeded() {
        return LOGGED_IN.equals(outcome) || SIGNED_UP_AND_LOGGED_IN.equals(outcome);
    }
}
