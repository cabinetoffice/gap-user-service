package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.dto.CreateUserDto;

public interface UserService {
    boolean doesUserExist(String email);

    void createNewUser(CreateUserDto createUserDto);
}
