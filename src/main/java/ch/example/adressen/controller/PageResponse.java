package ch.example.adressen.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** One page of a list, as the generated pages read it: the rows and where they are in the whole. */
public record PageResponse<T>(List<T> items, long total, int page, int size, int pages) {

    /** Pages are at most this long, whatever a client asks for. */
    public static final int MAX_SIZE = 100;

    public static Pageable request(int page, int size, String sortBy) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), Sort.by(sortBy));
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages());
    }
}
