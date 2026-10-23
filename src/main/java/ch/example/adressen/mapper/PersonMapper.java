package ch.example.adressen.mapper;

import ch.example.adressen.dto.PersonDto;
import ch.example.adressen.model.Person;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Entity <-> DTO by MapStruct. The inverse side of a relation is not in the DTO, so mapping a DTO to
 * the entity leaves it unset (unmapped targets are ignored on purpose).
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {AddressMapper.class, CompanyMapper.class})
public interface PersonMapper {

    PersonDto toDto(Person entity);

    Person toEntity(PersonDto dto);
}
