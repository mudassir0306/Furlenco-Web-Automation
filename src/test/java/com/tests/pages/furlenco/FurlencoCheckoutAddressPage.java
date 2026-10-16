package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Page Object for the "Select Delivery Address" checkout step, verified live at
 * {@code /checkout/address?vertical=<rent|buy>&cartId=<id>}. A test account with at least one
 * saved address will have one pre-selected; {@link #confirmSelectedAddress()} proceeds with
 * whichever is currently selected rather than picking a specific one.
 */
public class FurlencoCheckoutAddressPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(FurlencoCheckoutAddressPage.class);

    private static final String ADDRESS_LIST_CONTAINER = ":text(\"Select Delivery Address\")";
    private static final String SAVED_ADDRESS_RADIO = "input[type='radio']";
    private static final String CONFIRM_LOCATION_BUTTON = "button:has-text('Confirm Location')";
    private static final String ADD_NEW_ADDRESS_BUTTON = "button:has-text('Add New Address')";

    public FurlencoCheckoutAddressPage(Page page) {
        super(page);
    }

    @Step("Check if the Delivery Address step is loaded")
    public boolean isLoaded() {
        // The route change after Checkout can lag on staging, so wait (bounded) for either signal instead of
        // reading the page once.
        try {
            com.framework.utils.WaitUtils.pollUntil("checkout address step shown",
                    () -> currentUrl().contains("/checkout/address")
                            || (page.locator(ADDRESS_LIST_CONTAINER).count() > 0 && page.locator(ADDRESS_LIST_CONTAINER).first().isVisible()),
                    java.time.Duration.ofSeconds(45), java.time.Duration.ofMillis(250));
        } catch (IllegalStateException e) {
            LOGGER.debug("Delivery Address step not seen within 45s: {}", e.getMessage());
        }
        boolean urlMatches = currentUrl().contains("/checkout/address");
        Locator container = page.locator(ADDRESS_LIST_CONTAINER);
        boolean containerVisible = container.count() > 0 && container.first().isVisible();
        LOGGER.info("Checkout address step loaded check: urlMatches={}, containerVisible={}", urlMatches, containerVisible);
        return urlMatches || containerVisible;
    }

    @Step("Check if at least one saved address is available")
    public boolean hasSavedAddress() {
        // Live 2026-10-05: saved addresses are buttons under a "Saved Addresses" heading, not radios.
        return page.locator(SAVED_ADDRESS_RADIO).count() > 0 || getSavedAddressCount() > 0;
    }

    /** Saved address cards are buttons whose text includes a 6-digit pincode (verified live 2026-10-05). */
    @Step("Count saved addresses")
    public int getSavedAddressCount() {
        waitForFirstVisible(":text(\"Saved Addresses\")", 30_000);
        return page.locator("button")
                .filter(new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("\\b\\d{6}\\b")))
                .count();
    }

    private Locator savedAddressWithPincode(String pincode) {
        return page.locator("button")
                .filter(new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("\\b" + pincode + "\\b")));
    }

    /** The selected saved address card carries the border-primary class (verified live on stag 2026-10-06). */
    @Step("Select the saved address with pincode {pincode}")
    public FurlencoCheckoutAddressPage selectSavedAddressWithPincode(String pincode) {
        Locator card = savedAddressWithPincode(pincode).first();
        card.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        card.click();
        return this;
    }

    @Step("Check if the saved address with pincode {pincode} is selected")
    public boolean isSavedAddressSelected(String pincode) {
        String cls = savedAddressWithPincode(pincode).first().getAttribute("class");
        return cls != null && cls.contains("border-primary");
    }

    @Step("Click Add New Address")
    public FurlencoNewAddressPage clickAddNewAddress() {
        waitForFirstVisible(ADD_NEW_ADDRESS_BUTTON, 45_000).click();
        return new FurlencoNewAddressPage(page);
    }

    @Step("Check if the Add New Address action is available")
    public boolean isAddNewAddressAvailable() {
        for (Locator btn : page.locator(ADD_NEW_ADDRESS_BUTTON).all()) {
            if (btn.isVisible()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Proceeds with whichever address is already selected (a test account is expected to have a
     * default/pre-selected saved address) — this class deliberately doesn't pick a specific
     * address, since which one is "correct" is test-data-dependent.
     */
    @Step("Confirm the currently selected delivery address")
    public FurlencoOrderSummaryPage confirmSelectedAddress() {
        LOGGER.info("Confirming selected delivery address");
        waitForFirstVisible(CONFIRM_LOCATION_BUTTON, 45_000).click();
        page.waitForLoadState();
        waitForUrlQuietly("**/checkout/summary**", 5000);
        return new FurlencoOrderSummaryPage(page);
    }
}
