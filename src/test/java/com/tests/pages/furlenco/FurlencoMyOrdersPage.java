package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Page Object for "My Orders" ({@code /my/orders}) and an individual order's detail page
 * ({@code /my/orders/cart-checkouts/&lt;id&gt;}) — verified live. Each order card shows an
 * order-status badge, a "TRACK BUY ORDER"/"TRACK RENTAL ORDER" link, and "View Details"; the
 * detail page shows a "Cancel" link ({@code /my/orders/cancel/&lt;id&gt;}) next to a still-active
 * order's status.
 * <p>
 * <b>Unverified beyond this point:</b> the actual cancel-confirmation screen (reached by
 * following that "Cancel" link) was not inspected this session — navigating directly to a
 * {@code /cancel/} URL was correctly treated as a potentially-destructive action and blocked.
 * {@link #confirmCancellation()} is a best-effort guess at common confirm-dialog copy
 * ("Confirm"/"Yes, Cancel"); re-verify against a real run and tighten before relying on this in
 * CI.
 */
public class FurlencoMyOrdersPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(FurlencoMyOrdersPage.class);

    private static final String ORDER_CARD = "a[href*='/my/orders/cart-checkouts/']";
    // The order-type label ("RENTAL ORDER" / "TRACK BUY ORDER") is a sibling of the details link, NOT inside
    // it (the link only wraps "View Details"), so find the label first and then the link of the same card.
    private static final String LOWER_TO_UPPER = "translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')";
    private static final String CARD_LINK_OF_LABEL =
            "/ancestor::div[.//a[contains(@href,'/my/orders/cart-checkouts/')]][1]"
                    + "//a[contains(@href,'/my/orders/cart-checkouts/')]";
    private static final String BUY_ORDER_CARD =
            "xpath=//p[contains(" + LOWER_TO_UPPER + ",'BUY ORDER')]" + CARD_LINK_OF_LABEL;
    // Unlimited orders are labelled "UNLMTD ORDER" / "TRACK UNLMTD ADD ORDER" (seen 2026-10-15).
    private static final String UNLMTD_ORDER_CARD =
            "xpath=//p[contains(" + LOWER_TO_UPPER + ",'UNLMTD')]" + CARD_LINK_OF_LABEL;
    private static final String RENT_ORDER_CARD =
            "xpath=//p[contains(" + LOWER_TO_UPPER + ",'RENTAL ORDER') or contains(" + LOWER_TO_UPPER + ",'RENT ORDER')]"
                    + CARD_LINK_OF_LABEL;
    private static final String CANCEL_LINK = "a[href*='/my/orders/cancel/']";
    private static final String CONFIRM_CANCEL_BUTTON =
            "button:has-text('Confirm'), button:has-text('Yes, Cancel'), button:has-text('Cancel Order')";
    private static final String CANCELLED_INDICATOR = ":text(\"Cancelled\"), :text(\"cancelled\")";

    public FurlencoMyOrdersPage(Page page) {
        super(page);
    }

    @Step("Navigate to My Orders")
    public FurlencoMyOrdersPage open(String baseUrl) {
        navigateTo(baseUrl + "/my/orders");
        page.waitForLoadState();
        waitQuietly(page.locator(ORDER_CARD).first(), WaitForSelectorState.VISIBLE, 3000);
        return this;
    }

    @Step("Check if My Orders is loaded")
    public boolean isLoaded() {
        return currentUrl().contains("/my/orders");
    }

    @Step("Get count of order cards displayed")
    public int getOrderCount() {
        // Order cards load asynchronously after the page shell: wait for the first one before counting.
        waitQuietly(page.locator(ORDER_CARD).first(), WaitForSelectorState.VISIBLE, 30_000);
        return page.locator(ORDER_CARD).count();
    }

    @Step("Open the first Buy order's details")
    public FurlencoMyOrdersPage openFirstBuyOrder() {
        LOGGER.info("Opening first Buy order details");
        Locator card = page.locator(BUY_ORDER_CARD).first();
        card.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        card.click();
        page.waitForLoadState();
        waitForUrlQuietly("**/my/orders/cart-checkouts/**", 3000);
        return this;
    }

    @Step("Open the first Unlimited order's details")
    public FurlencoMyOrdersPage openFirstUnlimitedOrder() {
        LOGGER.info("Opening first Unlimited order details");
        Locator card = page.locator(UNLMTD_ORDER_CARD).first();
        card.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
        card.click();
        page.waitForLoadState();
        waitForUrlQuietly("**/my/orders/cart-checkouts/**", 3000);
        return this;
    }

    @Step("Open the first Rent order's details")
    public FurlencoMyOrdersPage openFirstRentOrder() {
        LOGGER.info("Opening first Rent order details");
        Locator card = page.locator(RENT_ORDER_CARD).first();
        card.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        card.click();
        page.waitForLoadState();
        waitForUrlQuietly("**/my/orders/cart-checkouts/**", 3000);
        return this;
    }

    @Step("Check if this order's detail page is loaded")
    public boolean isOrderDetailLoaded() {
        return currentUrl().contains("/my/orders/cart-checkouts/");
    }

    @Step("Check if a Cancel action is available on this order")
    public boolean isCancelAvailable() {
        // The order detail page loads its content asynchronously (it shows only the header at first), so
        // wait for the Cancel action instead of checking once.
        Locator cancelLink = page.locator(CANCEL_LINK).first();
        return waitQuietly(cancelLink, WaitForSelectorState.VISIBLE, 30_000);
    }

    @Step("Click Cancel on this order")
    public FurlencoMyOrdersPage clickCancel() {
        LOGGER.info("Clicking Cancel on order");
        page.locator(CANCEL_LINK).first().click();
        page.waitForLoadState();
        waitForUrlQuietly("**/my/orders/cancel/**", 3000);
        return this;
    }

    /**
     * Walks the cancel flow that opens after "Cancel" (verified 2026-10-15: /my/orders/cancel/<id> shows
     * "Select products you want to cancel" with a "Select All" control and a CONTINUE button; later
     * screens were not inspected). Each step: select all items / pick the first reason if asked, then
     * press the first forward button from a fixed whitelist. The page text after every step is logged so
     * the later screens are visible in the run log. Stops once the order shows as cancelled.
     */
    @Step("Confirm the order cancellation")
    public FurlencoMyOrdersPage confirmCancellation() {
        LOGGER.info("Walking the order cancellation flow");
        String[] forwardButtons = {"CONTINUE", "CONFIRM", "SUBMIT", "CANCEL ORDER", "YES", "PROCEED"};
        for (int step = 1; step <= 8; step++) {
            if (isCancelled()) {
                break;
            }
            // wait for this screen to settle: some forward button must be present
            Locator forward = null;
            for (String label : forwardButtons) {
                Locator candidate = page.locator("button:has-text('" + label + "')").first();
                if (waitQuietly(candidate, WaitForSelectorState.VISIBLE, step == 1 ? 20_000 : 6_000)) {
                    forward = candidate;
                    break;
                }
            }
            LOGGER.info("[CANCEL STEP {}] {}", step, abbreviate(com.framework.driver.PlaywrightManager.capturePageText()));
            if (forward == null) {
                break; // nothing left to press: the flow is finished (or stuck: the caller asserts)
            }
            if (!forward.isEnabled()) {
                // CONTINUE stays disabled until something is selected. Try the visible "Select All" label,
                // then the checkbox input itself (MUI hides it, so force the click).
                try {
                    page.getByText("Select All").first().click(new Locator.ClickOptions().setTimeout(5_000));
                } catch (com.microsoft.playwright.TimeoutError e) {
                    LOGGER.info("'Select All' label not clickable, trying the checkbox input");
                }
                if (!forward.isEnabled()) {
                    Locator box = page.locator("input[type='checkbox']").first();
                    if (box.count() > 0) {
                        box.click(new Locator.ClickOptions().setForce(true).setTimeout(5_000));
                    }
                }
            }
            Locator reason = page.locator("input[type='radio'], [role='radio']").first();
            if (reason.count() > 0 && reason.isVisible() && !forward.isEnabled()) {
                reason.click();
            }
            forward.click(new Locator.ClickOptions().setTimeout(15_000));
            page.waitForLoadState();
        }
        waitQuietly(page.locator(CANCELLED_INDICATOR).first(), WaitForSelectorState.VISIBLE, 10_000);
        return this;
    }

    private static String abbreviate(String text) {
        String flat = text.replaceAll("\s+", " ");
        return flat.length() > 700 ? flat.substring(0, 700) : flat;
    }

    @Step("Check if the order now shows as cancelled")
    public boolean isCancelled() {
        Locator indicator = page.locator(CANCELLED_INDICATOR);
        return indicator.count() > 0 && indicator.first().isVisible();
    }
}
