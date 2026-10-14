package com.framework.utils;

/**
 * Immutable record of one login attempt's test data, written to the Allure report and to
 * {@code target/test-data/login-attempts.csv} by {@link LoginDataRecorder}. The full phone number
 * is deliberately not part of the record: only a masked form is kept.
 *
 * @param testName     the running test (class.method), or "unknown"
 * @param kind         FRESH (new random number) or EXISTING (seed account with pre-existing data)
 * @param label        seed label (e.g. seed-cancel-buy-2) or "fresh" for random numbers
 * @param maskedPhone  phone masked to first 2 + last 2 digits, e.g. 90******52
 * @param otp          OTP entered (staging default; not a secret)
 * @param name         signup name if a new-user signup was completed, else empty
 * @param email        signup email if a new-user signup was completed, else empty
 * @param timestamp    ISO-8601 time of the attempt
 * @param outcome      LOGGED_IN, SIGNED_UP_AND_LOGGED_IN, LOGIN_REJECTED or FAILED
 * @param otpWaitMs    ms from clicking Continue until the OTP boxes appeared (-1 if never reached)
 * @param submitWaitMs ms from submitting the OTP until the dialog closed / signup prompt appeared (-1 if never reached)
 */
public record LoginTestData(String testName, Kind kind, String label, String maskedPhone, String otp,
                            String name, String email, String timestamp, String outcome,
                            long otpWaitMs, long submitWaitMs) {

    public enum Kind { FRESH, EXISTING }

    public static final String LOGGED_IN = "LOGGED_IN";
    public static final String SIGNED_UP_AND_LOGGED_IN = "SIGNED_UP_AND_LOGGED_IN";
    public static final String LOGIN_REJECTED = "LOGIN_REJECTED";
    public static final String FAILED = "FAILED";

    /** First 2 + last 2 digits kept, the rest masked, e.g. 90******52. */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 5) {
            return "****";
        }
        return phone.substring(0, 2) + "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 2);
    }

    /** True when the attempt ended with the user logged in (with or without a signup step). */
    public boolean succeeded() {
        return LOGGED_IN.equals(outcome) || SIGNED_UP_AND_LOGGED_IN.equals(outcome);
    }
}
