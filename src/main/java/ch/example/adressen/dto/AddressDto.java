package ch.example.adressen.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The image of the entity Address as the API shows it: the same members, a reference or a part as a nested DTO. */
// @NoArgsConstructor for Jackson and MapStruct, @AllArgsConstructor because @Builder needs it next to it.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private Long id;

    private String street;

    @Pattern(regexp = "[0-9]{4}")
    private String zip;

    private String city;
}
