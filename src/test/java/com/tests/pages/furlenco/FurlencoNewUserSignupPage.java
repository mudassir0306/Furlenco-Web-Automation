package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Page Object for the new-user signup step reached after OTP verification for a phone number that
 * isn't already registered: "Hey, Looks like you are new here!" (Name + Email + WhatsApp opt-in),
 * and the follow-on "Oops! ... already registered" dialog shown if the entered email belongs to
 * an existing account (offering "Continue as &lt;email&gt;" or "Skip and Create a New Account").
 * <p>
 * Selectors are built directly from screenshots of a live session rather than independently
 * verified by this class's author navigating the flow itself — re-confirm against a real run
 * before trusting them blindly, same as any other page object in this package.
 */
public class FurlencoNewUserSignupPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(FurlencoNewUserSignupPage.class);

    // Verified live across two A/B-experiment variants with different exact copy: "Hey, Looks
    // like you are new here!" vs "Looks like you are New" — match the substring both share
    // ("you are new"), case-insensitively (Playwright's :text() default).
    private static final String NEW_USER_HEADING = ":text(\"you are new\")";
    // Placeholders also vary by variant ("Enter your Name"/"Enter your Email" vs "Name"/"Email
    // Address") — match by substring+case-insensitive rather than exact text. A third variant (seen
    // on staging 2026-10-14) has NO placeholder at all: plain type=text (name) and type=email inputs.
    private static final String NAME_INPUT =
            "input[placeholder*='name' i], input[type='text'][name*='name' i], input[type='text']:not([placeholder])";
    private static final String EMAIL_INPUT = "input[placeholder*='email' i], input[type='email']";
    private static final String WHATSAPP_OPT_IN_CHECKBOX = "input[type='checkbox']";
    private static final String CONTINUE_BUTTON = "button:has-text('Continue')";
    private static final String TERMS_OF_SERVICE_LINK =
            "a:has-text('Terms of Service'), a:has-text('Terms and Conditions'), a:has-text('Terms')";
    private static final String PRIVACY_POLICY_LINK = "a:has-text('Privacy Policy'), a:has-text('Privacy')";

    private static final String ALREADY_REGISTERED_HEADING = ":text(\"Oops\")";
    private static final String ALREADY_REGISTERED_MESSAGE = ":text(\"already registered\")";
    private static final String CONTINUE_AS_EMAIL_BUTTON = "button:has-text('Continue as'), a:has-text('Continue as')";
    private static final String SKIP_CREATE_NEW_ACCOUNT_LINK = ":text(\"Skip and Create a New Account\")";

    public FurlencoNewUserSignupPage(Page page) {
        super(page);
    }

    @Step("Check if the new-user signup screen is displayed")
    public boolean isLoaded() {
        Locator heading = page.locator(NEW_USER_HEADING);
        return heading.count() > 0 && heading.first().isVisible();
    }

    @Step("Enter name: {name}")
    public FurlencoNewUserSignupPage enterName(String name) {
        LOGGER.info("Entering signup name");
        page.locator(NAME_INPUT).first().fill(name);
        return this;
    }

    @Step("Enter email: {email}")
    public FurlencoNewUserSignupPage enterEmail(String email) {
        LOGGER.info("Entering signup email");
        page.locator(EMAIL_INPUT).first().fill(email);
        return this;
    }

    @Step("Check if the WhatsApp promotional opt-in checkbox is checked")
    public boolean isWhatsAppOptInChecked() {
        return page.locator(WHATSAPP_OPT_IN_CHECKBOX).first().isChecked();
    }

    @Step("Toggle the WhatsApp promotional opt-in checkbox")
    public FurlencoNewUserSignupPage toggleWhatsAppOptIn() {
        page.locator(WHATSAPP_OPT_IN_CHECKBOX).first().click();
        return this;
    }

    @Step("Check if Continue is enabled")
    public boolean isContinueEnabled() {
        Locator btn = page.locator(CONTINUE_BUTTON).first();
        return btn.count() > 0 && btn.isEnabled();
    }

    @Step("Click Continue after filling name and email")
    public FurlencoNewUserSignupPage clickContinue() {
        LOGGER.info("Clicking Continue on new-user signup");
        page.locator(CONTINUE_BUTTON).first().click();
        // Outcome is either the already-registered dialog or the signup form going away (account created);
        // wait for whichever happens first (bounded) instead of a fixed pause.
        try {
            com.framework.utils.WaitUtils.pollUntil("duplicate-email dialog shown or signup form closed",
                    () -> isAlreadyRegisteredDialogDisplayed() || !isLoaded(),
                    java.time.Duration.ofSeconds(15), java.time.Duration.ofMillis(200));
        } catch (IllegalStateException e) {
            LOGGER.debug("Signup Continue outcome not observed within 15s: {}", e.getMessage());
        }
        return this;
    }

    @Step("Check if the 'already registered' dialog is displayed")
    public boolean isAlreadyRegisteredDialogDisplayed() {
        Locator heading = page.locator(ALREADY_REGISTERED_HEADING);
        Locator message = page.locator(ALREADY_REGISTERED_MESSAGE);
        return (heading.count() > 0 && heading.first().isVisible())
                || (message.count() > 0 && message.first().isVisible());
    }

    @Step("Click 'Continue as <email>' on the already-registered dialog")
    public void clickContinueAsRegisteredEmail() {
        LOGGER.info("Clicking Continue as (registered email)");
        page.locator(CONTINUE_AS_EMAIL_BUTTON).first().click();
        // The duplicate-email dialog closes once the choice is applied.
        waitQuietly(page.locator(ALREADY_REGISTERED_HEADING + ", " + ALREADY_REGISTERED_MESSAGE).first(),
                WaitForSelectorState.HIDDEN, 10_000);
    }

    @Step("Click 'Skip and Create a New Account' on the already-registered dialog")
    public FurlencoNewUserSignupPage clickSkipAndCreateNewAccount() {
        LOGGER.info("Clicking Skip and Create a New Account");
        page.locator(SKIP_CREATE_NEW_ACCOUNT_LINK).first().click();
        // Observed on staging (2026-10-15): Skip returns to the mobile-number step; older builds kept the
        // signup form open. Wait for the duplicate dialog to close, then for either of those screens.
        waitQuietly(page.locator(ALREADY_REGISTERED_HEADING + ", " + ALREADY_REGISTERED_MESSAGE).first(),
                WaitForSelectorState.HIDDEN, 10_000);
        waitQuietly(page.locator("input[placeholder*='mobile' i], " + NAME_INPUT).first(), WaitForSelectorState.VISIBLE, 10_000);
        return this;
    }

    /**
     * Clicks "Terms of Service" and returns the resulting URL — handles both an in-page navigation
     * and a new-tab ({@code target=_blank}) link, since which one this uses wasn't independently
     * confirmed (B2B's nav link turned out to open a new tab; this may or may not follow the same
     * pattern).
     */
    @Step("Click Terms of Service link")
    public String clickTermsOfService() {
        return clickLinkAndGetUrl(TERMS_OF_SERVICE_LINK);
    }

    @Step("Click Privacy Policy link")
    public String clickPrivacyPolicy() {
        return clickLinkAndGetUrl(PRIVACY_POLICY_LINK);
    }

    private String clickLinkAndGetUrl(String selector) {
        // The signup form is a dialog; the page footer has same-named links hidden underneath its overlay
        // (first match on the plain selector, so clicking it timed out). Prefer the dialog's own link.
        Locator inDialog = page.locator("[role='dialog']").locator(selector).first();
        waitQuietly(inDialog, WaitForSelectorState.VISIBLE, 15_000);
        Locator link = inDialog.count() > 0 ? inDialog : page.locator(selector).first();
        link.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        String target = link.getAttribute("target");
        if ("_blank".equals(target)) {
            Page popup = page.waitForPopup(link::click);
            popup.waitForLoadState();
            return popup.url();
        }
        link.click();
        page.waitForLoadState();
        return page.url();
    }
}
