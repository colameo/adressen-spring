package ch.example.adressen.controller;

import jakarta.validation.Valid;
import java.util.List;
import ch.example.adressen.dto.*;
import ch.example.adressen.service.PersonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    /** All rows - for choosing one in another entity's form. */
    @GetMapping
    public List<PersonDto> list() {
        return service.list();
    }

    /** One page of the rows, sorted by id - for the list on the entity's page. */
    @GetMapping(params = "page")
    public PageResponse<PersonDto> page(@RequestParam int page, @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(service.page(PageResponse.request(page, size, "id")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDto> get(@PathVariable Long id) {
        return service.find(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public PersonDto create(@Valid @RequestBody PersonDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonDto> update(@PathVariable Long id, @Valid @RequestBody PersonDto dto) {
        return service.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
