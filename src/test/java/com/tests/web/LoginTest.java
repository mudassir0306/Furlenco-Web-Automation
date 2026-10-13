package com.tests.web;

import com.tests.pages.sample.InventoryPage;
import com.tests.pages.sample.LoginPage;
import com.tests.base.BaseWebTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginTest extends BaseWebTest {

    @Test(groups = {"smoke", "web", "public"})
    public void validUserCanLogin() {
        InventoryPage inventory = new LoginPage(page).open()
                .loginAs(config.get("sample.web.username", "standard_user"), config.get("sample.web.password", "secret_sauce"));

        assertThat(inventory.isLoaded()).isTrue();
        assertThat(inventory.productCount()).isGreaterThan(0);
    }

    @Test(groups = {"regression", "web", "public"})
    public void lockedOutUserSeesError() {
        String error = new LoginPage(page).open()
                .attemptLogin("locked_out_user", config.get("sample.web.password", "secret_sauce"))
                .errorMessage();

        assertThat(error).contains("locked out");
    }

    @Test(groups = {"regression", "web", "public"})
    public void userCanAddItemToCart() {
        InventoryPage inventory = new LoginPage(page).open()
                .loginAs(config.get("sample.web.username", "standard_user"), config.get("sample.web.password", "secret_sauce"))
                .addToCart("Sauce Labs Backpack");

        assertThat(inventory.cartBadgeCount()).isEqualTo("1");
    }
}
