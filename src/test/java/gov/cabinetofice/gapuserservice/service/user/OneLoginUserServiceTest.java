package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.exceptions.*;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.DepartmentRepository;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.encryption.AwsEncryptionServiceImpl;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneLoginUserServiceTest {

    @InjectMocks
    private OneLoginUserService oneLoginUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private AwsEncryptionServiceImpl awsEncryptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oneLoginUserService, "adminBackend", "adminBackend");
    }

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
    void shouldReturnUserWhenValidIdIsGiven() {

        User mockedUser = User.builder().gapUserId(1).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(mockedUser));
        User result = oneLoginUserService.getUserById(1);

        assertThat(result)
            .isNotNull()
            .isEqualTo(mockedUser);
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void shouldReturnUserWhenValidSubIsGiven() {
            User mockedUser = User.builder().gapUserId(1).build();
            when(userRepository.findBySub("1234")).thenReturn(Optional.of(mockedUser));
            User result = oneLoginUserService.getUserByUserSub("1234");

            assertThat(result)
                .isEqualTo(mockedUser);
            verify(userRepository, times(1)).findBySub("1234");
    }

    @Test
    void shouldReturnUserWhenValidColaSubIsGiven() {

        UUID uuid = UUID.fromString("f1da81d1-375f-4693-b52e-60f38a253fc9");

        User mockedUser = User.builder().gapUserId(1).build();
        when(userRepository.findBySub("f1da81d1-375f-4693-b52e-60f38a253fc9")).thenReturn(Optional.empty());
        when(userRepository.findByColaSub(uuid)).thenReturn(Optional.of(mockedUser));
        User result = oneLoginUserService.getUserByUserSub("f1da81d1-375f-4693-b52e-60f38a253fc9");

        assertThat(result)
            .isEqualTo(mockedUser);
        verify(userRepository, times(1)).findBySub("f1da81d1-375f-4693-b52e-60f38a253fc9");
        verify(userRepository, times(1)).findByColaSub(uuid);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenInValidSubIsGiven() {

        UUID uuid = UUID.fromString("f1da81d1-375f-4693-b52e-60f38a253fc9");

        when(userRepository.findBySub("f1da81d1-375f-4693-b52e-60f38a253fc9")).thenReturn(Optional.empty());
        when(userRepository.findByColaSub(uuid)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                oneLoginUserService.getUserByUserSub("f1da81d1-375f-4693-b52e-60f38a253fc9"));
        verify(userRepository, times(1)).findBySub("f1da81d1-375f-4693-b52e-60f38a253fc9");
        verify(userRepository, times(1)).findByColaSub(uuid);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenInValidUUIDSubIsGiven() {

        when(userRepository.findBySub("1234")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                oneLoginUserService.getUserByUserSub("1234"));
        verify(userRepository, times(1)).findBySub("1234");
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenInValidIdIsGiven() {
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> oneLoginUserService.getUserById(100));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenInValidSubIsGiven() {
        when(userRepository.findBySub(anyString())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> oneLoginUserService.getUserBySub("100"));
    }

    @Nested
    class getPaginatedUsers {
        @Test
        void noEmailDepartmentsOrRole() {
            User user1 = User.builder().gapUserId(1).build();
            User user2 = User.builder().gapUserId(2).build();
            List<User> users = Arrays.asList(user1, user2);
            Pageable pageable = mock(Pageable.class);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(Collections.emptyList(), Collections.emptyList(), "");

            when(userRepository.findByOrderByEmail(pageable)).thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> resultContent = pageResult.getContent();
            assertEquals(users.size(), resultContent.size());
            assertEquals(user1, resultContent.get(0));
            assertEquals(user2, resultContent.get(1));
            verify(userRepository, times(1)).findByOrderByEmail(pageable);
        }

        @Test
        void hasEmail_noDepartmentsOrRoles() {
            User user1 = User.builder().gapUserId(1).build();
            User user2 = User.builder().gapUserId(2).build();
            List<User> users = Arrays.asList(user1, user2);
            String emailAddress = "baz";
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(Collections.emptyList(), Collections.emptyList(), emailAddress);

            when(userRepository.findUsersByFuzzyEmail(emailAddress, pageable))
                    .thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> result = pageResult.getContent();
            assertEquals(2, result.size());
            assertEquals(user1, result.get(0));
            assertEquals(user2, result.get(1));
            verify(userRepository, times(1)).findUsersByFuzzyEmail(emailAddress, pageable);
        }

        @Test
        void hasDepartments_noEmailOrRoles() {
            User user1 = User.builder().gapUserId(1).build();
            User user2 = User.builder().gapUserId(2).build();
            List<User> users = Arrays.asList(user1, user2);
            List<Integer> departmentIds = Arrays.asList(1, 2);
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(departmentIds, Collections.emptyList(), "");

            when(userRepository.findUsersByDepartment(departmentIds, pageable))
                    .thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> result = pageResult.getContent();
            assertEquals(2, result.size());
            assertEquals(user1, result.get(0));
            assertEquals(user2, result.get(1));
            verify(userRepository, times(1)).findUsersByDepartment(departmentIds, pageable);
        }

        @Test
        void hasRoles_NoDepartmentsOrEmail() {
            User user1 = User.builder().gapUserId(1).build();
            User user2 = User.builder().gapUserId(2).build();
            List<User> users = Arrays.asList(user1, user2);
            List<Integer> roleIds = Arrays.asList(1, 2);
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(Collections.emptyList(), roleIds, "");

            when(userRepository.findUsersByRoles(roleIds, pageable))
                    .thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> result = pageResult.getContent();
            assertEquals(2, result.size());
            assertEquals(user1, result.get(0));
            assertEquals(user2, result.get(1));
            verify(userRepository, times(1)).findUsersByRoles(roleIds, pageable);
        }

        @Test
        void hasEmailAndDepartment_NoRoles() {
            User user1 = User.builder().gapUserId(1).build();
            User user2 = User.builder().gapUserId(2).build();
            List<User> users = Arrays.asList(user1, user2);
            List<Integer> departmentIds = Arrays.asList(1, 2);
            String emailAddress = "example";
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(departmentIds, Collections.emptyList(), emailAddress);

            when(userRepository.findUsersByDepartmentAndFuzzyEmail(departmentIds, emailAddress, pageable))
                    .thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> result = pageResult.getContent();
            assertEquals(2, result.size());
            assertEquals(user1, result.get(0));
            assertEquals(user2, result.get(1));
            verify(userRepository, times(1)).findUsersByDepartmentAndFuzzyEmail(departmentIds, emailAddress, pageable);
        }

        @Test
        void hasEmailAndRoles_NoDepartments() {
            User user1 = User.builder().gapUserId(1).emailAddress("user1@example.com").build();
            User user2 = User.builder().gapUserId(2).emailAddress("user2@example.com").build();
            List<User> users = Arrays.asList(user1, user2);
            List<Integer> roleIds = Arrays.asList(3, 4);
            String emailAddress = "example";
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(Collections.emptyList(), roleIds, emailAddress);

            when(userRepository.findUsersByRolesAndFuzzyEmail(roleIds, emailAddress, pageable))
                    .thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> result = pageResult.getContent();
            assertEquals(2, result.size());
            assertEquals(user1, result.get(0));
            assertEquals(user2, result.get(1));
            verify(userRepository, times(1)).findUsersByRolesAndFuzzyEmail(roleIds, emailAddress, pageable);
        }

        @Test
        void hasRolesAndDepartment_NoEmail() {
            User user1 = User.builder().gapUserId(1).build();
            User user2 = User.builder().gapUserId(2).build();
            List<User> users = Arrays.asList(user1, user2);
            List<Integer> departmentIds = Arrays.asList(1, 2);
            List<Integer> roleIds = Arrays.asList(3, 4);
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(departmentIds, roleIds, "");

            when(userRepository.findUsersByDepartmentAndRoles(roleIds, departmentIds, pageable))
                    .thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> result = pageResult.getContent();
            assertEquals(2, result.size());
            assertEquals(user1, result.get(0));
            assertEquals(user2, result.get(1));
            verify(userRepository, times(1)).findUsersByDepartmentAndRoles(roleIds, departmentIds, pageable);
        }

        @Test
        void hasEmailDepartmentAndRoles() {
            User user1 = User.builder().gapUserId(1).emailAddress("user1@example.com").build();
            User user2 = User.builder().gapUserId(2).emailAddress("user2@example.com").build();
            List<User> users = Arrays.asList(user1, user2);
            List<Integer> departmentIds = Arrays.asList(1, 2);
            List<Integer> roleIds = Arrays.asList(3, 4);
            String emailAddress = "example";
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());
            UserQueryDto userQueryDto = new UserQueryDto(departmentIds, roleIds, emailAddress);

            when(userRepository.findUsersByDepartmentAndRolesAndFuzzyEmail(roleIds, departmentIds ,emailAddress, pageable))
                    .thenReturn(userPage);

            Page<User> pageResult = oneLoginUserService.getPaginatedUsers(pageable, userQueryDto);

            List<User> result = pageResult.getContent();
            assertEquals(2, result.size());
            assertEquals(user1, result.get(0));
            assertEquals(user2, result.get(1));
            verify(userRepository, times(1)).findUsersByDepartmentAndRolesAndFuzzyEmail(roleIds, departmentIds, emailAddress, pageable);
        }
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

    @Test
    void testDeleteUser_UserNotFound(){
        Integer userId = 1;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> oneLoginUserService.deleteUser(userId, "jwt"));
    }

    @Test
    void testDeleteUser_DeletesUser(){
        Integer userId = 1;
        User user = User.builder().gapUserId(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        final WebClient webClient = mock(WebClient.class);
        when(webClientBuilder.build()).thenReturn(webClient);
        final WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        final WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        final WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.when());

        oneLoginUserService.deleteUser(userId, "jwt");

        verify(userRepository).deleteById(userId);
    }

    @Test
    void updateRolesShouldAddApplicantAndFindRolesWhenNoRolesPresent() {
        Integer userId = 1;
        User user = User.builder().gapUserId(userId).build();
        List<Integer> newRoles = List.of(3, 4);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(3)).thenReturn(Optional.of(Role.builder().name(RoleEnum.ADMIN).build()));
        when(roleRepository.findById(4)).thenReturn(Optional.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build()));
        when(roleRepository.findByName(RoleEnum.APPLICANT)).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));
        when(roleRepository.findByName(RoleEnum.FIND)).thenReturn(Optional.of(Role.builder().name(RoleEnum.FIND).build()));
        User updatedUser = oneLoginUserService.updateRoles(userId , newRoles);

        assertThat(updatedUser.getRoles()).hasSize(4);
        assertThat(updatedUser.getRoles().stream().anyMatch(role -> role.getName().equals(RoleEnum.APPLICANT))).isTrue();
        assertThat(updatedUser.getRoles().stream().anyMatch(role -> role.getName().equals(RoleEnum.FIND))).isTrue();
    }

    @Test
    void testValidateRolesWhenRolesMatch() {
        Role find = Role.builder().name(RoleEnum.FIND).id(1).build();
        Role applicant = Role.builder().name(RoleEnum.APPLICANT).id(2).build();
        String testPayloadRoles = "[FIND, APPLICANT]";
        List<Role> testUserRoles = List.of(find,applicant);
        when(roleMapper.roleToRoleDto(find)).thenReturn(RoleDto.builder().name("FIND").build());
        when(roleMapper.roleToRoleDto(applicant)).thenReturn(RoleDto.builder().name("APPLICANT").build());

        assertDoesNotThrow(() -> oneLoginUserService.validateRoles(testUserRoles, testPayloadRoles));
    }

    @Test
    void testValidateRolesWhenPayloadHasExtraRoles__userWasUnblocked() {
        Role find = Role.builder().name(RoleEnum.FIND).id(1).build();
        Role applicant = Role.builder().name(RoleEnum.APPLICANT).id(2).build();
        String testPayloadRoles = "[]";
        List<Role> testUserRoles = List.of(find,applicant);
        when(roleMapper.roleToRoleDto(find)).thenReturn(RoleDto.builder().name("FIND").build());
        when(roleMapper.roleToRoleDto(applicant)).thenReturn(RoleDto.builder().name("APPLICANT").build());

        assertThrows(UnauthorizedException.class, () -> oneLoginUserService.validateRoles(testUserRoles, testPayloadRoles));
    }

    @Test
    void testValidateRolesWhenPayloadHasMissingRoles() {
        Role find = Role.builder().name(RoleEnum.FIND).id(1).build();
        Role applicant = Role.builder().name(RoleEnum.APPLICANT).id(2).build();
        String testPayloadRoles = "[FIND]";
        List<Role> testUserRoles = List.of(find,applicant);
        when(roleMapper.roleToRoleDto(find)).thenReturn(RoleDto.builder().name("FIND").build());
        when(roleMapper.roleToRoleDto(applicant)).thenReturn(RoleDto.builder().name("APPLICANT").build());

        assertDoesNotThrow(() -> oneLoginUserService.validateRoles(testUserRoles, testPayloadRoles));
    }

    @Test
    void testValidateRolesWhenUserHasNoRoles() {
        String testPayloadRoles = "[FIND, APPLICANT]";
        List<Role> testUserRoles = List.of();

        assertThrows(UnauthorizedException.class, () -> oneLoginUserService.validateRoles(testUserRoles, testPayloadRoles));
    }

    @Test
    void testValidateSessionsRolesThrowsInvalidExceptionWithEmptyUser() {
        String  email = "email";
        String roles = "APPLICANT";
        when(userRepository.findByEmailAddress(email)).thenReturn(Optional.empty());
        assertThrows(InvalidRequestException.class, () -> oneLoginUserService.validateSessionsRoles(email, roles));
    }

    @Test
    void updateRolesShouldSetDepartmentToNullIfNotSuperAdminOrAdmin() {
        Integer userId = 1;
        List<Integer> newRoles = List.of(1, 2);
        User user = User.builder().gapUserId(userId).department(Department.builder().name("test").build()).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1)).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));
        when(roleRepository.findById(2)).thenReturn(Optional.of(Role.builder().name(RoleEnum.FIND).build()));
        User updatedUser = oneLoginUserService.updateRoles(1 , newRoles);

        assertThat(updatedUser.getDepartment()).isNull();
    }

    @Test
    void updateRolesShouldNotSetDepartmentToNullIfUserHasMoreThanTwoRoles() {
        Integer userId = 1;
        List<Integer> newRoles = List.of(1, 2, 3);
        User user = User.builder().gapUserId(userId).department(Department.builder().name("test").build()).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1)).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));
        when(roleRepository.findById(2)).thenReturn(Optional.of(Role.builder().name(RoleEnum.FIND).build()));
        when(roleRepository.findById(3)).thenReturn(Optional.of(Role.builder().name(RoleEnum.ADMIN).build()));
        User updatedUser = oneLoginUserService.updateRoles(1 , newRoles);

        assertThat(updatedUser.getDepartment()).isNotNull();
    }

    @Test
    void updateRolesShouldNotSetDepartmentToNullIfUserIsAdminOrSuperAdmin() {
        Integer userId = 1;
        List<Integer> newRoles = List.of( 3, 4);
        User user = User.builder().gapUserId(userId).department(Department.builder().name("test").build()).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(3)).thenReturn(Optional.of(Role.builder().name(RoleEnum.ADMIN).build()));
        when(roleRepository.findById(4)).thenReturn(Optional.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build()));
        when(roleRepository.findByName(RoleEnum.FIND)).thenReturn(Optional.of(Role.builder().name(RoleEnum.FIND).id(1).build()));
        when(roleRepository.findByName(RoleEnum.APPLICANT)).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).id(2).build()));
        User updatedUser = oneLoginUserService.updateRoles(1 , newRoles);

        assertThat(updatedUser.getDepartment()).isNotNull();
    }

    @Test
    void shouldReturnNewUserRoles() {
        final List<RoleEnum> result = oneLoginUserService.getNewUserRoles();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(RoleEnum.APPLICANT, result.get(0));
        Assertions.assertEquals(RoleEnum.FIND, result.get(1));
    }

    @Nested
    class CreateUserTest {
        @Test
        void shouldReturnSavedUser() {
            when(roleRepository.findByName(any())).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));
            when(userRepository.save(any())).thenReturn(User.builder().roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build())).build());

            final User result = oneLoginUserService.createNewUser("", "");

            Assertions.assertEquals(1, result.getRoles().size());
            Assertions.assertEquals(RoleEnum.APPLICANT, result.getRoles().get(0).getName());
        }

        @Test
        void shouldSaveUserWithSubAndEmailWhenUserIsCreated() {
            when(roleRepository.findByName(any())).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));

            oneLoginUserService.createNewUser("sub", "test@email.com");

            final ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userArgumentCaptor.capture());
            Assertions.assertEquals("sub", userArgumentCaptor.getValue().getSub());
            Assertions.assertEquals("test@email.com", userArgumentCaptor.getValue().getEmailAddress());
        }

        @Test
        void shouldThrowExceptionWhenRoleDoesNotExist() {
            when(roleRepository.findByName(any())).thenReturn(Optional.empty());

            assertThrows(RoleNotFoundException.class, () -> oneLoginUserService.createNewUser("", ""));
        }
    }

    @Nested
    class SetUsersLoginJourneyStateTest {
        @Test
        void shouldSetUsersLoginJourneyState() {
            final User user = User.builder()
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .build();

            oneLoginUserService.setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);

            final ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userArgumentCaptor.capture());
            AssertionsForClassTypes.assertThat(userArgumentCaptor.getValue().getLoginJourneyState()).isEqualTo(LoginJourneyState.USER_READY);
        }
    }

    @Nested
    class CreateOrGetUserFromInfoTest {
        @Test
        void getExistingUser() {
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .sub("sub")
                    .emailAddress("email")
                    .build();
            final User existingUser = User.builder().sub("sub").build();

            when(userRepository.findBySub(any())).thenReturn(Optional.of(existingUser));

            final User result = oneLoginUserService.createOrGetUserFromInfo(oneLoginUserInfoDto);

            Assertions.assertEquals(existingUser, result);
        }

        @Test
        void createNewUser() {
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .sub("sub")
                    .emailAddress("email")
                    .build();

            when(userRepository.findBySub(any())).thenReturn(Optional.empty());
            when(roleRepository.findByName(any())).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));

            final User newUser = oneLoginUserService.createNewUser("sub", "email");
            final User result = oneLoginUserService.createOrGetUserFromInfo(oneLoginUserInfoDto);

            Assertions.assertEquals(newUser, result);
        }

        @Test
        void shouldGetUserEmailsFromSubsAndEncryptThem() {
            final List<String> subs = List.of("sub1", "sub2");
            final List<UserEmailDto> encryptedUserEmailDtos = List.of(
                    new UserEmailDto("encrypted1".getBytes(), "sub1"),
                    new UserEmailDto("encrypted2".getBytes(), "sub2")
            );

            when(userRepository.findBySubIn(subs)).thenReturn(
                    List.of(
                            User.builder().sub("sub1").emailAddress("unencrypted1").build(),
                            User.builder().sub("sub2").emailAddress("unencrypted2").build()
                    )
            );
            when(awsEncryptionService.encryptField("unencrypted1")).thenReturn("encrypted1".getBytes());
            when(awsEncryptionService.encryptField("unencrypted2")).thenReturn("encrypted2".getBytes());
            List<UserEmailDto> returnedList = oneLoginUserService.getUserEmailsBySubs(subs);
            assertThat(returnedList).isEqualTo(encryptedUserEmailDtos);
        }

    }



    @Nested
    class MigrateApplyUserTest {
        @Test
        void shouldMigrateUser() {
            final User user = User.builder()
                    .colaSub(UUID.randomUUID())
                    .sub("oneLoginSub")
                    .build();
            final MigrateUserDto migrateUserDto = MigrateUserDto.builder()
                    .oneLoginSub(user.getSub())
                    .colaSub(user.getColaSub())
                    .build();

            // TODO not sure how to spy/mock the builder pattern well. If anyone has a better way gimme a shout!
            final WebClient mockWebClient = mock(WebClient.class);
            final WebClient.RequestBodyUriSpec mockRequestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
            final WebClient.RequestBodySpec mockRequestBodySpec = mock(WebClient.RequestBodySpec.class);
            final WebClient.RequestHeadersSpec mockRequestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
            final WebClient.ResponseSpec mockResponseSpec = mock(WebClient.ResponseSpec.class);

            when(webClientBuilder.build()).thenReturn(mockWebClient);
            when(mockWebClient.patch()).thenReturn(mockRequestBodyUriSpec);
            when(mockRequestBodyUriSpec.uri(anyString())).thenReturn(mockRequestBodySpec);
            when(mockRequestBodySpec.header(anyString(), anyString())).thenReturn(mockRequestBodySpec);
            when(mockRequestBodySpec.contentType(any())).thenReturn(mockRequestBodySpec);
            when(mockRequestBodySpec.bodyValue(any())).thenReturn(mockRequestHeadersSpec);
            when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
            when(mockResponseSpec.bodyToMono(Void.class)).thenReturn(mock(Mono.class));

            oneLoginUserService.migrateApplyUser(user, "jwt");

            verify(webClientBuilder).build();
            verify(mockRequestBodyUriSpec).uri("adminBackend/users/migrate");
            verify(mockRequestBodySpec).header("Authorization", "Bearer jwt");
            verify(mockRequestBodySpec).bodyValue(migrateUserDto);
        }
    }

    @Test
    void getUserByEmailReturnsUser() {
        User user = User.builder().emailAddress("test@test.com").build();
        when(userRepository.findByEmailAddress("test@test.com")).thenReturn(Optional.of(user));

        User result = oneLoginUserService.getUserByEmail("test@test.com");
        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserByEmailThrowsWhenNoCorrespondingUserFound() {
        when(userRepository.findByEmailAddress("test@test.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> oneLoginUserService.getUserByEmail("test@test.com"));
    }

    @Test
    void getUserByEmailAndRoleReturnsUser() {
        User user = User.builder().emailAddress("test@test.com").roles(List.of(Role.builder().id(1).name(RoleEnum.ADMIN).build())).build();
        when(roleRepository.findById(1)).thenReturn(Optional.of(Role.builder().id(1).name(RoleEnum.ADMIN).build()));
        when(userRepository.findByEmailAddressAndRolesId("test@test.com", 1)).thenReturn(Optional.of(user));

        User result = oneLoginUserService.getUserByEmailAndRole("test@test.com", Role.builder().id(1).name(RoleEnum.ADMIN).build());
        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserByEmailAndRoleThrowsExceptionWhenInvalidRole() {
        when(roleRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RoleNotFoundException.class, () -> oneLoginUserService.getUserByEmailAndRole("test@test.com", Role.builder().id(1).name(RoleEnum.ADMIN).build()));
    }
}



