package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Page Object for the "Add New Address" flow, verified live 2026-10-05 at
 * {@code /checkout/address/new?vertical=rent&cartId=<id>}: a map with a search box
 * ({@code #location-search}); typing shows suggestion buttons; picking one and clicking
 * "Confirm Location" opens an "Enter Complete Address" form ending in "Save Address".
 * <p>
 * The suggestions only appear for real keystrokes, so the search uses {@code pressSequentially}
 * rather than {@code fill}. This class deliberately never clicks "Save Address": saving would
 * create a permanent address on the test account.
 */
public class FurlencoNewAddressPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(FurlencoNewAddressPage.class);

    private static final String SEARCH_INPUT = "#location-search";
    private static final String SUGGESTION = "button[class*='w-full'][class*='text-left']";
    private static final String CONFIRM_LOCATION_BUTTON = "button:text-is('Confirm Location')";
    private static final String ADDRESS_FORM_HEADING = ":text('Enter Complete Address')";
    private static final String SAVE_ADDRESS_BUTTON = "button:text-is('Save Address')";
    private static final String[] FORM_FIELD_NAMES = {
        "name", "contact_no", "alt_contact", "floor", "flat_no", "apt_name", "area", "landmark", "pincode", "city"
    };

    public FurlencoNewAddressPage(Page page) {
        super(page);
    }

    @Step("Check if the Add New Address map step is loaded")
    public boolean isLoaded() {
        Locator input = page.locator(SEARCH_INPUT);
        input.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return currentUrl().contains("/checkout/address/new") && input.isVisible();
    }

    /** The search box is rendered disabled (cursor-not-allowed) until the map/Places finish loading. */
    private boolean awaitSearchEnabled(Locator input) {
        return waitUntilVisibleAndEnabled(input, 45_000);
    }

    @Step("Search address: {query}")
    public FurlencoNewAddressPage searchAddress(String query) {
        LOGGER.info("Searching address for [{}]", query);
        Locator input = page.locator(SEARCH_INPUT);
        awaitSearchEnabled(input);
        input.click();
        input.fill("");
        input.pressSequentially(query);
        page.locator(SUGGESTION).first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    /**
     * Types a query and returns how many suggestions appear within {@code waitMs}, without failing
     * when there are none. On stag.furlenco.com the Google Maps area is blank and no suggestions
     * ever appear (observed 2026-10-06), so callers can tell "no results" from "search unavailable".
     */
    @Step("Search address {query} and count suggestions")
    public int searchAndCountSuggestions(String query, int waitMs) {
        Locator input = page.locator(SEARCH_INPUT);
        if (!awaitSearchEnabled(input)) {
            LOGGER.warn("Address search input stayed disabled: search is unavailable");
            return 0;
        }
        input.click();
        input.fill("");
        input.pressSequentially(query);
        // Verified behaviour (2026-10-06, still true 2026-10-15): on staging the Google Maps area is blank and no
        // suggestions appear, so this waits at most waitMs and returns 0 instead of throwing.
        waitQuietly(page.locator(SUGGESTION).first(), WaitForSelectorState.VISIBLE, waitMs);
        return page.locator(SUGGESTION).count();
    }

    @Step("Count address suggestions")
    public int getSuggestionCount() {
        return page.locator(SUGGESTION).count();
    }

    @Step("Read address suggestion {index}")
    public String getSuggestionText(int index) {
        return page.locator(SUGGESTION).nth(index).innerText();
    }

    @Step("Pick address suggestion {index}")
    public FurlencoNewAddressPage selectSuggestion(int index) {
        page.locator(SUGGESTION).nth(index).click();
        page.locator(CONFIRM_LOCATION_BUTTON).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    @Step("Confirm the chosen map location")
    public FurlencoNewAddressPage confirmLocation() {
        page.locator(CONFIRM_LOCATION_BUTTON).click();
        page.locator(ADDRESS_FORM_HEADING).first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    @Step("Check if the Enter Complete Address form is displayed with all fields")
    public boolean isAddressFormDisplayed() {
        if (!page.locator(ADDRESS_FORM_HEADING).first().isVisible()) {
            return false;
        }
        for (String name : FORM_FIELD_NAMES) {
            if (page.locator("input[name='" + name + "']").count() == 0) {
                LOGGER.warn("Address form field missing: {}", name);
                return false;
            }
        }
        return true;
    }

    @Step("Check if Save Address is available")
    public boolean isSaveAddressAvailable() {
        return page.locator(SAVE_ADDRESS_BUTTON).isVisible();
    }

    @Step("Read the pincode pre-filled in the address form")
    public String getFormPincode() {
        return page.locator("input[name='pincode']").inputValue();
    }
}
