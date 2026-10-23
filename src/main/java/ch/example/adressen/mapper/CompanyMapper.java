package ch.example.adressen.mapper;

import ch.example.adressen.dto.CompanyDto;
import ch.example.adressen.model.Company;
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
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompanyMapper {

    CompanyDto toDto(Company entity);

    Company toEntity(CompanyDto dto);
}
