package com.framework.utils;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.Reporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Records every login attempt's test data: an Allure attachment + parameters on the current test,
 * and a row in {@code target/test-data/login-attempts.csv}. The CSV is shared by parallel surefire
 * forks, so writes take a cross-process {@link java.nio.channels.FileLock} (plus an in-JVM monitor,
 * since file locks do not exclude threads of the same JVM).
 */
public final class LoginDataRecorder {

    private static final Logger LOGGER = LogManager.getLogger(LoginDataRecorder.class);
    private static final Path CSV = Paths.get("target", "test-data", "login-attempts.csv");
    private static final String HEADER = "timestamp,testName,kind,phone,otp,name,email,outcome";
    private static final Object LOCK = new Object();

    private LoginDataRecorder() {
    }

    /** Builds a record stamped with now and the current test name. */
    public static LoginTestData newRecord(LoginTestData.Kind kind, String phone, String otp,
                                          String name, String email, String outcome) {
        return new LoginTestData(currentTestName(), kind, phone, otp,
                name == null ? "" : name, email == null ? "" : email,
                Instant.now().toString(), outcome);
    }

    /** Attaches the record to the Allure test and appends it to the CSV. Never throws. */
    public static LoginTestData record(LoginTestData d) {
        try {
            String json = "{\n"
                    + "  \"testName\": " + q(d.testName()) + ",\n"
                    + "  \"kind\": " + q(d.kind().name()) + ",\n"
                    + "  \"phone\": " + q(d.phone()) + ",\n"
                    + "  \"otp\": " + q(d.otp()) + ",\n"
                    + "  \"name\": " + q(d.name()) + ",\n"
                    + "  \"email\": " + q(d.email()) + ",\n"
                    + "  \"timestamp\": " + q(d.timestamp()) + ",\n"
                    + "  \"outcome\": " + q(d.outcome()) + "\n}";
            Allure.addAttachment("Login test data", "application/json", json, ".json");
            Allure.parameter("login.kind", d.kind().name());
            Allure.parameter("login.phone", d.phone());
            Allure.parameter("login.outcome", d.outcome());
        } catch (Throwable t) {
            LOGGER.debug("Could not attach login data to Allure (no test context?): {}", t.getMessage());
        }
        try {
            appendCsv(d);
        } catch (Throwable t) {
            LOGGER.warn("Could not append login data to {}: {}", CSV, t.getMessage());
        }
        LOGGER.info("Login test data: kind={} phone={} outcome={}", d.kind(), d.phone(), d.outcome());
        return d;
    }

    private static void appendCsv(LoginTestData d) throws IOException {
        String row = String.join(",", csv(d.timestamp()), csv(d.testName()), csv(d.kind().name()),
                csv(d.phone()), csv(d.otp()), csv(d.name()), csv(d.email()), csv(d.outcome())) + "\n";
        synchronized (LOCK) {
            Files.createDirectories(CSV.getParent());
            try (var ch = java.nio.channels.FileChannel.open(CSV, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                 var lock = ch.lock()) {
                // Position is at end under APPEND; size()==0 means we are first, so write the header.
                String out = ch.size() == 0 ? HEADER + "\n" + row : row;
                ch.write(java.nio.ByteBuffer.wrap(out.getBytes(StandardCharsets.UTF_8)));
                ch.force(false);
            }
        }
    }

    private static String currentTestName() {
        try {
            ITestResult r = Reporter.getCurrentTestResult();
            if (r != null) {
                return r.getTestClass().getRealClass().getSimpleName() + "." + r.getMethod().getMethodName();
            }
        } catch (Throwable ignored) {
            // fall through to stack-trace lookup
        }
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("com.tests.web.")) {
                return c.substring(c.lastIndexOf('.') + 1) + "." + e.getMethodName();
            }
        }
        return "unknown";
    }

    private static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }

    private static String csv(String s) {
        String v = s == null ? "" : s;
        return v.contains(",") || v.contains("\"") || v.contains("\n")
                ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
