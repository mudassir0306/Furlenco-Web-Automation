package com.tests.web.furlenco;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.TmsLink;
import java.util.ArrayList;
import java.util.List;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Cart item scenarios (PDF: Add Multiple Items to Cart with Verification, Replace Similar Items in
 * Cart), verified live on stag.furlenco.com 2026-10-06. Each test uses a fresh guest browser
 * session, so the cart needs no cleanup.
 */
@Epic("Furlenco Web Automation")
@Feature("Cart Items")
public class FurlencoCartItemsTest extends FurlencoCityStockSupport {

    @TmsLink("TC-467")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify several products added from their PDPs all appear in the cart with the right count")
    public void verifyAddMultipleItemsToCart() {
        List<String> products = new ArrayList<>(
                stockPage.open(furlencoUrl, HOME_CITY, CATEGORY).getInStockProductPaths());
        if (products.size() < 2) {
            throw new SkipException("Need at least two in-stock " + CATEGORY + " products in " + HOME_CITY);
        }

        int added = 0;
        for (String product : products) {
            if (added == 2) {
                break;
            }
            if (pdpPage.openIfPurchasable(furlencoUrl, product)) {
                pdpPage.addToCart();
                added++;
            }
        }
        if (added < 2) {
            throw new SkipException("Could not find two purchasable " + CATEGORY + " products in " + HOME_CITY);
        }
        cartPage.open(furlencoUrl);

        assertThat(cartPage.getItemCount()).as("Items in cart").isEqualTo(2);
        assertThat(cartPage.getRentalItemsHeadingCount()).as("Rental items heading").isEqualTo(2);
    }

    @Test(groups = {"regression", "web", "furlenco"}, priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify an out-of-stock cart item can be replaced with a similar in-stock product")
    public void verifyReplaceOutOfStockItemWithSimilarProduct() {
        String product = findProductOnlyInHomeCity();
        stockPage.open(furlencoUrl, HOME_CITY, CATEGORY);
        pdpPage.open(furlencoUrl, product).addToCart();
        cartPage.open(furlencoUrl);
        cartPage.changeCity("Mumbai");
        cartPage.waitForOutOfStockItem();

        cartPage.clickReplace();
        assertThat(cartPage.getReplacementOptionCount()).as("Similar products offered").isGreaterThan(0);
        cartPage.selectReplacement(0);

        assertThat(cartPage.getOutOfStockItemCount()).as("No item left out of stock").isZero();
        assertThat(cartPage.getItemCount()).as("Item count unchanged by a replace").isEqualTo(1);
    }
}
