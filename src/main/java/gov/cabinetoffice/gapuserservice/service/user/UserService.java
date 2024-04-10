package gov.cabinetoffice.gapuserservice.service.user;

import gov.cabinetoffice.gapuserservice.dto.CreateUserDto;

public interface UserService {
    boolean doesUserExist(String email);

    void createNewUser(CreateUserDto createUserDto);
}
