package com.tests.web.furlenco;

import com.tests.base.BaseWebTest;
import com.tests.pages.furlenco.FurlencoCartPage;
import com.tests.pages.furlenco.FurlencoExperiments;
import com.tests.pages.furlenco.FurlencoHomePage;
import com.tests.pages.furlenco.FurlencoPdpPage;
import com.tests.pages.furlenco.FurlencoStockPage;
import java.util.LinkedHashSet;
import java.util.Set;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

/**
 * Shared setup for tests that depend on per-city stock. Stock changes, so products are discovered
 * from the live listing instead of hardcoded; if no suitable product exists the test is skipped
 * (not failed) because the data, not the app, is what is missing.
 */
public abstract class FurlencoCityStockSupport extends BaseWebTest {

    protected static final String HOME_CITY = "bengaluru";
    protected static final String OTHER_CITY = "mumbai";
    protected static final String CATEGORY = "appliances";

    protected String furlencoUrl;
    protected FurlencoHomePage homePage;
    protected FurlencoStockPage stockPage;
    protected FurlencoPdpPage pdpPage;
    protected FurlencoCartPage cartPage;

    @BeforeMethod(alwaysRun = true)
    public void initStockPages() {
        furlencoUrl = config.get("furlenco.base.url", "https://www.furlenco.com");
        // These tests open URLs directly, so pin the A/B cookie here (before any navigation) when enabled.
        FurlencoExperiments.pinIfEnabled(page.context(), furlencoUrl);
        homePage = new FurlencoHomePage(page);
        stockPage = new FurlencoStockPage(page);
        pdpPage = new FurlencoPdpPage(page);
        cartPage = new FurlencoCartPage(page);
    }

    /** A product in stock in {@code HOME_CITY} but out of stock in {@code OTHER_CITY}. */
    protected String findProductOnlyInHomeCity() {
        Set<String> inStockHome = new LinkedHashSet<>(
                stockPage.open(furlencoUrl, HOME_CITY, CATEGORY).getInStockProductPaths());
        Set<String> outOfStockOther = stockPage.open(furlencoUrl, OTHER_CITY, CATEGORY).getOutOfStockProductPaths();
        inStockHome.retainAll(outOfStockOther);
        if (inStockHome.isEmpty()) {
            throw new SkipException("No " + CATEGORY + " product is in stock in " + HOME_CITY
                    + " and out of stock in " + OTHER_CITY + " right now");
        }
        return inStockHome.iterator().next();
    }
}
