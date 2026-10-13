package com.tests.web.furlenco;

import com.tests.base.BaseWebTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.TmsLink;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Subscription renewals (test-case feed sheet). The web account menu has "My Subscriptions"
 * ({@code /my/subscriptions}), so the flow very likely exists on the web app. These stay disabled
 * skeletons because:
 * <ul>
 *   <li>the staging test account has no subscriptions (the page says it has not placed any order),
 *       and renewals depend on the subscription's state (RO state, Active, with or without Autopay,
 *       item or bundle), so each case needs an account seeded into that state;</li>
 *   <li>the renewal screens' locators have not been inspected;</li>
 *   <li>payment steps must use test mode and published test cards, never a real card or bank.</li>
 * </ul>
 * The sheet has no steps; TODOs are from the titles only.
 */
@Epic("Furlenco Web Automation")
@Feature("Renewals")
public class FurlencoRenewalTest extends BaseWebTest {

    @Test(groups = {"furlenco", "renewals", "backlog"}, enabled = false)
    @TmsLink("TC-550")
    @Description("TC-550 DIY Renewal with Pay & X - In-progress - last Kane AI run: failed")
    public void testDiyRenewalWithPayAndXInProgress_TC550() {
        // TODO(TC-550): seed an account with a DIY subscription whose renewal is in progress, open it from My Subscriptions, renew with Pay & X.
        // TODO(TC-550): "Pay & X" is not defined in the sheet; confirm what X means before writing assertions.
        throw new SkipException("TODO: TC-550 not implemented");
    }

    @Test(groups = {"furlenco", "renewals", "backlog"}, enabled = false)
    @TmsLink("TC-555")
    @Description("TC-555 Renew Subscription via Net Banking - last Kane AI run: failed")
    public void testRenewSubscriptionViaNetBanking_TC555() {
        // TODO(TC-555): open a renewable subscription, choose Net Banking in Razorpay test mode, assert the renewal result.
        throw new SkipException("TODO: TC-555 not implemented");
    }

    @Test(groups = {"furlenco", "renewals", "backlog"}, enabled = false)
    @TmsLink("TC-557")
    @Description("TC-557 DIY Renewal without Pay & X - Bundle with RO State without Autopay - last Kane AI run: failed")
    public void testDiyRenewalWithoutPayAndXBundleWithRoStateWithoutAutopay_TC557() {
        // TODO(TC-557): seed a bundle subscription in RO state without Autopay, renew it without Pay & X, assert the result.
        throw new SkipException("TODO: TC-557 not implemented");
    }

    @Test(groups = {"furlenco", "renewals", "backlog"}, enabled = false)
    @TmsLink("TC-569")
    @Description("TC-569 DIY Renewal with Autopay - with Active state - Item")
    public void testDiyRenewalWithAutopayWithActiveStateItem_TC569() {
        // TODO(TC-569): seed an item subscription in Active state with Autopay, renew it, assert the Autopay renewal result.
        throw new SkipException("TODO: TC-569 not implemented");
    }

    @Test(groups = {"furlenco", "renewals", "backlog"}, enabled = false)
    @TmsLink("TC-575")
    @Description("TC-575 Renew Subscription with Credit Card Payment")
    public void testRenewSubscriptionWithCreditCardPayment_TC575() {
        // TODO(TC-575): open a renewable subscription, pay with the Razorpay test card (see FurlencoPaymentPage#payWithCard), assert success.
        throw new SkipException("TODO: TC-575 not implemented");
    }
}
