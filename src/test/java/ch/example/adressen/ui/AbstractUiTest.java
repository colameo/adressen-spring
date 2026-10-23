package ch.example.adressen.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.impl.driver.Driver;
import com.microsoft.playwright.options.LoadState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Base of the browser tests: the application runs on a random port and a headless Chromium opens
 * its pages. Tagged "ui": {@code mvn test} skips them, {@code mvn test -Pui} runs them (the first
 * run downloads the browser).
 */
@Tag("ui")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractUiTest {

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    Page page;

    // Playwright would install Chromium, Firefox and WebKit by itself; the tests only need Chromium.
    private static final Map<String, String> SKIP_DEFAULT_BROWSERS = Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");

    @BeforeAll
    static void launchBrowser() throws Exception {
        ProcessBuilder install = Driver.ensureDriverInstalled(SKIP_DEFAULT_BROWSERS, false).createProcessBuilder();
        install.command().addAll(List.of("install", "chromium"));
        if (install.inheritIO().start().waitFor() != 0) {
            throw new IllegalStateException("Could not install the Chromium browser for the UI tests");
        }
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(SKIP_DEFAULT_BROWSERS));
        browser = playwright.chromium().launch();
        // The first page of a cold application can take a while; a slow machine should not fail a test.
        PlaywrightAssertions.setDefaultAssertionTimeout(15_000);
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        page = browser.newPage();
        page.setDefaultTimeout(10_000);
        signIn("tester", "tester");
    }

    @AfterEach
    void closePage() {
        page.close();
    }

    /** Signs in through the login page (the test users are in src/test/resources/security-test.yml). */
    void signIn(String user, String password) {
        page.navigate("http://localhost:" + port + "/login.html");
        page.locator("[name='username']").fill(user);
        page.locator("[name='password']").fill(password);
        page.locator("form button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /** Opens a page of the application and waits until its script has loaded the data. */
    void open(String path) {
        page.navigate("http://localhost:" + port + "/" + path);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
}
