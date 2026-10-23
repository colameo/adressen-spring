package ch.example.adressen.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ch.example.adressen.model.*;
import ch.example.adressen.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PersonControllerTest {

    private static final String PATH = "/api/persons";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    /** Every request is made as the test user `tester`, who may do what the entity allows (src/test/resources/security-test.yml). */
    @BeforeEach
    void signIn() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(user("tester").roles("FULL")).with(csrf()))
                .build();
    }

    @Autowired
    private PersonRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    /** Creates a row through the API and returns its id as it appears in a URL. */
    private String create(String body) throws Exception {
        String response = mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((Object) JsonPath.read(response, "$.id"));
    }

    @Test
    void createsAndReadsARow() throws Exception {
        String id = create("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}");

        mockMvc.perform(get(PATH + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("firstName-1"))
                .andExpect(jsonPath("$.lastName").value("lastName-1"))
                .andExpect(jsonPath("$.email").value("email-1@example.com"))
                .andExpect(jsonPath("$.phone").value("+41791234501"))
                .andExpect(jsonPath("$.birthday").value("2000-01-15"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.address.street").value("street-1"))
                .andExpect(jsonPath("$.address.zip").value("0000"))
                .andExpect(jsonPath("$.address.city").value("city-1"))
        ;
    }

    @Test
    void listsTheRowsAllAtOnceAndByPage() throws Exception {
        create("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}");

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(PATH).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pages").value(1))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void updatesARow() throws Exception {
        String id = create("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}");

        mockMvc.perform(put(PATH + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-2\",\"lastName\":\"lastName-2\",\"email\":\"email-2@example.com\",\"phone\":\"+41791234502\",\"birthday\":\"2000-01-16\",\"status\":\"BLOCKED\",\"address\":{\"street\":\"street-2\",\"zip\":\"1111\",\"city\":\"city-2\"}}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PATH + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("firstName-2"))
                .andExpect(jsonPath("$.lastName").value("lastName-2"))
                .andExpect(jsonPath("$.email").value("email-2@example.com"))
                .andExpect(jsonPath("$.phone").value("+41791234502"))
                .andExpect(jsonPath("$.birthday").value("2000-01-16"))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.address.street").value("street-2"))
                .andExpect(jsonPath("$.address.zip").value("1111"))
                .andExpect(jsonPath("$.address.city").value("city-2"))
        ;
    }

    @Test
    void deletesARow() throws Exception {
        String id = create("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}");

        mockMvc.perform(delete(PATH + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(PATH + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(PATH + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void answers404ForARowThatDoesNotExist() throws Exception {
        mockMvc.perform(get(PATH + "/999999")).andExpect(status().isNotFound());
        mockMvc.perform(put(PATH + "/999999").contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void answers400WhenARequiredValueIsMissing() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answers400ForAnUnknownValueOf_status() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"NO_SUCH_VALUE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answers400ForAnInvalid_firstName_tooLong() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['firstName']").exists());
    }

    @Test
    void answers400ForAnInvalid_firstName_tooShort() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['firstName']").exists());
    }

    @Test
    void answers400ForAnInvalid_lastName_tooLong() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['lastName']").exists());
    }

    @Test
    void answers400ForAnInvalid_lastName_tooShort() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['lastName']").exists());
    }

    @Test
    void answers400ForAnInvalid_email_emailWithoutDomainEnding() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"foo@bar\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['email']").exists());
    }

    @Test
    void answers400ForAnInvalid_email_notAnEmail() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"not-an-email\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['email']").exists());
    }

    @Test
    void answers400ForAnInvalid_phone_notAPhone() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"abc\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['phone']").exists());
    }

    @Test
    void answers400ForAnInvalid_address_zip_patternMismatch() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000!\",\"city\":\"city-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['address.zip']").exists());
    }

    @Test
    void answers409ForADuplicateOfAUniqueValue() throws Exception {
        create("{\"firstName\":\"firstName-1\",\"lastName\":\"lastName-1\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234501\",\"birthday\":\"2000-01-15\",\"status\":\"ACTIVE\",\"address\":{\"street\":\"street-1\",\"zip\":\"0000\",\"city\":\"city-1\"}}");

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"firstName-2\",\"lastName\":\"lastName-2\",\"email\":\"email-1@example.com\",\"phone\":\"+41791234502\",\"birthday\":\"2000-01-16\",\"status\":\"BLOCKED\",\"address\":{\"street\":\"street-2\",\"zip\":\"1111\",\"city\":\"city-2\"}}"))
                .andExpect(status().isConflict());
    }
}
