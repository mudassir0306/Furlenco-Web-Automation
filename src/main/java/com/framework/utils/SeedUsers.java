package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.SkipException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pre-existing staging test accounts (with orders, saved addresses, cancellable orders), loaded
 * from a <b>local, gitignored</b> file ({@code src/test/resources/seed/seed-users.local.properties})
 * or, per pool, from an environment variable (e.g. a CI secret). Numbers must never be committed;
 * see {@code seed-users.example.properties} and the README ("Seed test accounts").
 * <p>
 * Read-only pools ({@link #pick(Pool)}) are handed out round-robin. Consumable pools
 * ({@link #acquireCancelCandidate(Pool)}) reserve a number in a shared consumed file under a file
 * lock, so parallel forks never double-pick and a consumed account is never reused.
 * <p>
 * Phone numbers are never logged; {@link SeedUser#toString()} is the label only.
 */
public final class SeedUsers {

    private static final Logger LOGGER = LogManager.getLogger(SeedUsers.class);
    private static final String RESOURCE = "seed/seed-users.local.properties";
    private static final Path LOCAL_FILE = Paths.get("src", "test", "resources", "seed", "seed-users.local.properties");
    private static final Path CONSUMED_FILE = Paths.get("src", "test", "resources", "seed", "seed-consumed.local.consumed");
    private static final Object LOCK = new Object();
    private static final AtomicInteger ROUND_ROBIN = new AtomicInteger();
    private static volatile Properties fileProps;

    /** Seed pools: property key, environment variable and label fragment. */
    public enum Pool {
        HISTORY_RENTBUY("seed.history.rentbuy", "SEED_HISTORY_RENTBUY", "history-rentbuy"),
        HISTORY_RENT("seed.history.rent", "SEED_HISTORY_RENT", "history-rent"),
        ADDRESS("seed.address", "SEED_ADDRESS", "address"),
        CANCEL_BUY("seed.cancel.buy", "SEED_CANCEL_BUY", "cancel-buy"),
        CANCEL_UNLIMITED("seed.cancel.unlimited", "SEED_CANCEL_UNLIMITED", "cancel-unlimited");

        final String key;
        final String envVar;
        final String labelPart;

        Pool(String key, String envVar, String labelPart) {
            this.key = key;
            this.envVar = envVar;
            this.labelPart = labelPart;
        }
    }

    /** One seed account: a label safe to log/report (e.g. {@code seed-cancel-buy-2}) and its phone. */
    public record SeedUser(String label, String phone) {
        @Override
        public String toString() {
            return label;
        }
    }

    private SeedUsers() {
    }

    /** True if at least one number is configured for the pool. */
    public static boolean isConfigured(Pool pool) {
        return !load(pool).isEmpty();
    }

    /** Round-robin pick from a read-only pool, or empty if the pool has no numbers. */
    public static Optional<SeedUser> pick(Pool pool) {
        List<SeedUser> all = load(pool);
        if (all.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(all.get(Math.floorMod(ROUND_ROBIN.getAndIncrement(), all.size())));
    }

    /** Like {@link #pick(Pool)} but throws a {@link SkipException} with a clear reason when unconfigured. */
    public static SeedUser require(Pool pool) {
        return pick(pool).orElseThrow(() -> new SkipException(
                "No seed accounts configured for pool '" + pool.labelPart + "' (set " + pool.key + " in "
                        + "seed-users.local.properties or env " + pool.envVar + "); see README 'Seed test accounts'"));
    }

    /**
     * Reserves the first not-yet-consumed account of the pool and records it as consumed, atomically
     * across threads and processes. Empty when the pool is empty or exhausted.
     */
    public static Optional<SeedUser> acquireCancelCandidate(Pool pool) {
        List<SeedUser> all = load(pool);
        if (all.isEmpty()) {
            return Optional.empty();
        }
        synchronized (LOCK) {
            try (FileChannel ch = openConsumed(); var lock = ch.lock()) {
                Set<String> consumed = readConsumed(ch);
                for (SeedUser u : all) {
                    if (!consumed.contains(u.phone())) {
                        append(ch, u.phone());
                        LOGGER.info("Reserved seed account {} for a destructive test", u.label());
                        return Optional.of(u);
                    }
                }
                LOGGER.warn("Seed pool '{}' is exhausted ({} accounts consumed)", pool.labelPart, all.size());
                return Optional.empty();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not access consumed-seed file", e);
            }
        }
    }

    /** Records the account as consumed (idempotent; {@link #acquireCancelCandidate} already does so). */
    public static void markConsumed(SeedUser user) {
        synchronized (LOCK) {
            try (FileChannel ch = openConsumed(); var lock = ch.lock()) {
                if (!readConsumed(ch).contains(user.phone())) {
                    append(ch, user.phone());
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not access consumed-seed file", e);
            }
        }
    }

    /** Gives back a reserved account whose destructive step never ran (e.g. login failed). */
    public static void release(SeedUser user) {
        synchronized (LOCK) {
            try (FileChannel ch = openConsumed(); var lock = ch.lock()) {
                Set<String> consumed = readConsumed(ch);
                if (consumed.remove(user.phone())) {
                    ch.truncate(0);
                    ch.position(0);
                    for (String p : consumed) {
                        ch.write(ByteBuffer.wrap((p + "\n").getBytes(StandardCharsets.UTF_8)));
                    }
                    ch.force(false);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not access consumed-seed file", e);
            }
        }
    }

    // ---- internals ----

    private static List<SeedUser> load(Pool pool) {
        String raw = props().getProperty(pool.key);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(pool.envVar);
        }
        List<SeedUser> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String phone = part.trim();
            if (phone.matches("\\d{10}") && seen.add(phone)) {
                out.add(new SeedUser("seed-" + pool.labelPart + "-" + seen.size(), phone));
            }
        }
        return out;
    }

    private static Properties props() {
        Properties p = fileProps;
        if (p == null) {
            synchronized (LOCK) {
                if (fileProps == null) {
                    Properties loaded = new Properties();
                    try (InputStream in = SeedUsers.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                        if (in != null) {
                            loaded.load(in);
                        } else if (Files.exists(LOCAL_FILE)) {
                            try (InputStream f = Files.newInputStream(LOCAL_FILE)) {
                                loaded.load(f);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Could not read seed-users.local.properties: {}", e.getMessage());
                    }
                    fileProps = loaded;
                }
                p = fileProps;
            }
        }
        return p;
    }

    private static FileChannel openConsumed() throws IOException {
        Files.createDirectories(CONSUMED_FILE.getParent());
        return FileChannel.open(CONSUMED_FILE, StandardOpenOption.CREATE, StandardOpenOption.READ,
                StandardOpenOption.WRITE);
    }

    private static Set<String> readConsumed(FileChannel ch) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate((int) ch.size());
        ch.position(0);
        while (buf.hasRemaining() && ch.read(buf) > 0) {
            // keep reading
        }
        Set<String> set = new HashSet<>();
        for (String line : new String(buf.array(), StandardCharsets.UTF_8).split("\\R")) {
            if (!line.isBlank()) {
                set.add(line.trim());
            }
        }
        return set;
    }

    private static void append(FileChannel ch, String phone) throws IOException {
        ch.position(ch.size());
        ch.write(ByteBuffer.wrap((phone + "\n").getBytes(StandardCharsets.UTF_8)));
        ch.force(false);
    }
}
