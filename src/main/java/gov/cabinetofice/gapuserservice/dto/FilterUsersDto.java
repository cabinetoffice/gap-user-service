package gov.cabinetofice.gapuserservice.dto;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Data
@Getter
@Setter
public class FilterUsersDto {
    private ArrayList<String> department;
    private ArrayList<String> role;
    private boolean clearAllFilters;
    private String searchTerm;
}
