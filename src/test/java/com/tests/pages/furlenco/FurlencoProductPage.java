package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Page Object for Furlenco Product Details Page (PDP).
 */
public class FurlencoProductPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(FurlencoProductPage.class);

    private static final String PRODUCT_TITLE = "h1, h2, [data-testid='product-title']";
    private static final String ADD_TO_CART_BTN = "button:has-text('Add to Cart'), button:has-text('ADD TO CART'), button:has-text('Rent Now'), button:has-text('Buy Now')";
    private static final String PRICE_TEXT = "[data-testid*='price'], p:has-text('₹'), span:has-text('₹')";

    public FurlencoProductPage(Page page) {
        super(page);
    }

    @Step("Check if Product Details Page is loaded")
    public boolean isLoaded() {
        return isVisible(PRODUCT_TITLE) || currentUrl().contains("/product");
    }

    @Step("Get product title text")
    public String getProductTitle() {
        Locator titleLocator = page.locator(PRODUCT_TITLE).first();
        titleLocator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        String title = titleLocator.textContent().trim();
        LOGGER.info("Product title is: {}", title);
        return title;
    }

    @Step("Check if Add to Cart / Rent CTA button is visible")
    public boolean isAddToCartButtonVisible() {
        return page.locator(ADD_TO_CART_BTN).first().isVisible();
    }

    /**
     * Checks if Add to Cart is enabled, polling briefly first — verified live that the button can
     * render disabled momentarily while price/availability data is still loading, then become
     * enabled a moment later, so a single instant check can false-negative.
     */
    @Step("Check if Add to Cart / Rent CTA button is enabled (in stock / no variant required)")
    public boolean isAddToCartButtonEnabled() {
        Locator btn = page.locator(ADD_TO_CART_BTN).first();
        waitQuietly(btn, WaitForSelectorState.ATTACHED, 20_000); // the CTA may not be rendered yet on a slow page
        if (btn.count() == 0) {
            return false;
        }
        // Verified live: after a full page load/reload, this app's client-side data fetch
        // (price/availability) can take noticeably longer than a few seconds before the button
        // flips from its initial disabled state to enabled — a short poll window previously
        // false-negatived here. 20s total, polled every second.
        return waitUntilVisibleAndEnabled(btn, 20_000);
    }

    @Step("Click Add to Cart / Rent CTA button")
    public void clickAddToCart() {
        LOGGER.info("Clicking Add to Cart / Rent button");
        Locator btn = page.locator(ADD_TO_CART_BTN).first();
        btn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        btn.click();
    }

    /**
     * After Add to Cart the PDP shows a "Go to cart" button (verified from a failure page-text dump,
     * 2026-10-15); clicking it is more reliable than the header cart icon, which with force=true
     * sometimes left the cart closed. Falls back to the header icon if the button isn't there.
     * Waits (up to 30s) for cart contents so callers get a loaded cart.
     */
    @Step("Go to cart from the product page")
    public FurlencoCartDrawer goToCart() {
        Locator goToCart = page.locator("button:has-text('Go to cart'), a:has-text('Go to cart')").first();
        if (waitQuietly(goToCart, WaitForSelectorState.VISIBLE, 15_000)) {
            try {
                goToCart.click(new Locator.ClickOptions().setTimeout(15_000));
            } catch (com.microsoft.playwright.TimeoutError e) {
                // The click can be blocked (overlay/animation); the cart is a normal page, so go there directly.
                LOGGER.info("'Go to cart' click timed out; navigating to /cart directly");
                page.navigate(java.net.URI.create(page.url()).resolve("/cart").toString());
            }
        } else {
            LOGGER.info("'Go to cart' not shown; using the header cart icon");
            page.locator("button[aria-label='Cart']").first().click(new Locator.ClickOptions().setForce(true));
        }
        waitQuietly(page.locator(":text(\"Cost Breakup\"), button:has-text('CHECKOUT'), #empty-cart, :text(\"Rent Cart\")").first(),
                WaitForSelectorState.VISIBLE, 30_000);
        return new FurlencoCartDrawer(page);
    }

    @Step("Check if product pricing is displayed")
    public boolean isPriceDisplayed() {
        return page.locator(PRICE_TEXT).first().isVisible();
    }
}
