package gov.cabinetoffice.gapuserservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuperAdminDashboardPageRequestDto {
    private String departments;
    private String roles;
    @Size(max = 255, message = "Search term cannot exceed 255 characters")
    private String searchTerm;
    private boolean clearAllFilters;
}
