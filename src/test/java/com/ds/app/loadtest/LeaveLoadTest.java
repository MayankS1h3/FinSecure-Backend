package com.ds.app.loadtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent load test: simulates N employees applying for leave simultaneously.
 *
 * HOW TO RUN
 * ----------
 * 1. Start your Spring Boot app normally (with seeded data).
 * 2. Open JConsole: jconsole → attach to your Spring Boot process.
 *    Watch: Threads tab (peak thread count), Memory tab (heap), MBeans → java.lang → Threading.
 * 3. Run this class as a plain Java main (no Spring context needed):
 *       Right-click → Run 'LeaveLoadTest.main()'
 *    Or from terminal:
 *       mvn compile exec:java -Dexec.mainClass="com.ds.app.loadtest.LeaveLoadTest"
 *
 * WHAT TO WATCH IN JCONSOLE
 * --------------------------
 * - Threads → Live threads spike when all requests fire simultaneously
 * - Threads → Peak threads = max threads Tomcat allocated (default pool = 200)
 * - Memory → Heap usage climbs during the burst, then GC reclaims it
 * - MBeans → Catalina → ThreadPool → "http-nio-8080" → currentThreadsBusy
 *   This shows exactly how many Tomcat threads are actually processing requests
 *
 * SCENARIOS (change SCENARIO constant below)
 * -------------------------------------------
 * BURST      — all 20 users fire at exactly the same time (max contention)
 * RAMP       — users added gradually, 1 every 200ms (realistic load curve)
 * SUSTAINED  — repeated bursts every 2s for 30s (sustained pressure)
 */
public class LeaveLoadTest {

    // ── Configuration ──────────────────────────────────────────────────────────
    private static final String BASE_URL       = "http://localhost:8085";
    private static final Scenario SCENARIO     = Scenario.BURST;
    private static final int CONCURRENT_USERS  = 20;   // number of simultaneous leave requests
    private static final int RAMP_DELAY_MS     = 200;  // ms between each user in RAMP mode
    private static final int SUSTAINED_ROUNDS  = 5;    // how many burst rounds in SUSTAINED mode
    private static final int SUSTAINED_PAUSE_MS= 2000; // pause between rounds

    // Leave date — must be a future working day
    private static final String LEAVE_DATE = LocalDate.now().plusDays(7).toString();  // e.g. "2026-05-30"
    private static final String LEAVE_TYPE = "CASUAL";

    enum Scenario { BURST, RAMP, SUSTAINED }

    // ── Employee credentials (matches DataSeeder usernames, all have password "123") ──
    // Add/remove entries to match your actual seeded employees.
    // Format: { "username", "password" }
    private static final String[][] EMPLOYEES = {
            {"mayank",  "123"}, {"harsh",   "123"}, {"neha",    "123"},
            {"arjun",   "123"}, {"pooja",   "123"}, {"viresh",  "123"},
            {"tanya",   "123"}, {"karan",   "123"}, {"sonal",   "123"},
            {"aditya",  "123"}, {"ritika",  "123"}, {"suresh",  "123"},
            {"divya",   "123"}, {"ankur",   "123"}, {"meena",   "123"},
            {"vivek",   "123"}, {"shreya",  "123"}, {"nikhil",  "123"},
            {"anjali",  "123"}, {"sameer",  "123"},
    };

    // ── State ──────────────────────────────────────────────────────────────────
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failCount    = new AtomicInteger(0);
    private static final List<String> errors        = Collections.synchronizedList(new ArrayList<>());

    // ── Entry point ────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  Leave Load Test  |  Scenario: " + SCENARIO);
        System.out.println("  Target: " + BASE_URL);
        System.out.println("  Users:  " + CONCURRENT_USERS + "  |  Leave date: " + LEAVE_DATE);
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Attach JConsole now, then press ENTER to start...");
        //noinspection ResultOfMethodCallIgnored
        System.in.read();

        // Step 1: Log in all employees and get their JWT tokens
        System.out.println("\n[1/3] Logging in " + EMPLOYEES.length + " employees...");
        List<String> tokens = loginAll();
        System.out.println("      Obtained " + tokens.size() + " tokens.\n");

        if (tokens.isEmpty()) {
            System.err.println("ERROR: No tokens obtained. Is the server running?");
            return;
        }

        // Step 2: Run the chosen scenario
        System.out.println("[2/3] Running scenario: " + SCENARIO + " ...\n");
        long startMs = System.currentTimeMillis();

        switch (SCENARIO) {
            case BURST     -> runBurst(tokens);
            case RAMP      -> runRamp(tokens);
            case SUSTAINED -> runSustained(tokens);
        }

        long elapsedMs = System.currentTimeMillis() - startMs;

        // Step 3: Print results
        System.out.println("\n[3/3] Results");
        System.out.println("-".repeat(40));
        System.out.printf("  Total time    : %d ms%n", elapsedMs);
        System.out.printf("  Successful    : %d%n", successCount.get());
        System.out.printf("  Failed        : %d%n", failCount.get());
        System.out.printf("  Throughput    : %.1f req/s%n",
                (double)(successCount.get() + failCount.get()) / (elapsedMs / 1000.0));
        if (!errors.isEmpty()) {
            System.out.println("\n  First 5 errors:");
            errors.stream().limit(5).forEach(e -> System.out.println("    - " + e));
        }
        System.out.println("\nSwitch back to JConsole and check:");
        System.out.println("  - Threads → peak thread count");
        System.out.println("  - Memory  → heap before/after GC");
        System.out.println("  - MBeans  → Catalina/ThreadPool/http-nio-8080/currentThreadsBusy");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Scenarios
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BURST — fire all requests at exactly the same instant using a CountDownLatch.
     * This is the worst-case for your email thread pool: every email fires simultaneously.
     */
    private static void runBurst(List<String> tokens) throws InterruptedException {
        int n = Math.min(CONCURRENT_USERS, tokens.size());
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final String token = tokens.get(i);
            final int idx = i;
            pool.submit(() -> {
                try {
                    startGun.await();  // All threads wait here until the gun fires
                    applyLeave(token, idx);
                } catch (Exception e) {
                    errors.add("Thread " + idx + ": " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        System.out.println("  All " + n + " threads ready. Firing...");
        startGun.countDown();  // Release all threads simultaneously
        done.await(60, TimeUnit.SECONDS);
        pool.shutdown();
    }

    /**
     * RAMP — add one user every RAMP_DELAY_MS ms.
     * Observe how thread count grows linearly in JConsole.
     */
    private static void runRamp(List<String> tokens) throws InterruptedException {
        int n = Math.min(CONCURRENT_USERS, tokens.size());
        ExecutorService pool = Executors.newCachedThreadPool();
        CountDownLatch done = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final String token = tokens.get(i);
            final int idx = i;
            System.out.printf("  Adding user %d/%d (%.1fs elapsed)%n",
                    i + 1, n, i * RAMP_DELAY_MS / 1000.0);
            pool.submit(() -> {
                try {
                    applyLeave(token, idx);
                } catch (Exception e) {
                    errors.add("Thread " + idx + ": " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
            Thread.sleep(RAMP_DELAY_MS);
        }

        done.await(120, TimeUnit.SECONDS);
        pool.shutdown();
    }

    /**
     * SUSTAINED — repeat BURST every SUSTAINED_PAUSE_MS for SUSTAINED_ROUNDS rounds.
     * Watch heap allocation pattern and whether threads are properly released between rounds.
     * NOTE: Each round uses a fresh leave date offset so requests don't duplicate.
     */
    private static void runSustained(List<String> tokens) throws InterruptedException {
        for (int round = 0; round < SUSTAINED_ROUNDS; round++) {
            System.out.printf("  Round %d/%d%n", round + 1, SUSTAINED_ROUNDS);
            successCount.set(0); failCount.set(0);
            runBurst(tokens);
            System.out.printf("    Round %d done — success=%d fail=%d%n",
                    round + 1, successCount.get(), failCount.get());
            if (round < SUSTAINED_ROUNDS - 1) {
                System.out.printf("    Pausing %dms...%n", SUSTAINED_PAUSE_MS);
                Thread.sleep(SUSTAINED_PAUSE_MS);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTTP helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static List<String> loginAll() {
        List<String> tokens = new ArrayList<>();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        for (String[] creds : EMPLOYEES) {
            try {
                String token = login(client, creds[0], creds[1]);
                if (token != null) {
                    tokens.add(token);
                    System.out.printf("  ✓ %s logged in%n", creds[0]);
                } else {
                    System.out.printf("  ✗ %s — no token returned%n", creds[0]);
                }
            } catch (Exception e) {
                System.out.printf("  ✗ %s — %s%n", creds[0], e.getMessage());
            }
        }
        return tokens;
    }

    /**
     * POST /api/auth/login  (adjust path if your auth endpoint differs)
     * Returns the JWT token string.
     */
    private static String login(HttpClient client, String username, String password) throws Exception {
        String body = String.format("""
                {"username":"%s","password":"%s"}
                """, username, password).strip();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/finsecure/public/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.printf("  Login failed for %s: HTTP %d — %s%n",
                    username, response.statusCode(), response.body());
            return null;
        }

        // Parse token — adjust the JSON field name to match your actual login response
        // Common patterns: { "token": "..." } or { "accessToken": "..." } or { "data": { "token": "..." } }
        JsonNode root = mapper.readTree(response.body());
        JsonNode tokenNode = root.path("token");          // try "token" first
        if (tokenNode.isMissingNode()) tokenNode = root.path("accessToken");
        if (tokenNode.isMissingNode()) tokenNode = root.at("/data/token");
        if (tokenNode.isMissingNode()) {
            System.err.printf("  Could not find token field in login response for %s: %s%n",
                    username, response.body());
            return null;
        }
        return tokenNode.asText();
    }

    /**
     * POST /api/leaves/apply  (adjust path if your leave endpoint differs)
     */
    private static void applyLeave(String token, int idx) throws Exception {
        // Use a slightly different date per thread so they don't collide on uniqueness constraints
        String leaveDate = LocalDate.parse(LEAVE_DATE).plusDays(idx % 5).toString();

        String body = String.format("""
                {
                  "startDate": "%s",
                  "endDate": "%s",
                  "leaveType": "%s",
                  "reasonForLeave": "Load test request #%d"
                }
                """, leaveDate, leaveDate, LEAVE_TYPE, idx).strip();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/leaves"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            System.out.printf("  [%3d] ✓ HTTP %d — leave applied for %s%n",
                    idx, response.statusCode(), leaveDate);
            successCount.incrementAndGet();
        } else {
            String msg = String.format("[%d] HTTP %d: %s", idx, response.statusCode(), response.body());
            System.out.printf("  [%3d] ✗ %s%n", idx, msg);
            errors.add(msg);
            failCount.incrementAndGet();
        }
    }
}