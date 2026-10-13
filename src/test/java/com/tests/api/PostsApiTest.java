package com.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.APIResponse;
import com.framework.api.ApiClient;
import com.tests.base.BaseApiTest;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PostsApiTest extends BaseApiTest {

    @Test(groups = {"smoke", "api", "public"})
    public void getPostReturnsExpectedFields() {
        APIResponse res = api.get("/posts/1");

        assertThat(res.status()).isEqualTo(200);
        JsonNode body = ApiClient.json(res);
        assertThat(body.get("id").asInt()).isEqualTo(1);
        assertThat(body.has("title")).isTrue();
    }

    @Test(groups = {"regression", "api", "public"})
    public void createPostEchoesPayload() {
        APIResponse res = api.post("/posts", Map.of("title", "hello", "body", "world", "userId", 1));

        assertThat(res.status()).isEqualTo(201);
        JsonNode body = ApiClient.json(res);
        assertThat(body.get("title").asText()).isEqualTo("hello");
        assertThat(body.get("id").asInt()).isPositive();
    }

    @Test(groups = {"regression", "api", "public"})
    public void unknownPostReturns404() {
        assertThat(api.get("/posts/999999").status()).isEqualTo(404);
    }
}
