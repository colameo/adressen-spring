package ch.example.adressen.config;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The login: 401 without a sign-in, the form, 403 for a user without the right, what /api/me says, and the first sign-in that has
 * to choose a password. The test users are in src/test/resources/security-test.yml (written by `pisa generate`); a chosen password lands in the table usr_password of the test's own database. Generated.
 */
@SpringBootTest
class SecurityTest {

    private static final String PATH = "/api/persons";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void answers401WithoutASignIn() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void aPageLeadsToTheLoginForABrowser() throws Exception {
        mockMvc.perform(get("/persons.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void theFormSignsInAndAWrongPasswordIsRefused() throws Exception {
        mockMvc.perform(post("/login").param("username", "tester").param("password", "tester"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/login").param("username", "tester").param("password", "wrong"))
                .andExpect(redirectedUrl("/login.html?error"));
        mockMvc.perform(post("/login").param("username", "nobody-here").param("password", "tester"))
                .andExpect(redirectedUrl("/login.html?error"));
    }

    @Test
    void aUserWithoutARoleThatAllowsItGets403() throws Exception {
        mockMvc.perform(get(PATH).with(user("nobody").roles("NOTHING"))).andExpect(status().isForbidden());
    }

    @Test
    void aUserWithTheRightRoleGetsThrough_andMeSaysWhatIsAllowed() throws Exception {
        mockMvc.perform(get(PATH).with(user("tester").roles("FULL"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/me").with(user("tester").roles("FULL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("tester"))
                .andExpect(jsonPath("$.roles[0]").value("FULL"))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.permissions.Person").isArray());
        mockMvc.perform(get("/api/me").with(user("nobody").roles("NOTHING")))
                .andExpect(jsonPath("$.permissions.Person.length()").value(0));
    }

    @Test
    void theFirstSignInHasToChooseAPasswordAndOnlyItsHashIsKept() throws Exception {
        // newcomer is marked in security-test.yml: until the password is chosen, everything but the way to choose it is closed
        var newcomer = user("newcomer").roles("FULL");
        mockMvc.perform(get(PATH).with(newcomer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("password change required"));
        mockMvc.perform(get("/api/me").with(newcomer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        String body = "{\"password\":\"%s\",\"repeat\":\"%s\"}";
        mockMvc.perform(post("/api/password").with(newcomer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("a-new-password", "another-one")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.repeat").exists());
        mockMvc.perform(post("/api/password").with(newcomer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("short", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
        mockMvc.perform(post("/api/password").with(newcomer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("newcomer", "newcomer")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password", containsString("user name")));
        mockMvc.perform(post("/api/password").with(newcomer).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("a-new-password", "a-new-password")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/password").with(newcomer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("a-new-password", "a-new-password")))
                .andExpect(status().isNoContent());

        // from now on the user is through, the new password works and the old one doesn't
        mockMvc.perform(get(PATH).with(newcomer)).andExpect(status().isOk());
        mockMvc.perform(post("/login").param("username", "newcomer").param("password", "a-new-password"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/login").param("username", "newcomer").param("password", "tester"))
                .andExpect(redirectedUrl("/login.html?error"));

        // only a bcrypt hash is kept (table usr_password), the password is nowhere in it
        String stored = jdbc.queryForObject("SELECT hash FROM usr_password WHERE name = 'newcomer'", String.class);
        assertTrue(stored.matches("\\$2[aby]\\$10\\$[./0-9A-Za-z]{53}"), stored);
        assertEquals(-1, stored.indexOf("a-new-password"));
        mockMvc.perform(get("/api/me").with(newcomer)).andExpect(jsonPath("$.mustChangePassword").value(false));
    }
}
