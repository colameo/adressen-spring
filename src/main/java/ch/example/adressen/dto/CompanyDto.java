package ch.example.adressen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The image of the entity Company as the API shows it: the same members, a reference or a part as a nested DTO. */
// @NoArgsConstructor for Jackson and MapStruct, @AllArgsConstructor because @Builder needs it next to it.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {

    private Long id;

    private String name;
}
