package ch.example.adressen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ch.example.adressen.dto.*;
import ch.example.adressen.model.*;
import ch.example.adressen.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

/** The use cases of Person with the real repository and mapper: what goes in comes out, mapped both ways. */
@SpringBootTest
class PersonServiceTest {

    @Autowired
    private PersonService service;

    @Autowired
    private PersonRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void createsARowAndFindsItAgainWithEveryValue() {
        PersonDto entity = new PersonDto();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        AddressDto entityAddress = new AddressDto();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        PersonDto created = service.create(entity);

        PersonDto found = service.find(created.getId()).orElseThrow();
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
    void updatesARow() {
        PersonDto entity = new PersonDto();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        AddressDto entityAddress = new AddressDto();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        PersonDto created = service.create(entity);
        PersonDto updated = new PersonDto();
        updated.setFirstName("firstName-2");
        updated.setLastName("lastName-2");
        updated.setEmail("email-2@example.com");
        updated.setPhone("+41791234502");
        updated.setBirthday(LocalDate.of(2000, 1, 16));
        updated.setStatus(PersonStatus.BLOCKED);
        AddressDto updatedAddress = new AddressDto();
        updatedAddress.setStreet("street-2");
        updatedAddress.setZip("1111");
        updatedAddress.setCity("city-2");
        updated.setAddress(updatedAddress);

        PersonDto found = service.update(created.getId(), updated).orElseThrow();
        assertEquals("firstName-2", found.getFirstName());
        assertEquals("lastName-2", found.getLastName());
        assertEquals("email-2@example.com", found.getEmail());
        assertEquals("+41791234502", found.getPhone());
        assertEquals(LocalDate.of(2000, 1, 16), found.getBirthday());
        assertEquals(PersonStatus.BLOCKED, found.getStatus());
        assertEquals("street-2", found.getAddress().getStreet());
        assertEquals("1111", found.getAddress().getZip());
        assertEquals("city-2", found.getAddress().getCity());
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void listsAndPagesTheRows() {
        PersonDto entity = new PersonDto();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        AddressDto entityAddress = new AddressDto();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        service.create(entity);

        assertEquals(1, service.list().size());
        assertEquals(1, service.page(PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void deletesARowAndKnowsWhenThereIsNone() {
        PersonDto entity = new PersonDto();
        entity.setFirstName("firstName-1");
        entity.setLastName("lastName-1");
        entity.setEmail("email-1@example.com");
        entity.setPhone("+41791234501");
        entity.setBirthday(LocalDate.of(2000, 1, 15));
        entity.setStatus(PersonStatus.ACTIVE);
        AddressDto entityAddress = new AddressDto();
        entityAddress.setStreet("street-1");
        entityAddress.setZip("0000");
        entityAddress.setCity("city-1");
        entity.setAddress(entityAddress);
        PersonDto created = service.create(entity);

        assertTrue(service.delete(created.getId()));
        assertTrue(service.find(created.getId()).isEmpty());
        assertFalse(service.delete(created.getId()));
    }
}
