package ch.example.adressen.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ch.example.adressen.model.*;
import ch.example.adressen.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CompanyPageUiTest extends AbstractUiTest {

    @Autowired
    private CompanyRepository repository;

    @Autowired
    private PersonRepository personRepository;

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

    @Test
    void showsItsHeadingAndAnEmptyList() {
        open("companies.html");

        assertThat(page.locator("main > h1")).hasText("Companies");
        assertThat(page.getByText("No entries yet.")).isVisible();
    }

    @Test
    void createsEditsAndDeletesARowThroughTheForm() {
        open("companies.html");

        fill(0);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        assertThat(rows().first()).containsText("name-1");

        rows().first().locator("button:has-text('Edit')").click();
        assertThat(page.locator("[name='name']")).hasValue("name-1");
        fill(1);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        assertThat(rows().first()).containsText("name-2");

        page.onDialog(Dialog::accept);
        rows().first().locator("button:has-text('Delete')").click();
        assertThat(rows()).hasCount(0);
    }

    @Test
    void aClickOnARowShowsTheEntryReadOnly() {
        open("companies.html");

        fill(0);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        rows().first().locator("td").first().click();

        Locator field = page.locator("form [name='name']");
        assertThat(field).isVisible();
        assertThat(field).isDisabled();
        assertThat(page.locator("form button[type=submit]")).isHidden();

        page.locator("form button:has-text('Edit')").click();
        assertThat(field).isEnabled();
        assertThat(page.locator("form button[type=submit]")).isVisible();
    }

    @Test
    void marksTheFieldTheServerRejects() {
        // A 400 answer that names a field, as the server sends it for a rule the page doesn't know.
        page.route("**/api/companies", route -> {
            if ("POST".equals(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(400)
                        .setContentType("application/json")
                        .setBody("{\"error\":\"Some values are not valid.\",\"fields\":{\"name\":\"rejected by the server\"}}"));
            } else {
                route.resume();
            }
        });
        open("companies.html");

        fill(0);
        page.locator("form button[type=submit]").click();

        assertThat(page.locator("[name='name'].invalid")).hasCount(1);
        assertThat(page.getByText("rejected by the server")).isVisible();
        assertThat(rows()).hasCount(0);
    }

    @Test
    void listsTheRowsOfTheTable_Employees() {
        Company entity = new Company();
        entity.setName("name-1");
        Company saved = repository.save(entity);
        Person target = new Person();
        target.setFirstName("firstName-1");
        target.setLastName("lastName-1");
        target.setEmail("email-1@example.com");
        target.setPhone("+41791234501");
        target.setBirthday(LocalDate.of(2000, 1, 15));
        target.setStatus(PersonStatus.ACTIVE);
        Address targetAddress = new Address();
        targetAddress.setStreet("street-1");
        targetAddress.setZip("0000");
        targetAddress.setCity("city-1");
        target.setAddress(targetAddress);
        target.setCompany(saved);
        personRepository.save(target);

        open("companies.html#edit=" + saved.getId());

        assertThat(page.locator(".subtable:has(.caption:text-is('Employees')) tbody tr")).hasCount(1);
    }
}
