package ch.example.adressen.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The name of the application for the home page: app.title in application.yml. Generated. */
@RestController
public class AppController {

    private final String title;

    public AppController(@Value("${app.title:Application}") String title) {
        this.title = title;
    }

    @GetMapping("/api/app")
    public Map<String, String> app() {
        return Map.of("title", title);
    }
}
