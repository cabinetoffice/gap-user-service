package gov.cabinetoffice.gapuserservice.mappers;

import gov.cabinetoffice.gapuserservice.dto.RoleDto;
import gov.cabinetoffice.gapuserservice.model.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto roleToRoleDto(Role role);
}
