package com.tests.web.furlenco;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.TmsLink;
import java.util.Set;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Pincode serviceability and availability (PDF scenarios: Non serviceable Pincode, Verify Delivery
 * Availability by Pincode, Select Appliance for Rent by Pincode). Behaviour verified live on
 * stag.furlenco.com 2026-10-06: an unserviceable pincode shows "Pincode is not serviceable" and
 * keeps the previous pincode; the PDP shows a delivery estimate for the selected pincode.
 */
@Epic("Furlenco Web Automation")
@Feature("Pincode Availability")
public class FurlencoPincodeAvailabilityTest extends FurlencoCityStockSupport {

    private static final String SERVICEABLE_PINCODE = "400001";

    @TmsLink("TC-464")
    @Test(groups = {"smoke", "web", "furlenco"}, priority = 1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify a non-serviceable pincode shows an error and keeps the previous pincode")
    public void verifyNonServiceablePincodeShowsError() {
        homePage.open(furlencoUrl);
        homePage.enterPincode(SERVICEABLE_PINCODE);
        assertThat(homePage.getHeaderLocationText()).contains(SERVICEABLE_PINCODE);

        homePage.enterPincode(config.get("test.nonserviceable.pincode", "744101"));

        assertThat(homePage.getLocationDrawerText()).contains("Pincode is not serviceable");
        assertThat(homePage.getLocationDrawerText()).contains(SERVICEABLE_PINCODE);
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify the PDP shows delivery details and updates the pincode when it is changed")
    public void verifyDeliveryAvailabilityOnPdpByPincode() {
        Set<String> inStock = stockPage.open(furlencoUrl, HOME_CITY, CATEGORY).getInStockProductPaths();
        if (inStock.isEmpty()) {
            throw new SkipException("No in-stock " + CATEGORY + " product in " + HOME_CITY);
        }

        pdpPage.open(furlencoUrl, inStock.iterator().next());
        assertThat(pdpPage.isDeliverySectionDisplayed()).as("Delivery & Assembly Details with pincode row").isTrue();
        assertThat(pdpPage.getDeliveryEstimate()).startsWith("Delivery and Assemble by");

        pdpPage.clickChangePincode();
        homePage.enterPincode(SERVICEABLE_PINCODE);

        assertThat(homePage.getHeaderLocationText()).contains(SERVICEABLE_PINCODE);
        assertThat(pdpPage.isDeliverySectionDisplayed()).as("Delivery section still shown after the change").isTrue();
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify appliances for rent can be browsed after selecting a pincode")
    public void verifySelectApplianceForRentByPincode() {
        homePage.open(furlencoUrl);
        homePage.enterPincode(SERVICEABLE_PINCODE);

        homePage.clickRentTab();
        homePage.clickCategory("Appliances");
        page.waitForURL("**/appliances-on-rent**");

        assertThat(page.url()).contains("/mumbai/appliances-on-rent");
        assertThat(stockPage.getInStockProductPaths()).as("In-stock appliances for the pincode").isNotEmpty();
    }
}
