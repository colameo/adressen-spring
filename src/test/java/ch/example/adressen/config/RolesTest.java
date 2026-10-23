package ch.example.adressen.config;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * What each role of the architecture (`roles` in the .adl, plan.roles) may do with each entity: a request it may make doesn't end in
 * 403, one it may not make does, and /api/me lists exactly what is allowed. The users are in src/test/resources/security-test.yml. Generated.
 */
@SpringBootTest
class RolesTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private void check(String user, String role, HttpMethod method, String url, boolean allowed) throws Exception {
        var builder = request(method, url).with(user(user).roles(role)).with(csrf());
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            builder = builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        int status;
        try {
            status = mockMvc.perform(builder).andReturn().getResponse().getStatus();
        } catch (ServletException e) {
            // The request got through the access check and failed later (an empty `{}` is not a valid body): anything but 403.
            status = 500;
        }
        if (allowed) {
            assertNotEquals(403, status, user + " " + method + " " + url);
        } else {
            assertEquals(403, status, user + " " + method + " " + url);
        }
    }

    @Test
    void roleADMINOnPerson() throws Exception {
        var me = mockMvc.perform(get("/api/me").with(user("as-admin").roles("ADMIN"))).andExpect(status().isOk());
        me.andExpect(jsonPath("$.permissions.Person", containsInAnyOrder("create", "read", "update", "delete")));
        check("as-admin", "ADMIN", HttpMethod.POST, "/api/persons", true);
        check("as-admin", "ADMIN", HttpMethod.GET, "/api/persons", true);
        check("as-admin", "ADMIN", HttpMethod.PUT, "/api/persons/999999", true);
        check("as-admin", "ADMIN", HttpMethod.DELETE, "/api/persons/999999", true);
    }

    @Test
    void roleADMINOnCompany() throws Exception {
        var me = mockMvc.perform(get("/api/me").with(user("as-admin").roles("ADMIN"))).andExpect(status().isOk());
        me.andExpect(jsonPath("$.permissions.Company", containsInAnyOrder("create", "read", "update", "delete")));
        check("as-admin", "ADMIN", HttpMethod.POST, "/api/companies", true);
        check("as-admin", "ADMIN", HttpMethod.GET, "/api/companies", true);
        check("as-admin", "ADMIN", HttpMethod.PUT, "/api/companies/999999", true);
        check("as-admin", "ADMIN", HttpMethod.DELETE, "/api/companies/999999", true);
    }

    @Test
    void roleUSEROnPerson() throws Exception {
        var me = mockMvc.perform(get("/api/me").with(user("as-user").roles("USER"))).andExpect(status().isOk());
        me.andExpect(jsonPath("$.permissions.Person", containsInAnyOrder("create", "read", "update", "delete")));
        check("as-user", "USER", HttpMethod.POST, "/api/persons", true);
        check("as-user", "USER", HttpMethod.GET, "/api/persons", true);
        check("as-user", "USER", HttpMethod.PUT, "/api/persons/999999", true);
        check("as-user", "USER", HttpMethod.DELETE, "/api/persons/999999", true);
    }

    @Test
    void roleUSEROnCompany() throws Exception {
        var me = mockMvc.perform(get("/api/me").with(user("as-user").roles("USER"))).andExpect(status().isOk());
        me.andExpect(jsonPath("$.permissions.Company", containsInAnyOrder("read", "update")));
        check("as-user", "USER", HttpMethod.POST, "/api/companies", false);
        check("as-user", "USER", HttpMethod.GET, "/api/companies", true);
        check("as-user", "USER", HttpMethod.PUT, "/api/companies/999999", true);
        check("as-user", "USER", HttpMethod.DELETE, "/api/companies/999999", false);
    }
}
