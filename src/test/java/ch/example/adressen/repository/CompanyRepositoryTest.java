package ch.example.adressen.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ch.example.adressen.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsBackEveryValue() {
        Company entity = new Company();
        entity.setName("name-1");
        Company saved = repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        Company found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("name-1", found.getName());
    }

    @Test
    void deletesTheRow() {
        Company entity = new Company();
        entity.setName("name-1");
        Company saved = repository.save(entity);
        entityManager.flush();

        repository.deleteById(saved.getId());
        entityManager.flush();

        assertTrue(repository.findById(saved.getId()).isEmpty());
    }

    @Test
    void rejectsAMissingRequiredValue() {
        Company entity = new Company();
        entity.setName("name-1");
        entity.setName(null);

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(entity));
    }

    @Test
    void rejectsADuplicateOfAUniqueValue() {
        Company entity = new Company();
        entity.setName("name-1");
        repository.saveAndFlush(entity);

        Company duplicate = new Company();
        duplicate.setName("name-2");
        duplicate.setName("name-1");

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
    }
}
