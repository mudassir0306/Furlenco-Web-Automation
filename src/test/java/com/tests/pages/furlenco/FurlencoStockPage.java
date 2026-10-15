package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rent category listing page by city, e.g. {@code /bengaluru/appliances-on-rent?collectionType=CATEGORY_RENT}
 * (verified live on stag.furlenco.com 2026-10-06). Opening the city URL also switches the delivery
 * location to that city. Product cards are links to {@code /rent/products/...}; a card whose text
 * contains "Out of stock" is unavailable in the selected city. Stock differs per city, so tests
 * discover products from the page instead of hardcoding product URLs.
 */
public class FurlencoStockPage extends BasePage {

    private static final String PRODUCT_LINKS = "a[href*='/rent/products/']";

    public FurlencoStockPage(Page page) {
        super(page);
    }

    @Step("Open the {categorySlug} rent listing for {city}")
    public FurlencoStockPage open(String baseUrl, String city, String categorySlug) {
        navigateTo(baseUrl + "/" + city + "/" + categorySlug + "-on-rent?collectionType=CATEGORY_RENT");
        page.locator(PRODUCT_LINKS).first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        settle(1500);
        return this;
    }

    @Step("Read the header location chip")
    public String getHeaderLocation() {
        return page.locator("header").first().innerText().trim().split("\\R")[0].trim();
    }

    @Step("Collect product paths marked Out of stock")
    public Set<String> getOutOfStockProductPaths() {
        return pathsWhere(true);
    }

    @Step("Collect product paths that are in stock")
    public Set<String> getInStockProductPaths() {
        return pathsWhere(false);
    }

    private Set<String> pathsWhere(boolean outOfStock) {
        Object result = page.locator(PRODUCT_LINKS).evaluateAll(
                "(els, oos) => els.filter(e => /Out of stock/i.test(e.innerText) === oos).map(e => e.getAttribute('href'))",
                outOfStock);
        Set<String> paths = new LinkedHashSet<>();
        for (Object o : (List<?>) result) {
            paths.add(String.valueOf(o));
        }
        return paths;
    }
}
