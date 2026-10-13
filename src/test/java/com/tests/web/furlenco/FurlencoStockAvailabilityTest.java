package com.tests.web.furlenco;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.TmsLink;
import java.util.Set;
import org.testng.annotations.Test;

/**
 * Per-city stock (PDF scenarios: Verify Out of Stock Status for Selected City and Category, Verify
 * Out of Stock Item in Cart for Different Delivery City). Verified live on stag.furlenco.com
 * 2026-10-06: appliances had different out-of-stock products in Bengaluru and Mumbai, and a cart
 * item that is unavailable after switching city shows "Out of stock" with Remove and Replace.
 * Each test uses a fresh guest browser session, so the cart needs no cleanup.
 */
@Epic("Furlenco Web Automation")
@Feature("City Stock")
public class FurlencoStockAvailabilityTest extends FurlencoCityStockSupport {

    @TmsLink("TC-468")
    @Test(groups = {"smoke", "web", "furlenco"}, priority = 1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify the out-of-stock products in a category differ between cities")
    public void verifyOutOfStockStatusVariesByCity() {
        Set<String> outOfStockHome = stockPage.open(furlencoUrl, HOME_CITY, CATEGORY).getOutOfStockProductPaths();
        assertThat(stockPage.getHeaderLocation()).containsIgnoringCase("Bengaluru");

        Set<String> outOfStockOther = stockPage.open(furlencoUrl, OTHER_CITY, CATEGORY).getOutOfStockProductPaths();
        assertThat(stockPage.getHeaderLocation()).containsIgnoringCase("Mumbai");

        assertThat(outOfStockOther).as("Out-of-stock products shown in " + OTHER_CITY).isNotEmpty();
        assertThat(outOfStockOther).as("Stock should differ between the two cities").isNotEqualTo(outOfStockHome);
    }

    @TmsLink("TC-473")
    @Test(groups = {"regression", "web", "furlenco"}, priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify a cart item unavailable in the new delivery city shows Out of stock with Remove and Replace")
    public void verifyOutOfStockItemInCartForDifferentCity() {
        String product = findProductOnlyInHomeCity();

        stockPage.open(furlencoUrl, HOME_CITY, CATEGORY);
        pdpPage.open(furlencoUrl, product).addToCart();
        cartPage.open(furlencoUrl);
        assertThat(cartPage.getItemCount()).isEqualTo(1);
        assertThat(cartPage.getOutOfStockItemCount()).as("Item is available in " + HOME_CITY).isZero();

        cartPage.changeCity("Mumbai");

        cartPage.waitForOutOfStockItem();
        assertThat(cartPage.getOutOfStockItemCount()).isEqualTo(1);
        assertThat(cartPage.isRemoveButtonDisplayed()).as("Remove offered").isTrue();
        assertThat(cartPage.isReplaceButtonDisplayed()).as("Replace offered").isTrue();
    }
}
