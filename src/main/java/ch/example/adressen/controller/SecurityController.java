package ch.example.adressen.controller;

import ch.example.adressen.config.EntityAccess;
import ch.example.adressen.config.SecurityConfig;
import ch.example.adressen.config.SecurityStore;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Who is signed in and what they may do (for the pages), and choosing a password. */
@RestController
public class SecurityController {

    private final SecurityStore store;
    private final PasswordEncoder encoder;

    public SecurityController(SecurityStore store, PasswordEncoder encoder) {
        this.store = store;
        this.encoder = encoder;
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        List<String> roles = SecurityConfig.rolesOf(authentication);
        Map<String, Object> permissions = new TreeMap<>();
        EntityAccess.RESOURCES.forEach(
                resource -> permissions.put(resource.entity(), store.permissions(roles, resource)));
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("name", authentication.getName());
        me.put("roles", roles);
        me.put("mustChangePassword", store.mustChangePassword(authentication.getName()));
        me.put("permissions", permissions);
        return me;
    }

    public record PasswordChange(String current, String password, String repeat) {}

    /**
     * Sets a new password: at the first login without the current one, later with it. The password is hashed with bcrypt and
     * only the hash is kept; it is never logged.
     */
    @PostMapping("/api/password")
    public ResponseEntity<Map<String, Object>> password(
            Authentication authentication, @RequestBody PasswordChange change) {
        String name = authentication.getName();
        if (!store.mustChangePassword(name)
                && (change.current() == null
                        || !encoder.matches(
                                change.current(), store.loadUserByUsername(name).getPassword()))) {
            return rejected("current", "is not right");
        }
        String password = change.password() == null ? "" : change.password();
        if (!password.equals(change.repeat())) {
            return rejected("repeat", "is not the same");
        }
        if (password.length() < 8) {
            return rejected("password", "must be at least 8 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return rejected("password", "must be at most 72 bytes (bcrypt ignores the rest)");
        }
        if (password.equals(name)) {
            return rejected("password", "must not be the user name");
        }
        store.setPassword(name, encoder.encode(password));
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<Map<String, Object>> rejected(String field, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "The password was not changed.", "fields", Map.of(field, message)));
    }
}
