package com.framework.utils;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fails fast when the target environment is unreachable (typically: staging while the company VPN is
 * down), so a run produces one clear message instead of dozens of slow, confusing browser failures.
 * <p>
 * The result is cached per JVM (each Surefire fork checks once). Disable with
 * {@code -Dskip.connectivity.check=true}.
 */
public final class EnvironmentCheck {

    private static final Logger LOGGER = LogManager.getLogger(EnvironmentCheck.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private static volatile boolean verified;

    private EnvironmentCheck() {
    }

    /** Throws {@link IllegalStateException} with a readable cause when {@code baseUrl} cannot be reached. */
    public static void ensureReachable(String baseUrl) {
        if (verified || Boolean.getBoolean("skip.connectivity.check")) {
            return;
        }
        synchronized (EnvironmentCheck.class) {
            if (verified) {
                return;
            }
            String[] lastProblem = {"unknown"};
            try {
                WaitUtils.pollUntil("environment reachable: " + baseUrl, () -> {
                    lastProblem[0] = String.valueOf(probe(baseUrl));
                    return "null".equals(lastProblem[0]);
                }, Duration.ofSeconds(12), Duration.ofSeconds(5));
                verified = true;
                LOGGER.info("Environment reachable: {}", baseUrl);
                return;
            } catch (IllegalStateException e) {
                LOGGER.warn("Connectivity check failed for {}: {}", baseUrl, lastProblem[0]);
            }
            String host = URI.create(baseUrl).getHost();
            String message = "Environment unreachable: " + baseUrl + " (" + lastProblem[0] + "). "
                    + "Staging/preprod need the company VPN; check that it is connected and that '" + host
                    + "' resolves. Skip this check with -Dskip.connectivity.check=true.";
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
    }

    /** Returns null when reachable, otherwise a short description of what failed. */
    private static String probe(String baseUrl) {
        String host;
        try {
            host = URI.create(baseUrl).getHost();
        } catch (IllegalArgumentException e) {
            return "invalid URL";
        }
        if (host == null) {
            return "no host in URL";
        }
        try {
            InetAddress.getByName(host);
        } catch (Exception e) {
            return "DNS lookup failed";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(HTTP_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl)).timeout(HTTP_TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            // stag.furlenco.com resolves to a private address. Off the VPN (e.g. a GitHub-hosted runner) Cloudflare
            // answers with an HTML error page ("Error 1002: DNS points to local or disallowed IP"), so a plain
            // "did we get an HTTP answer" test would wrongly pass. Recognise that page explicitly.
            if (body.contains("DNS points to local or disallowed IP") || body.contains("Error 1002")) {
                return "Cloudflare Error 1002 (the host is private: you are not on the VPN / network)";
            }
            // A healthy site root answers 2xx/3xx; 4xx/5xx on the root means the environment is not usable.
            return response.statusCode() >= 400 ? "HTTP " + response.statusCode() : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
        } catch (Exception e) {
            return "HTTP request failed: " + e.getClass().getSimpleName();
        }
    }
}
