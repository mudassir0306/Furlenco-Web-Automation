package com.tests.pages.furlenco;

import com.framework.base.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The full-page cart at {@code /cart}, verified live on stag.furlenco.com 2026-10-06. Items have an
 * icon button labelled "Remove item". An item that is not available in the selected city shows an
 * "Out of stock" label with Remove and Replace buttons; Replace opens a drawer
 * ({@code [data-slot='drawer-content']}, titled "Replace item") listing similar in-stock products,
 * each with its own Replace button.
 */
public class FurlencoCartPage extends BasePage {

    private static final String REMOVE_ITEM_ICON = "button[aria-label='Remove item']";
    private static final String REPLACE_DRAWER = "[data-slot='drawer-content']";
    private static final Pattern RENTAL_ITEMS = Pattern.compile("Rental items \\((\\d+)\\)");

    public FurlencoCartPage(Page page) {
        super(page);
    }

    private Locator exactButton(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    @Step("Open the cart")
    public FurlencoCartPage open(String baseUrl) {
        navigateTo(baseUrl + "/cart");
        page.locator(REMOVE_ITEM_ICON + ", :text('No products added')").first()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(60_000));
        return this;
    }

    /**
     * Switches the delivery city from the cart header. The header chip shows "City 123456"; the
     * generic home-page city trigger is not visible on this page in Playwright (verified 2026-10-06),
     * so the chip is found by its 6-digit pincode instead. City options in the drawer are buttons.
     */
    @Step("Change the delivery city to {city}")
    public FurlencoCartPage changeCity(String city) {
        page.locator("header >> text=/[0-9]{6}/ >> visible=true").first().click();
        Locator cityButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(city).setExact(true)).first();
        cityButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        cityButton.click();
        waitQuietly(cityButton, WaitForSelectorState.HIDDEN, 5000);
        settle(1500);
        return this;
    }

    @Step("Count cart items")
    public int getItemCount() {
        return page.locator(REMOVE_ITEM_ICON).count();
    }

    @Step("Read the rental items count from the heading")
    public int getRentalItemsHeadingCount() {
        Locator heading = page.getByText(RENTAL_ITEMS).first();
        heading.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Matcher m = RENTAL_ITEMS.matcher(heading.innerText());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    @Step("Count cart items marked Out of stock")
    public int getOutOfStockItemCount() {
        return page.getByText("Out of stock", new Page.GetByTextOptions().setExact(true)).count();
    }

    @Step("Wait until an item shows Out of stock")
    public FurlencoCartPage waitForOutOfStockItem() {
        page.getByText("Out of stock", new Page.GetByTextOptions().setExact(true)).first()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    @Step("Check the item-level Remove button is displayed")
    public boolean isRemoveButtonDisplayed() {
        return exactButton("Remove").first().isVisible();
    }

    @Step("Check the item-level Replace button is displayed")
    public boolean isReplaceButtonDisplayed() {
        return exactButton("Replace").first().isVisible();
    }

    @Step("Open the Replace item drawer")
    public FurlencoCartPage clickReplace() {
        exactButton("Replace").first().click();
        page.locator(REPLACE_DRAWER).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    @Step("Count similar products offered in the Replace drawer")
    public int getReplacementOptionCount() {
        return page.locator(REPLACE_DRAWER).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Replace").setExact(true)).count();
    }

    @Step("Pick replacement option {index}")
    public FurlencoCartPage selectReplacement(int index) {
        page.locator(REPLACE_DRAWER).getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Replace").setExact(true))
                .nth(index).click();
        page.locator(REPLACE_DRAWER).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        settle(1500);
        return this;
    }
}
