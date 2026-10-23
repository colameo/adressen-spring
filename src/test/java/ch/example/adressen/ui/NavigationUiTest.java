package ch.example.adressen.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NavigationUiTest extends AbstractUiTest {

    @Test
    void theHomePageShowsTheNameOfTheApplication() {
        open("index.html");

        // The heading comes from app.title in application.yml (GET /api/app).
        assertThat(page.locator("main > h1")).hasText(Pattern.compile(".+"));
    }

    @Test
    void theHomePageLinksToEveryPage() {
        open("index.html");

        assertThat(page.locator("main a[href='persons.html']")).hasText("Persons");
        assertThat(page.locator("main a[href='companies.html']")).hasText("Companies");
    }

    @Test
    void theMenuOpens_persons() {
        open("index.html");

        page.locator("nav a[href='persons.html']").click();

        assertThat(page).hasURL(Pattern.compile(".*/persons.html$"));
        assertThat(page.locator("main > h1")).hasText("Persons");
    }

    @Test
    void theMenuOpens_companies() {
        open("index.html");

        page.locator("nav a[href='companies.html']").click();

        assertThat(page).hasURL(Pattern.compile(".*/companies.html$"));
        assertThat(page.locator("main > h1")).hasText("Companies");
    }

    @Test
    void theUserMenuOn_index_isClosedOpensWithItsButtonAndClosesWithEscapeAndAClickElsewhere() {
        open("index.html");

        assertThat(page.locator("#who-name")).isHidden();
        page.locator(".menu-button").click();
        assertThat(page.locator("#who-name")).isVisible();
        page.keyboard().press("Escape");
        assertThat(page.locator("#who-name")).isHidden();
        page.locator(".menu-button").click();
        page.mouse().click(5, 400);
        assertThat(page.locator("#who-name")).isHidden();
    }

    @Test
    void theUserMenuOn_persons_isClosedOpensWithItsButtonAndClosesWithEscapeAndAClickElsewhere() {
        open("persons.html");

        assertThat(page.locator("#who-name")).isHidden();
        page.locator(".menu-button").click();
        assertThat(page.locator("#who-name")).isVisible();
        page.keyboard().press("Escape");
        assertThat(page.locator("#who-name")).isHidden();
        page.locator(".menu-button").click();
        page.mouse().click(5, 400);
        assertThat(page.locator("#who-name")).isHidden();
    }
}
