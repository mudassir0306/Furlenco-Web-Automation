package com.tests.pages.sample;

import com.framework.base.BasePage;
import com.framework.config.ConfigManager;
import com.microsoft.playwright.Page;

public class LoginPage extends BasePage {
    public LoginPage(Page page) {
        super(page);
    }

    public LoginPage open() {
        navigateTo(ConfigManager.getInstance().get("sample.web.base.url", "https://www.saucedemo.com"));
        return this;
    }

    public InventoryPage loginAs(String user, String password) {
        attemptLogin(user, password);
        return new InventoryPage(page);
    }

    public LoginPage attemptLogin(String user, String password) {
        page.locator("[data-test=username]").fill(user);
        page.locator("[data-test=password]").fill(password);
        page.locator("[data-test=login-button]").click();
        return this;
    }

    public String errorMessage() {
        return page.locator("[data-test=error]").innerText();
    }
}
