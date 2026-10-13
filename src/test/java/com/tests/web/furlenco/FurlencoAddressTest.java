package com.tests.web.furlenco;

import static org.assertj.core.api.Assertions.assertThat;

import com.tests.base.BaseWebTest;
import com.tests.pages.furlenco.FurlencoCartDrawer;
import com.tests.pages.furlenco.FurlencoCheckoutAddressPage;
import com.tests.pages.furlenco.FurlencoHomePage;
import com.tests.pages.furlenco.FurlencoLoginPage;
import com.tests.pages.furlenco.FurlencoNewAddressPage;
import com.tests.pages.furlenco.FurlencoOrderSummaryPage;
import com.tests.pages.furlenco.FurlencoPlpPage;
import com.tests.pages.furlenco.FurlencoProductPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.TmsLink;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Delivery address step of checkout (PDF scenarios: Add New Address, Address change from order
 * summary). Uses the same login and add-to-cart path as {@link FurlencoCheckoutFlowTest}.
 * <p>
 * The new-address tests stop before "Save Address" so they never create addresses on the account.
 */
@Epic("Furlenco Web Automation")
@Feature("Address")
public class FurlencoAddressTest extends BaseWebTest {

    private String furlencoUrl;
    private FurlencoHomePage homePage;

    @BeforeMethod(alwaysRun = true)
    public void initHomePage() {
        furlencoUrl = config.get("furlenco.base.url", "https://www.furlenco.com");
        homePage = new FurlencoHomePage(page);
    }

    private FurlencoCheckoutAddressPage reachAddressStep() {
        homePage.open(furlencoUrl);
        homePage.enterPincode(config.get("test.delivery.pincode", "110001"));
        FurlencoLoginPage loginPage = homePage.openLogin();
        if (loginPage.isOpen()) {
            loginPage.loginWithOtp(config.get("test.user.username"), config.get("test.user.password"));
        }
        homePage.clickRentTab();
        FurlencoPlpPage plpPage = homePage.clickCategory("Bedroom");
        FurlencoProductPage productPage = plpPage.clickFirstAvailableProduct(10);
        productPage.clickAddToCart();

        FurlencoCartDrawer cartDrawer = homePage.openCart();
        return cartDrawer.clickCheckout();
    }

    @Test(groups = {"smoke", "web", "furlenco"}, priority = 1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify the delivery address step offers the Add New Address option")
    public void verifyAddNewAddressOptionAvailable() {
        FurlencoCheckoutAddressPage addressPage = reachAddressStep();

        assertThat(addressPage.isLoaded()).as("Delivery Address step should load").isTrue();
        assertThat(addressPage.isAddNewAddressAvailable()).as("Add New Address should be offered").isTrue();
    }

    @TmsLink("TC-445")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify a saved address can be confirmed and leads to the Order Summary")
    public void verifyConfirmSavedAddressLeadsToOrderSummary() {
        FurlencoCheckoutAddressPage addressPage = reachAddressStep();

        assertThat(addressPage.hasSavedAddress()).as("Test account should have a saved address").isTrue();
        FurlencoOrderSummaryPage summary = addressPage.confirmSelectedAddress();

        assertThat(summary.isLoaded()).as("Order Summary should load after confirming the address").isTrue();
        assertThat(summary.isPriceBreakdownDisplayed()).as("Price breakdown should be shown").isTrue();
    }
    @TmsLink("TC-435")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify Add New Address reaches the Enter Complete Address form (does not save)")
    public void verifyAddNewAddress() {
        FurlencoNewAddressPage newAddress = reachAddressStep().clickAddNewAddress();
        assertThat(newAddress.isLoaded()).as("Add New Address map step should load").isTrue();
        requireAddressSearch(newAddress);

        newAddress.searchAddress("560068").selectSuggestion(0).confirmLocation();

        assertThat(newAddress.isAddressFormDisplayed()).as("Complete Address form with all fields").isTrue();
        assertThat(newAddress.isSaveAddressAvailable()).as("Save Address button").isTrue();
    }

    @TmsLink("TC-434")
    @TmsLink("TC-436")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 4)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify address search returns suggestions for a pincode and for an area")
    public void verifySearchAddressByPincodeAndArea() {
        FurlencoNewAddressPage newAddress = reachAddressStep().clickAddNewAddress();

        requireAddressSearch(newAddress);
        newAddress.searchAddress("560068");
        assertThat(newAddress.getSuggestionCount()).as("Suggestions for a pincode").isGreaterThan(0);
        assertThat(newAddress.getSuggestionText(0)).contains("560068");

        newAddress.searchAddress("HSR Layout");
        assertThat(newAddress.getSuggestionCount()).as("Suggestions for an area").isGreaterThan(0);
        assertThat(newAddress.getSuggestionText(0)).containsIgnoringCase("HSR");
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 5)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify Change on the Cart delivery address opens the saved address list")
    public void verifyChangeAddressFromCart() {
        homePage.open(furlencoUrl);
        homePage.enterPincode(config.get("test.delivery.pincode", "110001"));
        FurlencoLoginPage loginPage = homePage.openLogin();
        if (loginPage.isOpen()) {
            loginPage.loginWithOtp(config.get("test.user.username"), config.get("test.user.password"));
        }
        homePage.clickRentTab();
        FurlencoPlpPage plpPage = homePage.clickCategory("Bedroom");
        plpPage.clickFirstAvailableProduct(10).clickAddToCart();

        FurlencoCheckoutAddressPage addressPage = homePage.openCart().clickChangeAddress();

        assertThat(addressPage.isLoaded()).as("Address list should open from Change").isTrue();
        assertThat(addressPage.getSavedAddressCount()).as("Saved addresses listed").isGreaterThan(0);
    }

    /**
     * The address search needs Google Places. On stag.furlenco.com the map area is blank and no
     * suggestions ever appear (observed 2026-10-06), so tests that need suggestions are skipped
     * there instead of failing.
     */
    private void requireAddressSearch(FurlencoNewAddressPage newAddress) {
        if (newAddress.searchAndCountSuggestions("560068", 5000) == 0) {
            throw new SkipException("Address search returned no suggestions; Google Places looks unavailable here");
        }
    }

    @TmsLink("TC-437")
    @TmsLink("TC-438")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 6)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify an invalid address and an invalid pincode return no suggestions")
    public void verifyInvalidAddressSearchReturnsNoSuggestions() {
        FurlencoNewAddressPage newAddress = reachAddressStep().clickAddNewAddress();
        assertThat(newAddress.isLoaded()).isTrue();
        requireAddressSearch(newAddress);

        assertThat(newAddress.searchAndCountSuggestions("zzzxqqqwww99", 4000))
                .as("Suggestions for a nonsense address").isZero();
        assertThat(newAddress.searchAndCountSuggestions("000000", 4000))
                .as("Suggestions for an invalid pincode").isZero();
    }

    @TmsLink("TC-484")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 7)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify the delivery address can be changed from the Order Summary")
    public void verifyChangeAddressFromOrderSummary() {
        String primary = config.get("test.address.pincode.primary", "560102");
        String alternate = config.get("test.address.pincode.alternate", "560068");

        FurlencoCheckoutAddressPage addressPage = reachAddressStep();
        assertThat(addressPage.getSavedAddressCount()).as("Account needs saved addresses").isGreaterThan(1);

        FurlencoOrderSummaryPage summary = addressPage.selectSavedAddressWithPincode(primary)
                .confirmSelectedAddress().waitForSummary();
        assertThat(summary.getAddressText()).as("Summary address before the change").contains(primary);

        FurlencoCheckoutAddressPage changePage = summary.clickChangeAddress();
        assertThat(changePage.isSavedAddressSelected(primary)).as("Current address is preselected").isTrue();

        summary = changePage.selectSavedAddressWithPincode(alternate).confirmSelectedAddress().waitForSummary();

        assertThat(summary.getAddressText()).as("Summary address after the change").contains(alternate);
    }
}
