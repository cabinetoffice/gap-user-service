package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.DepartmentRepository;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OneLoginUserServiceTest {

    @InjectMocks
    private OneLoginUserService oneLoginUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoleRepository roleRepository;

    @Test
    void shouldReturnUpdatedUserWhenValidIdAndRolesAreGiven() {
        Integer userId = 1;
        List<Integer> newRoles = List.of(1, 2);
        List<Role> currentUserRoles = List.of(Role.builder().name(RoleEnum.FIND).id(1).build());
        User user = spy(User.builder().gapUserId(1).sub("sub").roles(currentUserRoles).build());
        Role role1 = Role.builder().id(1).name(RoleEnum.FIND).description("a desc").build();
        Role role2 = Role.builder().id(2).name(RoleEnum.APPLICANT).description("a desc 2").build();

        doNothing().when(user).removeAllRoles();
        doNothing().when(user).addRole(role2);
        doNothing().when(user).addRole(role1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1)).thenReturn(Optional.of(role1));
        when(roleRepository.findById(2)).thenReturn(Optional.of(role2));
        when(roleRepository.findByName(RoleEnum.APPLICANT)).thenReturn(Optional.of(role2));

        User updatedUser = oneLoginUserService.updateRoles(1, newRoles);

        Mockito.verify(roleRepository, times(2)).findById(anyInt());
        Mockito.verify(roleRepository, times(1)).findByName(any(RoleEnum.class));
        Mockito.verify(userRepository, times(1)).save(user);
        assertThat(user).isEqualTo(updatedUser);
    }

    @Test
    void shouldCallUserRepository() {
        oneLoginUserService.getUserCount();
        verify(userRepository, times(1)).count();
    }

    @Test
    void shouldReturnUserWhenValidIdIsGiven() {

        User mockedUser = User.builder().gapUserId(1).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(mockedUser));
        User result = oneLoginUserService.getUserById(1);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockedUser);
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenInValidIdIsGiven() {
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> oneLoginUserService.getUserById(100));
    }

    @Test
    void shouldReturnPaginatedUsers() {
        User user1 = User.builder().gapUserId(1).build();
        User user2 = User.builder().gapUserId(2).build();
        List<User> users = Arrays.asList(user1, user2);

        Pageable pageable = mock(Pageable.class);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAll(pageable)).thenReturn(userPage);


        UserDto userDto1 = new UserDto(user1);
        UserDto userDto2 = new UserDto(user2);

        List<UserDto> result = oneLoginUserService.getPaginatedUsers(pageable, "");

        assertEquals(2, result.size());
        assertEquals(userDto1, result.get(0));
        assertEquals(userDto2, result.get(1));
    }

    @Test
    void shouldFuzzySearchAndReturnPaginatedUsers() {
        User user1 = User.builder().gapUserId(1).build();
        User user2 = User.builder().gapUserId(2).build();
        List<User> users = Arrays.asList(user1, user2);
        String emailAddress = "baz";

//        when(userRepository.findAllUsersByFuzzySearchOnEmailAddress(emailAddress)).thenReturn(users);

        Pageable pageable = PageRequest.of(0, 10);
        UserDto userDto1 = new UserDto(user1);
        UserDto userDto2 = new UserDto(user2);

        List<UserDto> result = oneLoginUserService.getPaginatedUsers(pageable, emailAddress);

        assertEquals(2, result.size());
        assertEquals(userDto1, result.get(0));
        assertEquals(userDto2, result.get(1));
    }

    @Test
    void shouldReturnUpdatedUserWhenValidUserAndDepartmentIsGiven() {
        User user = User.builder().gapUserId(1).build();
        Department department = new Department();
        Integer userId = 1;
        Integer departmentId = 2;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(userRepository.save(user)).thenReturn(user);
        User result = oneLoginUserService.updateDepartment(userId, departmentId);

        assertEquals(department, user.getDepartment());
        verify(userRepository).save(user);
        assertEquals(user, result);
    }

    @Test
    void testUpdateDepartment_UserNotFound() {
        Integer userId = 1;
        Integer departmentId = 2;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> oneLoginUserService.updateDepartment(userId, departmentId));
    }

    @Test
    void testUpdateDepartment_DepartmentNotFound() {
        User user = new User();
        Integer userId = 1;
        Integer departmentId = 2;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> oneLoginUserService.updateDepartment(userId, departmentId));
    }

}
