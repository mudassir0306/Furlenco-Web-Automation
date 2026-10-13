package com.tests.web.furlenco;

import com.tests.base.BaseWebTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.TmsLink;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * New-user checkout with and without Autopay (test-case feed sheet). The web order summary has an
 * Autopay option ("Monthly Autopay") next to the payment step. Payment steps must use the
 * Razorpay test mode and published test cards from config, as in {@link FurlencoSuccessfulOrderTest};
 * never a real card. A fresh new-user account needs a random phone number plus the fixed test OTP
 * (see {@link FurlencoNewUserSignupTest}).
 * <p>
 * The sheet has no steps, so these are disabled skeletons with TODOs taken from the titles only.
 */
@Epic("Furlenco Web Automation")
@Feature("Checkout and Autopay")
public class FurlencoCheckoutAutopayTest extends BaseWebTest {

    @Test(groups = {"furlenco", "checkout-payments", "backlog"}, enabled = false)
    @TmsLink("TC-497")
    @Description("TC-497 Checkout order with Autopay with new user")
    public void testCheckoutOrderWithAutopayWithNewUser_TC497() {
        // TODO(TC-497): sign up a new user, add an item, go to checkout, choose Autopay, pay with the Razorpay test card, assert the order succeeds.
        // TODO(TC-497): confirm how the Autopay mandate is completed in test mode (it may need a bank/UPI mandate screen that has no test mode).
        throw new SkipException("TODO: TC-497 not implemented");
    }

    @Test(groups = {"furlenco", "checkout-payments", "backlog"}, enabled = false)
    @TmsLink("TC-509")
    @Description("TC-509 Checkout order without Autopay with new user - last Kane AI run: failed")
    public void testCheckoutOrderWithoutAutopayWithNewUser_TC509() {
        // TODO(TC-509): sign up a new user, add an item, go to checkout, leave Autopay off, pay with the Razorpay test card, assert the order succeeds.
        // TODO(TC-509): the source run failed, so confirm the expected end state first.
        throw new SkipException("TODO: TC-509 not implemented");
    }
}
