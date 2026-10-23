package ch.example.adressen.repository;

import ch.example.adressen.model.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Page<Person> findByCompany_Id(Long id, Pageable pageable);
}
