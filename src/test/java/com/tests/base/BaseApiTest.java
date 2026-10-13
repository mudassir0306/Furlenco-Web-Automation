package com.tests.base;

import com.framework.api.ApiClient;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseApiTest extends BaseTest {
    protected ApiClient api;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        api = new ApiClient();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (api != null) api.close();
    }
}
