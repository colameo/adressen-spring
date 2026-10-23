package ch.example.adressen.service;

import java.util.List;
import java.util.Optional;
import ch.example.adressen.dto.*;
import ch.example.adressen.mapper.*;
import ch.example.adressen.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of Person: the controller talks to this, this talks to the repository and maps entities to DTOs. */
@Service
@Transactional
public class PersonService {

    private final PersonRepository repository;
    private final PersonMapper mapper;

    public PersonService(PersonRepository repository, PersonMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PersonDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<PersonDto> page(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<PersonDto> find(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public PersonDto create(PersonDto dto) {
        dto.setId(null);
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    public Optional<PersonDto> update(Long id, PersonDto dto) {
        if (!repository.existsById(id)) {
            return Optional.empty();
        }
        dto.setId(id);
        return Optional.of(mapper.toDto(repository.save(mapper.toEntity(dto))));
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
