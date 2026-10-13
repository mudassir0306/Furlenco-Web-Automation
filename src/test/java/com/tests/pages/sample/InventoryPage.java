package com.tests.pages.sample;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class InventoryPage extends BasePage {
    public InventoryPage(Page page) {
        super(page);
    }

    public boolean isLoaded() {
        return page.locator(".inventory_list").isVisible();
    }

    public int productCount() {
        return page.locator(".inventory_item").count();
    }

    public InventoryPage addToCart(String productName) {
        page.locator(".inventory_item")
                .filter(new Locator.FilterOptions().setHasText(productName))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add to cart"))
                .click();
        return this;
    }

    public String cartBadgeCount() {
        return page.locator(".shopping_cart_badge").innerText();
    }
}
