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
class PersonRepositoryTest {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void savesAndReadsBackEveryValue() {
        Person entity = new Person();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        Address entityAddress = new Address();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        Person saved = repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        Person found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("firstName-1", found.getFirstName());
        assertEquals("lastName-1", found.getLastName());
        assertEquals("email-1@example.com", found.getEmail());
        assertEquals("+41791234501", found.getPhone());
        assertEquals(LocalDate.of(2000, 1, 15), found.getBirthday());
        assertEquals(PersonStatus.ACTIVE, found.getStatus());
        assertEquals("street-1", found.getAddress().getStreet());
        assertEquals("0000", found.getAddress().getZip());
        assertEquals("city-1", found.getAddress().getCity());
    }

    @Test
    void deletesTheRow() {
        Person entity = new Person();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        Address entityAddress = new Address();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        Person saved = repository.save(entity);
        entityManager.flush();

        repository.deleteById(saved.getId());
        entityManager.flush();

        assertTrue(repository.findById(saved.getId()).isEmpty());
    }

    @Test
    void rejectsAMissingRequiredValue() {
        Person entity = new Person();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        Address entityAddress = new Address();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        entity.setFirstName(null);

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(entity));
    }

    @Test
    void rejectsADuplicateOfAUniqueValue() {
        Person entity = new Person();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        Address entityAddress = new Address();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        repository.saveAndFlush(entity);

        Person duplicate = new Person();
        duplicate.setFirstName("firstName-2");
        duplicate.setLastName("lastName-2");
        duplicate.setEmail("email-2@example.com");
        duplicate.setPhone("+41791234502");
        duplicate.setBirthday(LocalDate.of(2000, 1, 16));
        duplicate.setStatus(PersonStatus.BLOCKED);
        Address duplicateAddress = new Address();
        duplicateAddress.setStreet("street-2");
        duplicateAddress.setZip("1111");
        duplicateAddress.setCity("city-2");
        duplicate.setAddress(duplicateAddress);
        duplicate.setEmail("email-1@example.com");

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
    }

    @Test
    void findByCompany_IdFindsOnlyTheRowsOfThatOwner() {
        Company owner = new Company();
        owner.setName("name-1");
        Company savedOwner = companyRepository.save(owner);
        Person entity = new Person();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        Address entityAddress = new Address();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        entity.setCompany(savedOwner);
        repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        var page = repository.findByCompany_Id(savedOwner.getId(), PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
    }
}
