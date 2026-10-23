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
class CompanyControllerTest {

    private static final String PATH = "/api/companies";

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
    private CompanyRepository repository;

    @Autowired
    private PersonRepository personRepository;

    @AfterEach
    void cleanUp() {
        personRepository.deleteAll();
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
        String id = create("{\"name\":\"name-1\"}");

        mockMvc.perform(get(PATH + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("name-1"))
        ;
    }

    @Test
    void listsTheRowsAllAtOnceAndByPage() throws Exception {
        create("{\"name\":\"name-1\"}");

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
        String id = create("{\"name\":\"name-1\"}");

        mockMvc.perform(put(PATH + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"name-2\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PATH + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("name-2"))
        ;
    }

    @Test
    void deletesARow() throws Exception {
        String id = create("{\"name\":\"name-1\"}");

        mockMvc.perform(delete(PATH + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(PATH + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(PATH + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void answers404ForARowThatDoesNotExist() throws Exception {
        mockMvc.perform(get(PATH + "/999999")).andExpect(status().isNotFound());
        mockMvc.perform(put(PATH + "/999999").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"name-1\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void answers400WhenARequiredValueIsMissing() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answers409ForADuplicateOfAUniqueValue() throws Exception {
        create("{\"name\":\"name-1\"}");

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"name-1\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void pagesTheEmployeesOfARow() throws Exception {
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

        mockMvc.perform(get(PATH + "/" + saved.getId() + "/employees").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items.length()").value(1));
    }
}
