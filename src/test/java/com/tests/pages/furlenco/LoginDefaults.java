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

    /** Max wait for the OTP boxes after Continue ({@code login.otp.wait.ms}, default 60000): the OTP request can lag. */
    public static long otpWaitMs() {
        return ConfigManager.getInstance().getInt("login.otp.wait.ms", 60000);
    }

    /** Max wait for login to complete after submitting the OTP ({@code login.submit.wait.ms}, default 60000). */
    public static long submitWaitMs() {
        return ConfigManager.getInstance().getInt("login.submit.wait.ms", 60000);
    }
}
