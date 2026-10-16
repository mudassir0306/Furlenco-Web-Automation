package com.framework.base;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.ElementState;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Common actions for all Page Objects. No test/assertion logic belongs here — only
 * page interactions. Every wait is explicit (Playwright auto-waiting + our own visible-state
 * checks) — Thread.sleep is never used anywhere in this framework.
 */
public abstract class BasePage {

    private static final Logger LOGGER = LogManager.getLogger(BasePage.class);

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    @Step("Navigate to {url}")
    public void navigateTo(String url) {
        LOGGER.info("Navigating to {}", url);
        page.navigate(url);
    }

    @Step("Click element: {selector}")
    protected void click(String selector) {
        Locator locator = page.locator(selector);
        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        LOGGER.debug("Clicking [{}]", selector);
        locator.click();
    }

    @Step("Fill '{value}' into: {selector}")
    protected void fill(String selector, String value) {
        Locator locator = page.locator(selector);
        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        LOGGER.debug("Filling [{}] with masked value (len={})", selector, value.length());
        locator.fill(value);
    }

    @Step("Get text of: {selector}")
    protected String getText(String selector) {
        Locator locator = page.locator(selector);
        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return locator.textContent().trim();
    }

    protected boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }

    @Step("Wait for element to be visible: {selector}")
    protected void waitForVisible(String selector) {
        page.locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    protected void waitForHidden(String selector) {
        page.locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // ---- Bounded, never-throwing waits (replace fixed sleeps; a timeout just means "carry on") ----

    /**
     * Returns the first VISIBLE match of {@code selector}, waiting up to {@code timeoutMs}. Plain {@code .first()}
     * often resolves to a hidden duplicate (responsive/mobile copies, closed panels) that never becomes visible,
     * so the wait times out even though a visible match exists. Throws {@link TimeoutError} when none appears.
     */
    protected Locator waitForFirstVisible(String selector, double timeoutMs) {
        Locator[] found = new Locator[1];
        try {
            com.framework.utils.WaitUtils.pollUntil("a visible match for " + selector, () -> {
                for (Locator candidate : page.locator(selector).all()) {
                    if (candidate.isVisible()) {
                        found[0] = candidate;
                        return true;
                    }
                }
                return false;
            }, java.time.Duration.ofMillis((long) timeoutMs), java.time.Duration.ofMillis(250));
        } catch (IllegalStateException e) {
            throw new TimeoutError("No visible element for " + selector + " within " + (long) timeoutMs + "ms");
        }
        return found[0];
    }

    /** Waits up to {@code timeoutMs} for the locator to reach {@code state}; returns false (no throw) on timeout. */
    protected boolean waitQuietly(Locator locator, WaitForSelectorState state, double timeoutMs) {
        try {
            locator.waitFor(new Locator.WaitForOptions().setState(state).setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError e) {
            LOGGER.debug("Bounded wait for {} timed out after {}ms (continuing)", state, timeoutMs);
            return false;
        }
    }

    /** Waits up to {@code timeoutMs} for the URL to match the glob; returns false (no throw) on timeout. */
    protected boolean waitForUrlQuietly(String urlGlob, double timeoutMs) {
        try {
            page.waitForURL(urlGlob, new Page.WaitForURLOptions().setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError e) {
            LOGGER.debug("Bounded wait for URL {} timed out after {}ms (continuing)", urlGlob, timeoutMs);
            return false;
        }
    }

    /** Waits up to {@code timeoutMs} for the URL to differ from {@code previousUrl}; false (no throw) on timeout. */
    protected boolean waitForUrlChangeQuietly(String previousUrl, double timeoutMs) {
        try {
            page.waitForURL(u -> !u.equals(previousUrl), new Page.WaitForURLOptions().setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError e) {
            LOGGER.debug("URL still {} after {}ms (continuing)", previousUrl, timeoutMs);
            return false;
        }
    }

    /** Lets in-flight network activity settle for at most {@code timeoutMs}; never throws on timeout. */
    protected void settle(double timeoutMs) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(timeoutMs));
        } catch (TimeoutError e) {
            LOGGER.debug("Network not idle after {}ms (continuing)", timeoutMs);
        }
    }

    /** Waits up to {@code timeoutMs} for the locator to be visible and enabled; false on timeout. */
    protected boolean waitUntilVisibleAndEnabled(Locator locator, double timeoutMs) {
        try {
            locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeoutMs));
            ElementHandle handle = locator.elementHandle(new Locator.ElementHandleOptions().setTimeout(timeoutMs));
            handle.waitForElementState(ElementState.ENABLED, new ElementHandle.WaitForElementStateOptions().setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError e) {
            return false;
        }
    }

    @Step("Navigate back in browser history")
    public void goBack() {
        LOGGER.info("Navigating back");
        page.goBack();
        page.waitForLoadState();
    }

    public String currentUrl() {
        return page.url();
    }

    public String currentTitle() {
        return page.title();
    }
}
