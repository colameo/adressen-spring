package ch.example.adressen.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The first sign-in in a browser: the login leads to choosing a password (twice), and then to the application. Generated. */
class PasswordChangeUiTest extends AbstractUiTest {

    @Test
    void theFirstSignInLeadsToChoosingAPassword() {
        // newbie is marked in src/test/resources/security-test.yml (the password is tester until it is chosen)
        page.context().clearCookies();
        signIn("newbie", "tester");

        assertThat(page).hasURL(Pattern.compile(".*/change-password\\.html$"));
        assertThat(page.getByText("This is your first sign-in")).isVisible();

        page.locator("[name='password']").fill("a-new-password");
        page.locator("[name='repeat']").fill("another-one");
        page.locator("form button[type=submit]").click();
        assertThat(page.locator("#repeat-error")).hasText("is not the same");

        page.locator("[name='repeat']").fill("a-new-password");
        page.locator("form button[type=submit]").click();

        // The user menu (hamburger, top right): closed at first, opens with the button, closes with Escape, signs out.
        assertThat(page.locator("#who-name")).isHidden();
        page.locator(".menu-button").click();
        assertThat(page.locator("#who-name")).hasText("newbie");
        page.keyboard().press("Escape");
        assertThat(page.locator("#who-name")).isHidden();
        page.locator(".menu-button").click();
        page.getByText("Sign out").click();
        assertThat(page).hasURL(Pattern.compile(".*/login\\.html$"));
    }
}
