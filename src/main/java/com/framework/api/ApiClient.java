package com.framework.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.framework.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;

/** Thin wrapper over Playwright's APIRequestContext with JSON helpers. Close after use. */
public class ApiClient implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Playwright playwright;
    private final APIRequestContext request;

    public ApiClient() {
        this(ConfigManager.getInstance().get("sample.api.base.url", "https://jsonplaceholder.typicode.com"), Map.of());
    }

    public ApiClient(String baseUrl, Map<String, String> headers) {
        this.playwright = Playwright.create();
        Map<String, String> all = new HashMap<>();
        all.put("Accept", "application/json");
        all.putAll(headers);
        this.request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(baseUrl)
                .setTimeout(ConfigManager.getInstance().defaultTimeoutMs())
                .setExtraHTTPHeaders(all));
    }

    public APIResponse get(String path) {
        return request.get(path);
    }

    public APIResponse post(String path, Object body) {
        return request.post(path, RequestOptions.create().setData(body));
    }

    public APIResponse put(String path, Object body) {
        return request.put(path, RequestOptions.create().setData(body));
    }

    public APIResponse delete(String path) {
        return request.delete(path);
    }

    public static JsonNode json(APIResponse response) {
        try {
            return MAPPER.readTree(response.body());
        } catch (Exception e) {
            throw new IllegalStateException("Response is not valid JSON: " + response.text(), e);
        }
    }

    public static <T> T as(APIResponse response, Class<T> type) {
        try {
            return MAPPER.readValue(response.body(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot map response to " + type.getSimpleName(), e);
        }
    }

    @Override
    public void close() {
        request.dispose();
        playwright.close();
    }
}
