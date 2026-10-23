package ch.example.adressen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ch.example.adressen.dto.*;
import ch.example.adressen.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

/** The use cases of Company with the real repository and mapper: what goes in comes out, mapped both ways. */
@SpringBootTest
class CompanyServiceTest {

    @Autowired
    private CompanyService service;

    @Autowired
    private CompanyRepository repository;

    @Autowired
    private PersonRepository personRepository;

    @AfterEach
    void cleanUp() {
        personRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void createsARowAndFindsItAgainWithEveryValue() {
        CompanyDto entity = new CompanyDto();
        entity.setName("name-1");
        CompanyDto created = service.create(entity);

        CompanyDto found = service.find(created.getId()).orElseThrow();
        assertEquals("name-1", found.getName());
    }

    @Test
    void updatesARow() {
        CompanyDto entity = new CompanyDto();
        entity.setName("name-1");
        CompanyDto created = service.create(entity);
        CompanyDto updated = new CompanyDto();
        updated.setName("name-2");

        CompanyDto found = service.update(created.getId(), updated).orElseThrow();
        assertEquals("name-2", found.getName());
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void listsAndPagesTheRows() {
        CompanyDto entity = new CompanyDto();
        entity.setName("name-1");
        service.create(entity);

        assertEquals(1, service.list().size());
        assertEquals(1, service.page(PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void deletesARowAndKnowsWhenThereIsNone() {
        CompanyDto entity = new CompanyDto();
        entity.setName("name-1");
        CompanyDto created = service.create(entity);

        assertTrue(service.delete(created.getId()));
        assertTrue(service.find(created.getId()).isEmpty());
        assertFalse(service.delete(created.getId()));
    }
}
