package gov.cabinetofice.gapuserservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserQueryDto {
    private List<Integer> departmentIds;
    private List<Integer> roleIds;
    private String email;

    // Constructors, getters, and setters

    // You can also add convenience methods if needed
}

