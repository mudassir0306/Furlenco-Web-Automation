package com.tests.base;

import com.framework.driver.PlaywrightManager;
import com.framework.utils.EnvironmentCheck;
import com.microsoft.playwright.Page;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class BaseWebTest extends BaseTest {

    protected Page page;

    /**
     * Fails fast, with one clear message, when the environment is unreachable (e.g. VPN down) instead of
     * letting every test time out on its first page load. Checked once per JVM; skip with
     * {@code -Dskip.connectivity.check=true}.
     */
    @BeforeClass(alwaysRun = true)
    public void verifyEnvironmentReachable() {
        EnvironmentCheck.ensureReachable(config.get("furlenco.base.url", config.get("web.base.url", "https://www.furlenco.com")));
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpBrowser() {
        PlaywrightManager.initBrowser();
        page = PlaywrightManager.getPage();
        LOGGER.info("Browser session started for thread {}", Thread.currentThread().getId());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownBrowser() {
        PlaywrightManager.tearDown();
        LOGGER.info("Browser session closed for thread {}", Thread.currentThread().getId());
    }
}
