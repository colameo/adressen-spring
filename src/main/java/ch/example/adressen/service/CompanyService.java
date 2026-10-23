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

/** The use cases of Company: the controller talks to this, this talks to the repository and maps entities to DTOs. */
@Service
@Transactional
public class CompanyService {

    private final CompanyRepository repository;
    private final CompanyMapper mapper;
    private final PersonRepository personRepository;
    private final PersonMapper personMapper;

    public CompanyService(CompanyRepository repository, CompanyMapper mapper, PersonRepository personRepository, PersonMapper personMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.personRepository = personRepository;
        this.personMapper = personMapper;
    }

    @Transactional(readOnly = true)
    public List<CompanyDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<CompanyDto> page(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    /** One page of the employees of a Company (kept on the Person side, read-only here). */
    @Transactional(readOnly = true)
    public Page<PersonDto> employees(Long id, Pageable pageable) {
        return personRepository.findByCompany_Id(id, pageable).map(personMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<CompanyDto> find(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public CompanyDto create(CompanyDto dto) {
        dto.setId(null);
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    public Optional<CompanyDto> update(Long id, CompanyDto dto) {
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
