package gov.cabinetofice.gapuserservice.mappers;
import gov.cabinetofice.gapuserservice.model.Role;

import gov.cabinetofice.gapuserservice.dto.RoleDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto roleToRoleDto(Role role);
}