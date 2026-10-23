package ch.example.adressen.controller;

import jakarta.validation.Valid;
import java.util.List;
import ch.example.adressen.dto.*;
import ch.example.adressen.service.CompanyService;
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
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    /** All rows - for choosing one in another entity's form. */
    @GetMapping
    public List<CompanyDto> list() {
        return service.list();
    }

    /** One page of the rows, sorted by id - for the list on the entity's page. */
    @GetMapping(params = "page")
    public PageResponse<CompanyDto> page(@RequestParam int page, @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(service.page(PageResponse.request(page, size, "id")));
    }

    /** One page of the employees of a Company (kept on the other side, read-only here). */
    @GetMapping("/{id}/employees")
    public PageResponse<PersonDto> employees(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(service.employees(id, PageResponse.request(page, size, "id")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDto> get(@PathVariable Long id) {
        return service.find(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public CompanyDto create(@Valid @RequestBody CompanyDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyDto> update(@PathVariable Long id, @Valid @RequestBody CompanyDto dto) {
        return service.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
