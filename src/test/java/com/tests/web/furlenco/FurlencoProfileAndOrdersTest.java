package com.tests.web.furlenco;

import static org.assertj.core.api.Assertions.assertThat;

import com.tests.base.BaseWebTest;
import com.tests.pages.furlenco.FurlencoHomePage;
import com.tests.pages.furlenco.FurlencoLoginPage;
import com.tests.pages.furlenco.FurlencoMyOrdersPage;
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
 * Profile, OTP login and order-related scenarios from the PDF (Access User Profile and Login,
 * User Login with OTP Verification, Verify Order Details Consistency, Upsell with mandate
 * Autopay, DIY return).
 * <p>
 * Login uses {@code test.user.username} / {@code test.user.password} (the fixed test OTP) from the
 * active config. The Upsell/Autopay and DIY return tests are disabled: they need page objects for
 * flows whose locators have not been inspected.
 */
@Epic("Furlenco Web Automation")
@Feature("Profile and Orders")
public class FurlencoProfileAndOrdersTest extends BaseWebTest {

    private String furlencoUrl;
    private FurlencoHomePage homePage;

    @BeforeMethod(alwaysRun = true)
    public void initHomePage() {
        furlencoUrl = config.get("furlenco.base.url", "https://www.furlenco.com");
        homePage = new FurlencoHomePage(page);
    }

    private void login() {
        FurlencoLoginPage loginPage = homePage.openLogin();
        assertThat(loginPage.isOpen()).as("Login dialog should open from the Account menu").isTrue();
        loginPage.enterPhoneNumber(config.get("test.user.username")).clickContinue();
        assertThat(loginPage.isOtpStepDisplayed()).as("OTP step should follow the phone number").isTrue();
        loginPage.enterOtpAndSubmit(config.get("test.user.password"));
        assertThat(loginPage.isLoggedIn()).as("User should be logged in after a valid OTP").isTrue();
    }

    @Test(groups = {"smoke", "web", "furlenco"}, priority = 1)
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify login with phone number and OTP verification from the profile menu")
    public void verifyLoginWithOtpVerification() {
        homePage.open(furlencoUrl);

        login();
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify a wrong OTP does not log the user in")
    public void verifyWrongOtpIsRejected() {
        homePage.open(furlencoUrl);
        FurlencoLoginPage loginPage = homePage.openLogin();
        loginPage.enterPhoneNumber(config.get("test.user.username")).clickContinue();
        assertThat(loginPage.isOtpStepDisplayed()).isTrue();

        loginPage.enterOtpAndSubmit("0000");

        assertThat(loginPage.isLoggedIn()).as("A wrong OTP must not log the user in").isFalse();
    }

    @Test(groups = {"smoke", "web", "furlenco"}, priority = 3)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify the logged-in user can open My Orders and see their orders")
    public void verifyAccessMyOrdersAfterLogin() {
        homePage.open(furlencoUrl);
        login();

        FurlencoMyOrdersPage myOrders = new FurlencoMyOrdersPage(page).open(furlencoUrl);

        assertThat(myOrders.isLoaded()).as("My Orders should load").isTrue();
        assertThat(myOrders.getOrderCount()).as("Test account should have orders").isGreaterThan(0);
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 4)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify order detail opens from My Orders for a Rent and a Buy order")
    public void verifyOrderDetailsOpen() {
        homePage.open(furlencoUrl);
        login();

        FurlencoMyOrdersPage myOrders = new FurlencoMyOrdersPage(page).open(furlencoUrl);
        myOrders.openFirstRentOrder();
        assertThat(myOrders.isOrderDetailLoaded()).as("Rent order detail should load").isTrue();

        myOrders.open(furlencoUrl).openFirstBuyOrder();
        assertThat(myOrders.isOrderDetailLoaded()).as("Buy order detail should load").isTrue();
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 5, enabled = false)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TODO: Upsell an order with mandate Autopay (needs upsell and mandate page objects)")
    public void verifyUpsellOrderWithMandateAutopay() {
        // 1. login  2. open an active Rent order  3. start an upsell, add an item
        // 4. choose Autopay mandate on payment  5. assert the mandate is set up and the order confirmed
    }

    @TmsLink("TC-505")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 6, enabled = false)
    @Severity(SeverityLevel.CRITICAL)
    @Description("TODO: DIY product return without pay (needs return-flow page object)")
    public void verifyDiyReturnWithoutPay() {
        // 1. login  2. open an eligible DIY order  3. start a return  4. complete it without payment
        // 5. assert the return is created
    }

    @Test(groups = {"furlenco", "auth-profile", "backlog"}, enabled = false)
    @TmsLink("TC-560")
    @Description("TC-560 Login Using Database Phone Number")
    public void testLoginUsingDatabasePhoneNumber_TC560() {
        // TODO(TC-560): read a test phone number from the database, then log in with it and the fixed test OTP.
        // Needs: DB access for the active environment (the Furlenco DBUtils helper has not been ported to this repo)
        // and agreement on which table or query supplies the number. Never read real customer numbers.
        throw new SkipException("TODO: TC-560 needs database access");
    }

    @Test(groups = {"furlenco", "orders-returns", "backlog"}, enabled = false)
    @TmsLink("TC-563")
    @Description("TC-563 Perform Order Management System Query")
    public void testPerformOrderManagementSystemQuery_TC563() {
        // TODO(TC-563): this is a backend check, not a screen flow. Confirm the Order Management System endpoint or query,
        // its auth, and what to assert (for example an order's status for a seeded test order).
        // Likely belongs in an API test class once the endpoint is known.
        throw new SkipException("TODO: TC-563 needs the OMS endpoint or query");
    }
}
