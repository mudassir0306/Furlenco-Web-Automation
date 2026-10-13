package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import java.util.regex.Pattern;

/**
 * Rent product details page ({@code /rent/products/<slug>}), layout verified live on
 * stag.furlenco.com 2026-10-06. Uses visible-text locators because the page has no stable test ids.
 * The page title is the product name. Much of the page renders after the first paint (the Add to
 * cart button is visible before it works), so checks wait for their content and {@link #open}
 * waits for the delivery estimate, which appears once the page has loaded its data.
 */
public class FurlencoPdpPage extends BasePage {

    private static final int CONTENT_WAIT_MS = 15_000;
    private static final Pattern PRICE = Pattern.compile("₹[0-9,]+/mo");
    private static final Pattern DISCOUNT = Pattern.compile("[0-9]+% OFF");
    private static final Pattern DELIVERY_ESTIMATE = Pattern.compile("Delivery and Assemble by .+");

    public FurlencoPdpPage(Page page) {
        super(page);
    }

    private Locator addToCartButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to cart").setExact(true)).first();
    }

    private boolean waitVisible(Locator locator) {
        for (int i = 0; i < CONTENT_WAIT_MS / 500; i++) {
            Object any = locator.evaluateAll(
                    "els => els.some(e => e.getClientRects().length > 0 && getComputedStyle(e).visibility !== 'hidden')");
            if (Boolean.TRUE.equals(any)) {
                return true;
            }
            if (waitQuietly(locator.locator("visible=true").first(), WaitForSelectorState.VISIBLE, 500)) {
                return true;
            }
        }
        return false;
    }

    private boolean visibleText(String text) {
        return waitVisible(page.getByText(text, new Page.GetByTextOptions().setExact(true)));
    }

    @Step("Open product {path}")
    public FurlencoPdpPage open(String baseUrl, String path) {
        navigateTo(baseUrl + path);
        addToCartButton().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        waitVisible(page.getByText(DELIVERY_ESTIMATE));
        return this;
    }

    /** Opens a product page and reports whether it has a working Add to cart button (combos and sold-out items may not). */
    @Step("Open product {path} if it can be added to the cart")
    public boolean openIfPurchasable(String baseUrl, String path) {
        navigateTo(baseUrl + path);
        try {
            addToCartButton().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15_000));
        } catch (PlaywrightException e) {
            return false;
        }
        waitVisible(page.getByText(DELIVERY_ESTIMATE));
        return addToCartButton().isEnabled();
    }

    @Step("Read the product name")
    public String getProductName() {
        return page.title().trim();
    }

    @Step("Check the product price is displayed")
    public boolean isPriceDisplayed() {
        return waitVisible(page.getByText(PRICE));
    }

    @Step("Check the discount badge is displayed")
    public boolean isDiscountBadgeDisplayed() {
        return waitVisible(page.getByText(DISCOUNT));
    }

    @Step("Check the Zero deposit and Free delivery line is displayed")
    public boolean isZeroDepositDisplayed() {
        return waitVisible(page.getByText(Pattern.compile("Zero deposit")));
    }

    @Step("Check Add to cart is enabled")
    public boolean isAddToCartEnabled() {
        return waitUntilVisibleAndEnabled(addToCartButton(), 20_000);
    }

    @Step("Check the Delivery & Assembly Details section with the pincode row")
    public boolean isDeliverySectionDisplayed() {
        return waitVisible(page.getByText(Pattern.compile("Delivery & Assembly Details")))
                && waitVisible(page.getByText(DELIVERY_ESTIMATE))
                && waitVisible(page.getByText("Change", new Page.GetByTextOptions().setExact(true)));
    }

    @Step("Read the delivery estimate")
    public String getDeliveryEstimate() {
        waitVisible(page.getByText(DELIVERY_ESTIMATE));
        Object text = page.getByText(DELIVERY_ESTIMATE).evaluateAll(
                "els => (els.find(e => e.getClientRects().length > 0) || els[0]).innerText");
        return String.valueOf(text).trim();
    }

    @Step("Check the product information sections are displayed")
    public boolean areInfoSectionsDisplayed() {
        return visibleText("Product Specifications")
                && visibleText("About this Product")
                && visibleText("Care Instructions")
                && visibleText("Frequently Asked Questions");
    }

    @Step("Click Change next to the pincode")
    public FurlencoPdpPage clickChangePincode() {
        page.locator("text='Change' >> visible=true").first().click();
        return this;
    }

    /**
     * Clicks Add to cart and confirms it worked: the button turns into "Go to cart". A click made
     * before the page has hydrated is ignored, so one retry is allowed.
     */
    @Step("Add the product to the cart")
    public FurlencoPdpPage addToCart() {
        Locator goToCart = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^Go to cart$", Pattern.CASE_INSENSITIVE))).first();
        for (int attempt = 1; attempt <= 2; attempt++) {
            addToCartButton().click();
            try {
                goToCart.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(8_000));
                return this;
            } catch (PlaywrightException e) {
                // not hydrated yet; try once more
            }
        }
        throw new IllegalStateException("Add to cart did not register: no 'Go to cart' button appeared");
    }
}
