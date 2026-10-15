package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.framework.utils.LoginDataRecorder;
import com.framework.utils.LoginTestData;
import com.framework.utils.RandomDataUtils;
import com.framework.utils.SeedUsers;
import com.framework.utils.WaitUtils;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Page Object for the Furlenco Login dialog: phone number + 4-digit OTP (not a password) —
 * {@code test.user.otp} in the environment properties files holds the default test OTP, not a
 * real password. Staging accepts any phone number with that OTP.
 * <p>
 * <b>Login test data:</b> {@link #loginAsFreshUser()} logs in with a new random number every time
 * (completing signup if prompted) and {@link #loginWithSeed(SeedUsers.SeedUser)} uses a seed account
 * for tests needing pre-existing orders or addresses. Each call is recorded by
 * {@link LoginDataRecorder} (masked phone only): a "Login test data" attachment + parameters on the
 * Allure test, and a row in {@code target/test-data/login-attempts.csv}.
 * <p>
 * <b>OTP lag:</b> staging's OTP request can take a long time (button disabled / aria-busy), so the
 * Continue click waits for an idle button and the OTP boxes / login completion are awaited up to
 * {@code login.otp.wait.ms} / {@code login.submit.wait.ms} (default 60000), with timings logged.
 * <p>
 * <b>Verified against two live, materially different implementations</b> (2026-09-16), confirming
 * Furlenco's live A/B experiment framework (see {@link FurlencoExperiments}) changes not just
 * layout but the login dialog's own markup:
 * <ul>
 *   <li><b>stag.furlenco.com</b> (Radix): dialog is {@code [data-slot='dialog-content']}, OTP boxes
 *       are {@code input[maxlength='1']}, submit button text is "Verify".</li>
 *   <li><b>pp-janus.furlenco.com</b> with {@code experiment_new_desktop=B} (MUI): no dialog
 *       wrapper attribute at all, OTP boxes have no {@code maxlength} attribute, submit button text
 *       is "CONFIRM &amp; CONTINUE" for both the phone and OTP steps.</li>
 * </ul>
 * Every selector below is written to match both: OTP boxes are identified as {@code input[type=
 * 'text']} with no {@code placeholder} attribute (the one trait both variants share and the
 * "Mobile Number"/search inputs don't), and the submit button matches either "Continue" or
 * "Verify" text.
 * <p>
 * The dialog itself is reached by <b>hovering</b> the header's Account menu button (a Radix
 * {@code HoverCard} trigger, not a click target) and then clicking "Login" inside the hover card
 * that appears — see {@link FurlencoHomePage#openLogin()}.
 */
public class FurlencoLoginPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(FurlencoLoginPage.class);
    private static final long REJECTION_IDLE_NANOS = 4_000_000_000L;

    private long lastOtpWaitMs = -1;
    private long lastSubmitWaitMs = -1;

    private static final String PHONE_INPUT = "input[placeholder='Mobile Number']";
    // OTP boxes: input[type=text] with no placeholder attribute — true in both verified variants,
    // and excludes the phone field (placeholder="Mobile Number") and the header search box.
    private static final String OTP_DIGIT_INPUTS = "input[type='text']:not([placeholder])";
    private static final String CONTINUE_OR_VERIFY_BUTTON =
            "button:has-text('Continue'), button:has-text('Verify')";
    // Deliberately scoped to role='alert' only — a bare ".error" class matched unrelated elements
    // elsewhere on the page in live testing (false positive), so it was dropped rather than kept
    // "just in case."
    private static final String ERROR_MESSAGE = "[role='alert']:has-text(\"Invalid\"), [role='alert']:has-text(\"incorrect\")";

    public FurlencoLoginPage(Page page) {
        super(page);
    }

    @Step("Check if login dialog is open")
    public boolean isOpen() {
        return isVisibleAndPresent(PHONE_INPUT) || isOtpStepDisplayed();
    }

    private boolean isVisibleAndPresent(String selector) {
        Locator locator = page.locator(selector);
        return locator.count() > 0 && locator.first().isVisible();
    }

    @Step("Enter phone number")
    public FurlencoLoginPage enterPhoneNumber(String phoneNumber) {
        LOGGER.info("Entering phone number on login form");
        Locator phoneField = page.locator(PHONE_INPUT).first();
        phoneField.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        phoneField.fill(phoneNumber);
        return this;
    }

    /** Ms the last {@link #clickContinue()} took from click until the OTP boxes appeared (-1 if not reached). */
    public long getLastOtpWaitMs() {
        return lastOtpWaitMs;
    }

    /** Ms the last {@link #enterOtpAndSubmit(String)} took until the dialog closed / signup prompt appeared (-1 if not reached). */
    public long getLastSubmitWaitMs() {
        return lastSubmitWaitMs;
    }

    private Locator continueButton() {
        return page.locator(CONTINUE_OR_VERIFY_BUTTON).first();
    }

    /** True when the button exists, is visible, enabled and not {@code aria-busy="true"} (the OTP request lags). */
    private boolean isButtonReady(Locator button) {
        return button.count() > 0 && button.isVisible() && button.isEnabled()
                && !"true".equals(button.getAttribute("aria-busy"));
    }

    private boolean isButtonBusy(Locator button) {
        return button.count() > 0 && (!button.isEnabled() || "true".equals(button.getAttribute("aria-busy")));
    }

    private boolean isOtpBoxVisible() {
        Locator otpInputs = page.locator(OTP_DIGIT_INPUTS);
        return otpInputs.count() > 0 && otpInputs.first().isVisible();
    }

    @Step("Click Continue after entering phone number")
    public FurlencoLoginPage clickContinue() {
        lastOtpWaitMs = -1;
        double otpWait = LoginDefaults.otpWaitMs();
        Locator continueBtn = continueButton();
        // Click only once the button is enabled and not aria-busy; the OTP request can lag for a
        // long time, and the click carries the long timeout explicitly (not the 30s default).
        WaitUtils.pollUntil("login Continue button enabled and not busy",
                () -> isButtonReady(continueBtn),
                Duration.ofMillis((long) otpWait), Duration.ofMillis(100));
        long start = System.nanoTime();
        continueBtn.click(new Locator.ClickOptions().setTimeout(otpWait));
        page.locator(OTP_DIGIT_INPUTS).first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(otpWait));
        lastOtpWaitMs = elapsedMs(start);
        LOGGER.info("OTP step appeared after {} ms", lastOtpWaitMs);
        return this;
    }

    @Step("Check if the OTP step is displayed")
    public boolean isOtpStepDisplayed() {
        Locator otpInputs = page.locator(OTP_DIGIT_INPUTS);
        return otpInputs.count() == 4 && otpInputs.first().isVisible();
    }

    @Step("Enter OTP and submit")
    public FurlencoLoginPage enterOtpAndSubmit(String otp) {
        if (otp.length() != 4) {
            throw new IllegalArgumentException("Expected a 4-digit OTP, got length " + otp.length());
        }
        lastSubmitWaitMs = -1;
        double submitWait = LoginDefaults.submitWaitMs();
        LOGGER.info("Entering OTP on login form (value not logged)");
        Locator digitBoxes = page.locator(OTP_DIGIT_INPUTS);
        for (int i = 0; i < 4; i++) {
            digitBoxes.nth(i).fill(String.valueOf(otp.charAt(i)));
        }
        Locator submitBtn = continueButton();
        long start = System.nanoTime();
        // The form may verify automatically on the 4th digit (button goes busy, and on success the
        // dialog closes and the button disappears), or it may need the submit button clicked. So
        // don't wait for the button to become "ready" — that never happens on an auto-submit success.
        // Instead wait (bounded by login.submit.wait.ms) for the outcome: dialog closed, new-user
        // signup prompt, an error, or the button idle again for a few seconds with the OTP step up
        // (rejection) — clicking the button once if it is ready and nothing has started yet.
        long deadline = start + (long) (submitWait * 1_000_000L);
        long idleSince = -1;
        boolean clicked = false;
        boolean settled = false;
        while (System.nanoTime() < deadline) {
            if (!isOtpBoxVisible() || isNewUserSignupPromptDisplayed() || isErrorDisplayed()) {
                settled = true;
                break;
            }
            if (isButtonBusy(submitBtn)) {
                idleSince = -1;
            } else if (!clicked && isButtonReady(submitBtn)) {
                // Give an auto-submit a moment to kick in before clicking ourselves.
                if (idleSince < 0) {
                    idleSince = System.nanoTime();
                } else if (System.nanoTime() - idleSince > 500_000_000L) {
                    submitBtn.click(new Locator.ClickOptions().setTimeout(submitWait));
                    clicked = true;
                    idleSince = -1;
                }
            } else if (idleSince < 0) {
                idleSince = System.nanoTime();
            } else if (System.nanoTime() - idleSince > REJECTION_IDLE_NANOS) {
                settled = true;
                break;
            }
            page.waitForTimeout(100);
        }
        lastSubmitWaitMs = elapsedMs(start);
        if (settled) {
            LOGGER.info("login completed after {} ms", lastSubmitWaitMs);
        } else {
            LOGGER.warn("OTP step still busy/visible after {} ms (login.submit.wait.ms) — login may have failed",
                    lastSubmitWaitMs);
        }
        return this;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    /**
     * Full login flow: phone number + OTP. Callers pass the phone and OTP (see {@link LoginDefaults};
     * key {@code test.user.otp}) from the active environment (the OTP is the fixed test OTP, e.g.
     * {@code 1234}) — callers pull those via {@code ConfigManager} rather than hardcoding them.
     */
    @Step("Login with phone number and OTP")
    public void loginWithOtp(String phoneNumber, String otp) {
        enterPhoneNumber(phoneNumber);
        clickContinue();
        if (!isOtpStepDisplayed()) {
            throw new IllegalStateException(
                    "Expected the 4-digit OTP step after Continue but it wasn't displayed — the "
                            + "login flow may have changed. Re-verify this page object's selectors "
                            + "against a live session.");
        }
        enterOtpAndSubmit(otp);
    }

    /**
     * Logs in with a brand-new random phone number and the default OTP, completing the new-user
     * signup step (random name/email) if prompted, and records the data (masked phone, label
     * "fresh") via {@link LoginDataRecorder}. Preferred over a fixed account: staging accepts any
     * number, so there is no stale/rate-limited account to trip over.
     *
     * @return the recorded data; check {@link LoginTestData#succeeded()}
     */
    @Step("Login as a fresh random user")
    public LoginTestData loginAsFreshUser() {
        String phone = RandomDataUtils.randomIndianMobileNumber();
        String otp = LoginDefaults.otp();
        String name = "";
        String email = "";
        String outcome;
        try {
            loginWithOtp(phone, otp);
            if (isNewUserSignupPromptDisplayed()) {
                name = RandomDataUtils.randomName();
                email = RandomDataUtils.randomEmail();
                getNewUserSignupPage().enterName(name).enterEmail(email).clickContinue();
                outcome = waitUntilLoggedIn() ? LoginTestData.SIGNED_UP_AND_LOGGED_IN : LoginTestData.FAILED;
            } else {
                outcome = isLoggedIn() ? LoginTestData.LOGGED_IN : LoginTestData.FAILED;
            }
        } catch (RuntimeException e) {
            LoginDataRecorder.record(LoginDataRecorder.newRecord(LoginTestData.Kind.FRESH, null, phone, otp,
                    name, email, LoginTestData.FAILED, lastOtpWaitMs, lastSubmitWaitMs));
            throw e;
        }
        return LoginDataRecorder.record(LoginDataRecorder.newRecord(LoginTestData.Kind.FRESH, null, phone, otp,
                name, email, outcome, lastOtpWaitMs, lastSubmitWaitMs));
    }

    /**
     * Logs in with a seed account from {@link SeedUsers} (needed only when a test relies on
     * existing orders, saved addresses or a cancellable order) and records it as {@code EXISTING}
     * under the seed's label with a masked phone.
     */
    @Step("Login with seed account {seed}")
    public LoginTestData loginWithSeed(SeedUsers.SeedUser seed) {
        String otp = LoginDefaults.otp();
        String outcome;
        try {
            loginWithOtp(seed.phone(), otp);
            outcome = isLoggedIn() ? LoginTestData.LOGGED_IN : LoginTestData.FAILED;
        } catch (RuntimeException e) {
            LoginDataRecorder.record(LoginDataRecorder.newRecord(LoginTestData.Kind.EXISTING, seed.label(),
                    seed.phone(), otp, "", "", LoginTestData.FAILED, lastOtpWaitMs, lastSubmitWaitMs));
            throw e;
        }
        return LoginDataRecorder.record(LoginDataRecorder.newRecord(LoginTestData.Kind.EXISTING, seed.label(),
                seed.phone(), otp, "", "", outcome, lastOtpWaitMs, lastSubmitWaitMs));
    }

    /**
     * Waits (bounded by {@code login.submit.wait.ms}) until the login dialog AND the new-user signup
     * prompt are both gone — i.e. signup/login has really completed. Unlike {@link #isLoggedIn()},
     * the signup prompt still being visible does not count as "settled", since staging's signup
     * submit can lag.
     */
    @Step("Wait until login/signup has completed")
    public boolean waitUntilLoggedIn() {
        try {
            WaitUtils.pollUntil("login dialog and signup prompt closed",
                    () -> !isOpen() && !isNewUserSignupPromptDisplayed(),
                    Duration.ofMillis((long) LoginDefaults.submitWaitMs()), Duration.ofMillis(200));
        } catch (IllegalStateException e) {
            LOGGER.warn("Signup/login did not complete within login.submit.wait.ms: {}", e.getMessage());
        }
        return !isOpen() && !isNewUserSignupPromptDisplayed();
    }

    @Step("Check if login succeeded")
    public boolean isLoggedIn() {
        try {
            WaitUtils.pollUntil("login dialog closed or new-user prompt shown",
                    () -> !isOpen() || isNewUserSignupPromptDisplayed(),
                    Duration.ofMillis(1500), Duration.ofMillis(100));
        } catch (IllegalStateException e) {
            LOGGER.debug("Login outcome not observed within bound: {}", e.getMessage());
        }
        return !isOpen() && !isNewUserSignupPromptDisplayed();
    }

    /**
     * A phone number with no existing account redirects to a "Hey, Looks like you are new here!"
     * name/email signup step instead of completing login directly — check this after
     * {@link #enterOtpAndSubmit(String)} before assuming {@link #isLoggedIn()} means an existing
     * account was authenticated.
     */
    @Step("Check if the OTP submission redirected to new-user signup instead of logging in")
    public boolean isNewUserSignupPromptDisplayed() {
        return new FurlencoNewUserSignupPage(page).isLoaded();
    }

    @Step("Get the new-user signup page after OTP submission")
    public FurlencoNewUserSignupPage getNewUserSignupPage() {
        return new FurlencoNewUserSignupPage(page);
    }

    @Step("Check if a login error is displayed")
    public boolean isErrorDisplayed() {
        Locator error = page.locator(ERROR_MESSAGE);
        return error.count() > 0 && error.first().isVisible();
    }
}
