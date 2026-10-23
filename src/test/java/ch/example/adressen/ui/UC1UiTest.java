package ch.example.adressen.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import ch.example.adressen.model.*;
import ch.example.adressen.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UC1 Persons - from use-cases.uc.md, scenario "create" on the page of Person.
 * Rewritten by every {@code pisa generate}; the scenario is the "Test:" line of the use case.
 */
@Tag("UC1")
@DisplayName("UC1 Create person")
class UC1UiTest extends AbstractUiTest {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private CompanyRepository companyRepository;

    /** The use case names a role as its actor: sign in as the test user of that role instead of `tester`. */
    @BeforeEach
    void signInAsActor() {
        page.context().clearCookies();
        signIn("as-user", "tester");
    }

    @BeforeEach
    @AfterEach
    void clearRows() {
        repository.deleteAll();
        companyRepository.deleteAll();
    }

    private Locator rows() {
        return page.locator("main > table tbody tr");
    }

    private void fill(int variant) {
        page.locator("[name='firstName']").fill(variant == 0 ? "firstName-1" : "firstName-2");
        page.locator("[name='lastName']").fill(variant == 0 ? "lastName-1" : "lastName-2");
        page.locator("[name='email']").fill(variant == 0 ? "email-1@example.com" : "email-2@example.com");
        page.locator("[name='phone']").fill(variant == 0 ? "+41791234501" : "+41791234502");
        page.locator("[name='birthday']").fill(variant == 0 ? "2000-01-15" : "2000-01-16");
        page.locator("[name='status']").selectOption(variant == 0 ? "ACTIVE" : "BLOCKED");
        page.locator("[name='address.street']").fill(variant == 0 ? "address.street-1" : "address.street-2");
        page.locator("[name='address.zip']").fill(variant == 0 ? "0000" : "1111");
        page.locator("[name='address.city']").fill(variant == 0 ? "address.city-1" : "address.city-2");
    }

    private void create() {
        open("persons.html");
        fill(0);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        assertThat(rows().first()).containsText("firstName-1");
    }

    @Test
    void UC1_create() {
        create();
    }
}
