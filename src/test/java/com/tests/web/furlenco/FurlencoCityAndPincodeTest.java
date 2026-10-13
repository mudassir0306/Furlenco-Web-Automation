package com.tests.web.furlenco;

import static org.assertj.core.api.Assertions.assertThat;

import com.tests.base.BaseWebTest;
import com.tests.pages.furlenco.FurlencoHomePage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.TmsLink;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * City and pincode selection (PDF scenarios: Select Other City, Serviceable Pincode, Verify City
 * Pincode Visibility, Verify and Access Other Cities Pincode). No login needed. Behaviour was
 * checked against www.furlenco.com on 2026-10-05: the header chip shows the city (or
 * "Delhi 110001" after a valid pincode), and an unknown pincode shows "Invalid Pincode Entered"
 * while keeping the previous pincode selected.
 */
@Epic("Furlenco Web Automation")
@Feature("City and Pincode")
public class FurlencoCityAndPincodeTest extends BaseWebTest {

    private String furlencoUrl;
    private FurlencoHomePage homePage;

    @BeforeMethod(alwaysRun = true)
    public void initHomePage() {
        furlencoUrl = config.get("furlenco.base.url", "https://www.furlenco.com");
        homePage = new FurlencoHomePage(page);
        homePage.open(furlencoUrl);
    }

    @Test(groups = {"smoke", "web", "furlenco"}, priority = 1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify the city list in the location drawer shows the main and other cities")
    public void verifyCityListVisibility() {
        homePage.openCityModal();

        String drawer = homePage.getLocationDrawerText();
        assertThat(drawer).contains("Select Delivery Location", "Currently selected pincode", "Other Cities");
        assertThat(drawer).contains("Bengaluru", "Mumbai", "Hyderabad", "Pune", "Delhi", "Chennai");
    }

    @Test(groups = {"smoke", "web", "furlenco"}, priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify selecting another city updates the header location")
    public void verifySelectOtherCity() {
        homePage.selectCity("Mumbai");

        assertThat(homePage.getHeaderLocationText()).as("Header location after selecting Mumbai").contains("Mumbai");
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 3)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify a city from the Other Cities list can be selected")
    public void verifySelectCityFromOtherCities() {
        homePage.selectCity("Jaipur");

        assertThat(homePage.getHeaderLocationText()).as("Header location after selecting Jaipur").contains("Jaipur");
    }

    @Test(groups = {"smoke", "web", "furlenco"}, priority = 4)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify a serviceable pincode is accepted and shown in the header")
    public void verifyServiceablePincode() {
        homePage.enterPincode("110001");

        assertThat(homePage.getHeaderLocationText()).as("Header location after a valid pincode").contains("110001");
    }

    @TmsLink("TC-480")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 5)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify an invalid pincode shows an error and keeps the previous pincode")
    public void verifyInvalidPincodeShowsError() {
        homePage.enterPincode("110001");
        homePage.enterPincode("000000");

        assertThat(homePage.getLocationDrawerText())
                .contains("Invalid Pincode Entered")
                .contains("Currently selected pincode: 110001");
    }
}
