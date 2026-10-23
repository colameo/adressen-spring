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

class PersonPageUiTest extends AbstractUiTest {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private CompanyRepository companyRepository;

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

    @Test
    void showsItsHeadingAndAnEmptyList() {
        open("persons.html");

        assertThat(page.locator("main > h1")).hasText("Persons");
        assertThat(page.getByText("No entries yet.")).isVisible();
    }

    @Test
    void createsEditsAndDeletesARowThroughTheForm() {
        open("persons.html");

        fill(0);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        assertThat(rows().first()).containsText("firstName-1");

        rows().first().locator("button:has-text('Edit')").click();
        assertThat(page.locator("[name='firstName']")).hasValue("firstName-1");
        fill(1);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        assertThat(rows().first()).containsText("firstName-2");

        page.onDialog(Dialog::accept);
        rows().first().locator("button:has-text('Delete')").click();
        assertThat(rows()).hasCount(0);
    }

    @Test
    void aClickOnARowShowsTheEntryReadOnly() {
        open("persons.html");

        fill(0);
        page.locator("form button[type=submit]").click();
        assertThat(rows()).hasCount(1);
        rows().first().locator("td").first().click();

        Locator field = page.locator("form [name='firstName']");
        assertThat(field).isVisible();
        assertThat(field).isDisabled();
        assertThat(page.locator("form button[type=submit]")).isHidden();

        page.locator("form button:has-text('Edit')").click();
        assertThat(field).isEnabled();
        assertThat(page.locator("form button[type=submit]")).isVisible();
    }

    @Test
    void marksTheFieldRedForAnInvalidValue() {
        open("persons.html");

        fill(0);
        page.locator("[name='email']").fill("foo@bar");
        page.locator("form button[type=submit]").click();

        // The page marks the field red and says why; nothing is sent.
        assertThat(page.locator("[name='email'].invalid")).hasCount(1);
        assertThat(page.locator("label:has([name='email']) .field-error")).isVisible();
        assertThat(rows()).hasCount(0);
    }

    @Test
    void marksTheFieldTheServerRejects() {
        // A 400 answer that names a field, as the server sends it for a rule the page doesn't know.
        page.route("**/api/persons", route -> {
            if ("POST".equals(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(400)
                        .setContentType("application/json")
                        .setBody("{\"error\":\"Some values are not valid.\",\"fields\":{\"firstName\":\"rejected by the server\"}}"));
            } else {
                route.resume();
            }
        });
        open("persons.html");

        fill(0);
        page.locator("form button[type=submit]").click();

        assertThat(page.locator("[name='firstName'].invalid")).hasCount(1);
        assertThat(page.getByText("rejected by the server")).isVisible();
        assertThat(rows()).hasCount(0);
    }

    @Test
    void offersTheRowsToChooseFor_company() {
        Company target = new Company();
        target.setName("name-1");
        companyRepository.save(target);

        open("persons.html");

        assertThat(page.locator("select[name='company'] option")).hasCount(2);
    }
}
