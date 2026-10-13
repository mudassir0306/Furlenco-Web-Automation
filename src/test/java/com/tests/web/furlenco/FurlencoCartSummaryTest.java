package com.tests.web.furlenco;

import com.tests.base.BaseWebTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.TmsLink;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Scenarios from the Kane AI "Cart Summary" screen (test-case feed sheet). On the web app this is
 * the Order Summary page at {@code /checkout/summary}, reached after login, add to cart, Pay and
 * confirming a delivery address (see {@code FurlencoAddressTest#verifyChangeAddressFromOrderSummary}).
 * It has its own address block, coupon box (Apply coupon / APPLY) and an item list with Edit.
 * <p>
 * The sheet has no steps or assertions, so every method here is a disabled skeleton: the TODOs come
 * from the scenario title only. Replace the TODOs with real steps after inspecting the summary page,
 * then remove {@code enabled = false}. Each method throws {@link SkipException} so that enabling it
 * by mistake skips instead of passing.
 */
@Epic("Furlenco Web Automation")
@Feature("Order Summary (Cart Summary)")
public class FurlencoCartSummaryTest extends BaseWebTest {

    @Test(groups = {"furlenco", "cart", "backlog"}, enabled = false)
    @TmsLink("TC-442")
    @Description("TC-442 remove item from cart (Cart Summary)")
    public void testRemoveItemFromCartInCartSummary_TC442() {
        // TODO(TC-442): reach /checkout/summary with two items, remove one, assert the item list, count and total update.
        throw new SkipException("TODO: TC-442 not implemented");
    }

    @Test(groups = {"furlenco", "offers", "backlog"}, enabled = false)
    @TmsLink("TC-443")
    @Description("TC-443 Apply Offer (Cart Summary)")
    public void testApplyOfferInCartSummary_TC443() {
        // TODO(TC-443): reach /checkout/summary, apply an offered coupon from the list, assert it shows as applied and the total drops.
        throw new SkipException("TODO: TC-443 not implemented");
    }

    @Test(groups = {"furlenco", "offers", "backlog"}, enabled = false)
    @TmsLink("TC-463")
    @Description("TC-463 Inline message for invalid offers (Cart Summary) - last Kane AI run: failed")
    public void testInlineMessageForInvalidOffersInCartSummary_TC463() {
        // TODO(TC-463): reach /checkout/summary, enter an invalid code, assert the inline error. The source run failed, so confirm the expected message first.
        throw new SkipException("TODO: TC-463 not implemented");
    }

    @Test(groups = {"furlenco", "cart", "backlog"}, enabled = false)
    @TmsLink("TC-465")
    @Description("TC-465 update cart (Cart Summary)")
    public void testUpdateCartInCartSummary_TC465() {
        // TODO(TC-465): reach /checkout/summary, use Edit on an item to change quantity or plan, assert the price breakup updates.
        throw new SkipException("TODO: TC-465 not implemented");
    }

    @Test(groups = {"furlenco", "cart", "backlog"}, enabled = false)
    @TmsLink("TC-478")
    @Description("TC-478 Replace similar items in cart (Cart Summary)")
    public void testReplaceSimilarItemsInCartInCartSummary_TC478() {
        // TODO(TC-478): same flow as FurlencoCartItemsTest#verifyReplaceOutOfStockItemWithSimilarProduct but on /checkout/summary. Confirm the summary page offers Replace at all.
        throw new SkipException("TODO: TC-478 not implemented");
    }

    @Test(groups = {"furlenco", "offers", "backlog"}, enabled = false)
    @TmsLink("TC-479")
    @Description("TC-479 Custom offer code (Cart Summary)")
    public void testCustomOfferCodeInCartSummary_TC479() {
        // TODO(TC-479): reach /checkout/summary, type a custom coupon code in the Apply coupon box, assert the result.
        throw new SkipException("TODO: TC-479 not implemented");
    }
}
