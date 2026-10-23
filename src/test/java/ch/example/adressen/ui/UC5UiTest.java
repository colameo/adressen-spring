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
 * UC5 Companies - from use-cases.uc.md, scenario "edit" on the page of Company.
 * Rewritten by every {@code pisa generate}; the scenario is the "Test:" line of the use case.
 */
@Tag("UC5")
@DisplayName("UC5 Change company")
class UC5UiTest extends AbstractUiTest {

    @Autowired
    private CompanyRepository repository;

    @Autowired
    private PersonRepository personRepository;

    /** The use case names a role as its actor: sign in as the test user of that role instead of `tester`. */
    @BeforeEach
    void signInAsActor() {
        page.context().clearCookies();
        signIn("as-user", "tester");
    }

    @BeforeEach
    @AfterEach
    void clearRows() {
        personRepository.deleteAll();
        repository.deleteAll();
    }

    private Locator rows() {
        return page.locator("main > table tbody tr");
    }

    private void fill(int variant) {
        page.locator("[name='name']").fill(variant == 0 ? "name-1" : "name-2");
    }

    /** The entity doesn't allow create (@operations): the row of the sample goes straight into the repository. */
    private void create() {
        Company entity = new Company();
        entity.setName("name-1");
        repository.save(entity);
        open("companies.html");
        assertThat(rows()).hasCount(1);
        assertThat(rows().first()).containsText("name-1");
    }

    @Test
    void UC5_edit() {
        create();

        rows().first().locator("button:has-text('Edit')").click();
        fill(1);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        assertThat(rows().first()).containsText("name-2");
    }
}
