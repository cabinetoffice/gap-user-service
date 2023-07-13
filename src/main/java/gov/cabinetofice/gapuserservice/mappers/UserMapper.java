package gov.cabinetofice.gapuserservice.mappers;

import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto userTouserDto(User user);
}