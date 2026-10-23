package ch.example.adressen.config;

import java.util.List;
import java.util.Set;

/**
 * The REST resources of the model and what each entity allows at most (`@operations` in the .dml; all four by default).
 * Generated from spec/*.dml - rewritten on every `pisa generate`.
 */
public final class EntityAccess {

    public static final Set<String> OPERATIONS = Set.of("create", "read", "update", "delete");

    public record Resource(String path, String entity, Set<String> operations) {}

    public static final List<Resource> RESOURCES = List.of(
            new Resource("/api/persons", "Person", OPERATIONS),
            new Resource("/api/companies", "Company", OPERATIONS)
    );

    private EntityAccess() {}

    /** The resource a request path belongs to: /api/persons, /api/persons/3, /api/persons/3/something. */
    public static Resource resourceOf(String path) {
        return RESOURCES.stream()
                .filter(r -> path.equals(r.path()) || path.startsWith(r.path() + "/"))
                .findFirst()
                .orElse(null);
    }

    /** The operation an HTTP method stands for. */
    public static String operationOf(String method) {
        return switch (method) {
            case "POST" -> "create";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> "read";
        };
    }
}
