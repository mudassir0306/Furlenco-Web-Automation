package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.framework.utils.WaitUtils;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Page Object for the Product Listing Page (PLP) shared across Rent / Buy / Unlmtd verticals.
 * Selectors are best-effort (see {@link FurlencoLoginPage} javadoc note on unverified selectors).
 */
public class FurlencoPlpPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(FurlencoPlpPage.class);

    private static final String CATEGORY_TABS =
            "[data-slot='category-tabs'] button, [data-slot='category-tabs'] a, nav[aria-label='categories'] a";
    private static final String SUBCATEGORY_CHIPS =
            "[data-slot='subcategory-chips'] button, [data-slot='filters'] button, [data-slot='subcategory'] a";
    private static final String PRODUCT_CARDS =
            "a[href*='/product/'], a[href*='/products/'], a[href*='/package/'], div[data-testid*='product'], main a:has(img)";

    public FurlencoPlpPage(Page page) {
        super(page);
    }

    @Step("Check if PLP is loaded")
    public boolean isLoaded() {
        boolean urlMatches = currentUrl().contains("/listing") || currentUrl().contains("/rent")
                || currentUrl().contains("/buy") || currentUrl().contains("/unlmtd");
        boolean hasProducts = getProductCount() > 0;
        LOGGER.info("PLP loaded check: urlMatches={}, hasProducts={}", urlMatches, hasProducts);
        return urlMatches && hasProducts;
    }

    @Step("Get count of visible products on PLP")
    public int getProductCount() {
        return page.locator(PRODUCT_CARDS).count();
    }

    @Step("Get count of category tabs available")
    public int getCategoryCount() {
        return page.locator(CATEGORY_TABS).count();
    }

    @Step("Select category: {categoryName}")
    public FurlencoPlpPage selectCategory(String categoryName) {
        LOGGER.info("Selecting PLP category: {}", categoryName);
        Locator category = page.locator(CATEGORY_TABS).filter(new Locator.FilterOptions().setHasText(categoryName)).first();
        category.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        category.click(new Locator.ClickOptions().setForce(true));
        page.waitForLoadState();
        settle(1500);
        return this;
    }

    @Step("Get count of subcategory filter chips available")
    public int getSubcategoryCount() {
        return page.locator(SUBCATEGORY_CHIPS).count();
    }

    @Step("Select subcategory: {subcategoryName}")
    public FurlencoPlpPage selectSubcategory(String subcategoryName) {
        LOGGER.info("Selecting PLP subcategory: {}", subcategoryName);
        Locator subcategory = page.locator(SUBCATEGORY_CHIPS)
                .filter(new Locator.FilterOptions().setHasText(subcategoryName)).first();
        subcategory.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        subcategory.click(new Locator.ClickOptions().setForce(true));
        page.waitForLoadState();
        settle(1500);
        return this;
    }

    @Step("Click first product card on PLP")
    public FurlencoProductPage clickFirstProduct() {
        LOGGER.info("Clicking first product card on PLP");
        Locator firstProduct = page.locator(PRODUCT_CARDS).first();
        firstProduct.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        firstProduct.click();
        page.waitForLoadState();
        return new FurlencoProductPage(page);
    }

    /**
     * Opens product URLs in order until one has an enabled Add to Cart button, up to {@code
     * maxAttempts}.
     * <p>
     * <b>Verified live root cause this works around:</b> clicking a PLP product card
     * ({@code Locator.click()}) reliably fails to navigate at all on this app — the page silently
     * stays on the PLP's own URL, confirmed by logging {@code page.url()} immediately after the
     * click. Every earlier "Add to Cart is disabled" failure traced back to this: the code was
     * checking for an Add to Cart button while still sitting on the listing page. A direct page
     * navigation to the product's own URL (extracted from the card's real {@code href} via the
     * DOM, not the possibly-relative attribute) is what every successful manual verification of
     * this app used, and is what actually reaches the product page reliably.
     */
    @Step("Click first available (purchasable) product card on PLP")
    public FurlencoProductPage clickFirstAvailableProduct(int maxAttempts) {
        java.util.List<String> candidates = getCandidateProductUrls(maxAttempts);
        int count = candidates.size();
        for (int i = 0; i < count; i++) {
            String href = candidates.get(i);
            LOGGER.info("Trying product URL (index {}): {}", i, href);
            FurlencoProductPage productPage = openProduct(href);
            if (productPage.isAddToCartButtonEnabled()) {
                return productPage;
            }
            LOGGER.info("Product at index {} has Add to Cart disabled, trying next", i);
        }
        throw new IllegalStateException(
                "No purchasable product (enabled Add to Cart) found in the first " + maxAttempts + " cards on this PLP.");
    }


    /**
     * Opens a product page by URL. Waits for DOMContentLoaded (not the full {@code load} event, which
     * third-party scripts can delay past 30s on staging); callers wait for the Add to Cart button anyway.
     */
    @Step("Open product page by URL")
    public FurlencoProductPage openProduct(String url) {
        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                .setTimeout(90_000));
        return new FurlencoProductPage(page);
    }
    /**
     * Collects up to {@code max} candidate product URLs from the PLP BEFORE navigating anywhere: a
     * lazy Locator keeps re-resolving against whatever page is current, so reusing it after
     * {@code page.navigate()} looked for the next card on the product page and timed out (30s)
     * whenever product 0 was unusable. Callers navigate through the returned list themselves.
     */
    /** The home page also carries /products/ links in its carousels, so require the listing route too. */
    private static boolean isListingUrl(String url) {
        return url.contains("/listing") || url.contains("collectionType");
    }

    @Step("Collect candidate product URLs from the PLP (max {max})")
    public java.util.List<String> getCandidateProductUrls(int max) {
        // Wait for REAL product links, not just any card: the broad PRODUCT_CARDS selector also matches
        // banners on the page we came from, so waiting on it returned before the client-side route to
        // the listing had happened and the hrefs were collected from an empty/loading page (0 candidates).
        String productLinks = "a[href*='/product/'], a[href*='/products/'], a[href*='/package/']";
        try {
            WaitUtils.pollUntil("product links present on the PLP",
                    () -> isListingUrl(page.url()) && page.locator(productLinks).count() > 0,
                    java.time.Duration.ofSeconds(60), java.time.Duration.ofMillis(300));
        } catch (IllegalStateException e) {
            LOGGER.warn("No /product(s)/ links appeared within 60s; falling back to the broad card selector");
            page.locator(PRODUCT_CARDS).first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
        }
        settle(1500); // let the listing finish rendering/lazy-loading more cards
        @SuppressWarnings("unchecked")
        java.util.List<Object> rawHrefs = (java.util.List<Object>) page.locator(PRODUCT_CARDS)
                .evaluateAll("els => els.map(e => e.href || '')");
        java.util.LinkedHashSet<String> productHrefs = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<String> otherHrefs = new java.util.LinkedHashSet<>();
        for (Object h : rawHrefs) {
            String href = h == null ? "" : h.toString();
            if (href.isBlank()) {
                continue;
            }
            // Prefer real product/package URLs; the broad `main a:has(img)` selector can also match banners.
            if (href.contains("/product/") || href.contains("/products/") || href.contains("/package/")) {
                productHrefs.add(href);
            } else {
                otherHrefs.add(href);
            }
        }
        java.util.List<String> candidates = new java.util.ArrayList<>(productHrefs.isEmpty() ? otherHrefs : productHrefs);
        int count = Math.min(max, candidates.size());
        LOGGER.info("PLP offered {} candidate product URLs, using up to {}", candidates.size(), count);
        return new java.util.ArrayList<>(candidates.subList(0, count));
    }
}
