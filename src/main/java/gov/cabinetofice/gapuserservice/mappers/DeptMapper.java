package gov.cabinetofice.gapuserservice.mappers;

import gov.cabinetofice.gapuserservice.dto.DeptDto;
import gov.cabinetofice.gapuserservice.model.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeptMapper {
    DeptDto deptToDeptDto(Department dept);
}
