package ch.example.adressen.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * The users and roles of the application. security.yml (on the classpath, read when the application starts) lists the roles
 * with what they may do and the users with their roles. A user starts with the password of their name (public knowledge, so
 * they have to choose a new one at the first login); the hash of the password they chose is kept in the table usr_password of
 * the application's database. Passwords exist as bcrypt hashes only.
 */
@Component
public class SecurityStore implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityStore.class);

    private static final java.util.regex.Pattern BCRYPT =
            java.util.regex.Pattern.compile("^\\$2[aby]\\$\\d\\d\\$[./0-9A-Za-z]{53}$");

    /** `start` is the hash of the password the user starts with (the user name, or the `password` of security.yml). */
    private record Account(String name, String start, List<String> roles, boolean mustChange) {}

    private final JdbcTemplate jdbc;
    private final Map<String, Map<String, Set<String>>> roles = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public SecurityStore(
            @Value("${pisa.security.file:classpath:security.yml}") Resource file, JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        load(file);
        LOG.info("Users from {} are active: {}", file.getDescription(), String.join(", ", accounts.keySet()));
    }

    /** The table of the passwords users chose (name, bcrypt hash): the same database as the application's data. */
    @PostConstruct
    void createTable() {
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS usr_password (name VARCHAR(100) PRIMARY KEY, hash VARCHAR(100) NOT NULL)");
        accounts.values().stream()
                .filter(a -> a.mustChange() && chosen(a.name()) == null)
                .forEach(a -> LOG.warn(
                        "User '{}' still has the password of the start and has to choose a new one at the first login.",
                        a.name()));
    }

    // --- reading ------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void load(Resource file) {
        Map<String, Object> root;
        try (Reader reader = new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            Object value = new Yaml().load(reader);
            if (!(value instanceof Map)) {
                throw new IllegalStateException(file.getDescription() + " must be a mapping with roles and users.");
            }
            root = (Map<String, Object>) value;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file.getDescription() + ": " + e.getMessage(), e);
        }
        List<String> problems = new ArrayList<>();
        Object roleMap = root.get("roles");
        if (roleMap instanceof Map<?, ?> map) {
            map.forEach((role, rights) -> {
                Map<String, Set<String>> perEntity = new LinkedHashMap<>();
                if (rights instanceof Map<?, ?> byEntity) {
                    byEntity.forEach((entity, ops) -> {
                        Set<String> operations = new LinkedHashSet<>();
                        if (ops instanceof List<?> list) {
                            list.forEach(op -> operations.add(String.valueOf(op)));
                        }
                        if (!EntityAccess.OPERATIONS.containsAll(operations) || operations.isEmpty()) {
                            problems.add(
                                    "roles." + role + "." + entity + " must be a list of " + EntityAccess.OPERATIONS);
                        }
                        perEntity.put(String.valueOf(entity), operations);
                    });
                } else {
                    problems.add("roles." + role + " must be a mapping of entity names (or \"*\") to operations.");
                }
                roles.put(String.valueOf(role), perEntity);
            });
        } else {
            problems.add("roles must be a mapping.");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        Object userList = root.get("users");
        if (userList instanceof List<?> list) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> user)) {
                    problems.add("every user must be a mapping with name and roles.");
                    continue;
                }
                String name = String.valueOf(user.get("name"));
                // The password a user starts with: their name, or a bcrypt hash given as `password`.
                String start;
                if (user.get("password") == null) {
                    start = encoder.encode(name);
                } else {
                    start = String.valueOf(user.get("password"));
                    if (!BCRYPT.matcher(start).matches()) {
                        problems.add(
                                "User '" + name
                                        + "': password must be a bcrypt hash, never plain text (make one with: pisa hash-password).");
                    }
                }
                List<String> userRoles = new ArrayList<>();
                if (user.get("roles") instanceof List<?> given) {
                    given.forEach(r -> userRoles.add(String.valueOf(r)));
                }
                userRoles.stream()
                        .filter(r -> !roles.containsKey(r))
                        .forEach(r -> problems.add(
                                "User '" + name + "': the role \"" + r + "\" is not defined under roles."));
                if (accounts.containsKey(name)) {
                    problems.add("The user '" + name + "' is listed twice.");
                }
                accounts.put(name, new Account(name, start, userRoles, !Boolean.FALSE.equals(user.get("mustChangePassword"))));
            }
        } else {
            problems.add("users must be a list.");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException(file.getDescription() + " is not valid:\n  " + String.join("\n  ", problems));
        }
    }

    // --- users --------------------------------------------------------------------------------------------------

    /** The hash of the password the user chose, or null if they have not chosen one. */
    private String chosen(String name) {
        List<String> hashes = jdbc.queryForList("SELECT hash FROM usr_password WHERE name = ?", String.class, name);
        return hashes.isEmpty() ? null : hashes.get(0);
    }

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        Account account = accounts.get(name);
        if (account == null) {
            throw new UsernameNotFoundException("unknown user");
        }
        String chosen = chosen(name);
        return User.withUsername(account.name())
                .password(chosen != null ? chosen : account.start())
                .authorities(account.roles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList())
                .build();
    }

    /** The user has to choose a new password: marked so in security.yml (the default), and none chosen yet. */
    public boolean mustChangePassword(String name) {
        Account account = accounts.get(name);
        return account != null && account.mustChange() && chosen(name) == null;
    }

    /** Keeps the hash of a password the user chose: one row per user, replaced when they choose again. */
    public void setPassword(String name, String hash) {
        if (jdbc.update("UPDATE usr_password SET hash = ? WHERE name = ?", hash, name) == 0) {
            jdbc.update("INSERT INTO usr_password (name, hash) VALUES (?, ?)", name, hash);
        }
    }

    // --- rights -------------------------------------------------------------------------------------------------

    /**
     * What the roles allow on an entity: the union over the roles of the list for that entity, else of the list for "*",
     * cut with what the entity itself allows (`@operations` in the model).
     */
    public Set<String> permissions(Iterable<String> userRoles, EntityAccess.Resource resource) {
        Set<String> allowed = new TreeSet<>();
        for (String role : userRoles) {
            Map<String, Set<String>> rights = roles.get(role);
            if (rights == null) {
                continue;
            }
            Set<String> own = rights.containsKey(resource.entity()) ? rights.get(resource.entity()) : rights.get("*");
            if (own != null) {
                allowed.addAll(own);
            }
        }
        allowed.retainAll(resource.operations());
        return allowed;
    }
}
