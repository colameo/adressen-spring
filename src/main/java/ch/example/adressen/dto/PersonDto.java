package ch.example.adressen.dto;

import ch.example.adressen.model.PersonStatus;
import ch.example.adressen.validation.ValidEmail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The image of the entity Person as the API shows it: the same members, a reference or a part as a nested DTO. */
// @NoArgsConstructor for Jackson and MapStruct, @AllArgsConstructor because @Builder needs it next to it.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonDto {

    private Long id;

    @Size(min = 1, max = 80)
    private String firstName;

    @Size(min = 1, max = 80)
    private String lastName;

    @ValidEmail
    private String email;

    @Pattern(regexp = "\\+?[0-9 ()/.-]{6,20}")
    private String phone;

    private LocalDate birthday;

    private PersonStatus status;

    @Valid
    private AddressDto address;

    private CompanyDto company;
}
