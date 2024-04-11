package gov.cabinetoffice.gapuserservice.mappers;

import gov.cabinetoffice.gapuserservice.dto.DepartmentDto;
import gov.cabinetoffice.gapuserservice.model.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    DepartmentDto departmentToDepartmentDto(Department dept);
}
