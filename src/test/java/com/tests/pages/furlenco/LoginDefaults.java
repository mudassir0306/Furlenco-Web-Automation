package com.tests.pages.furlenco;

import com.framework.config.ConfigManager;

/** Config lookups for login test data, so tests don't repeat {@code config.get} chains. */
public final class LoginDefaults {

    private LoginDefaults() {
    }

    /** Default OTP ({@code test.user.otp}, falling back to legacy {@code test.user.password}). */
    public static String otp() {
        ConfigManager config = ConfigManager.getInstance();
        return config.get("test.user.otp", config.get("test.user.password", ""));
    }

    /** Fixed account with pre-existing orders/addresses ({@code test.user.history.username}, else {@code test.user.username}). */
    public static String historyPhone() {
        ConfigManager config = ConfigManager.getInstance();
        return config.get("test.user.history.username", config.get("test.user.username", ""));
    }
}
