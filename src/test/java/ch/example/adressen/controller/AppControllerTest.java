package ch.example.adressen.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** GET /api/app answers the title it was given (the application passes app.title from application.yml). Generated. */
class AppControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AppController("Address book")).build();

    @Test
    void answersTheTitle() throws Exception {
        mockMvc.perform(get("/api/app")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Address book"));
    }
}
