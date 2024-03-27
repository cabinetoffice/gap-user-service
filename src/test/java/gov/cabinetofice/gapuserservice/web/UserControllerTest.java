package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.WebUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @InjectMocks
    private UserController controller;
    @Mock
    private OneLoginUserService oneLoginUserService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private CustomJwtServiceImpl customJwtService;

    @Mock
    private RoleService roleService;

    private static MockedStatic<WebUtils> mockedWebUtils;

    @BeforeEach
    void setUp() {
        mockedWebUtils = mockStatic(WebUtils.class);
        ReflectionTestUtils.setField(controller, "userServiceCookieName", "userServiceCookieName");
    }

    @AfterEach
    public void close() {
        mockedWebUtils.close();
    }

    @Test
    void updateRolesForUserId() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        UpdateUserRolesRequestDto updateUserRolesRequestDto = UpdateUserRolesRequestDto.builder()
                .newUserRoles(Arrays.asList(1, 2, 3, 4, 5)).build();

        mockedWebUtils.when(() -> WebUtils.getCookie(httpRequest, "userServiceCookieName"))
                .thenReturn(new Cookie("userServiceCookieName", "jwt"));
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(2).build()));
        final ResponseEntity<String> methodResponse = controller.updateRoles(httpRequest,updateUserRolesRequestDto, 1);

        assertThat(methodResponse).isEqualTo(ResponseEntity.ok("success"));
    }

    @Test
    void testSuperAdminCannotBlockThemselves() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        mockedWebUtils.when(() -> WebUtils.getCookie(httpRequest, "userServiceCookieName"))
                .thenReturn(new Cookie("userServiceCookieName", "jwt"));

        UpdateUserRolesRequestDto updateUserRolesRequestDto = UpdateUserRolesRequestDto.builder()
                .newUserRoles(new ArrayList<>()).build();

        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(1).build()));

        assertThrows(UnsupportedOperationException.class, () -> controller.updateRoles(httpRequest,
                updateUserRolesRequestDto, 1));
    }

    @Test
    void testSuperAdminThrowsInvalidRequestWithNoUser() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        UpdateUserRolesRequestDto updateUserRolesRequestDto = UpdateUserRolesRequestDto.builder()
                .newUserRoles(new ArrayList<>()).departmentId(1).build();

        mockedWebUtils.when(() -> WebUtils.getCookie(httpRequest, "userServiceCookieName"))
                .thenReturn(new Cookie("userServiceCookieName", "jwt"));

        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> controller.updateRoles(httpRequest,
                updateUserRolesRequestDto, 1));
    }

    @Test
    void shouldReturnUserWhenValidIdIsGiven() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        User mockUser = User.builder().sub("1").gapUserId(1)
                .roles(List.of(Role.builder()
                        .name(RoleEnum.FIND)
                        .description("desc").build()))
                .emailAddress("test@gov.uk").build();
        when(oneLoginUserService.getUserById(1)).thenReturn(mockUser);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        final ResponseEntity<UserAndRelationsDto> methodResponse = controller.getUserById(httpRequest, 1);

        assertThat(methodResponse.getBody()).isEqualTo(new UserAndRelationsDto(mockUser));
    }

    @Test
    void shouldReturnChangeDepartmentPageDtoWhenValidIdIsGiven() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        User mockUser = User.builder().sub("1").gapUserId(1)
                .roles(List.of(Role.builder()
                        .name(RoleEnum.FIND)
                        .description("desc").build()))
                .emailAddress("test@gov.uk").build();
        when(oneLoginUserService.getUserById(1)).thenReturn(mockUser);
        when(departmentService.getAllDepartments())
                .thenReturn(List.of(DepartmentDto.builder().id(1).name("dept").build()));
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);

        ResponseEntity<ChangeDepartmentPageDto> result = controller.getChangeDepartmentPage(httpRequest, 1);

        verify(oneLoginUserService, times(1)).getUserById(1);
        verify(departmentService, times(1)).getAllDepartments();
        assertThat(Objects.requireNonNull(result.getBody()).getDepartments().get(0).getId()).isEqualTo(1);
    }

    @Test
    void shouldDeleteUserWhenValidIdIsGiven() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        mockedWebUtils.when(() -> WebUtils.getCookie(httpRequest, "userServiceCookieName"))
                .thenReturn(new Cookie("userServiceCookieName", "jwt"));

        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(2).build()));
        final ResponseEntity<String> methodResponse = controller.deleteUser(httpRequest, 1);

        assertThat(methodResponse).isEqualTo(ResponseEntity.ok("success"));
    }

    @Test
    void shouldThrowErrorWhenAdminTriesToDeleteThemselves() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        mockedWebUtils.when(() -> WebUtils.getCookie(httpRequest, "userServiceCookieName"))
                .thenReturn(new Cookie("userServiceCookieName", "jwt"));

        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(1).build()));

        assertThrows(UnsupportedOperationException.class, () -> controller.deleteUser(httpRequest, 1));
    }

    @Test
    void shouldThrowErrorWhenUserIsEmpty() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        mockedWebUtils.when(() -> WebUtils.getCookie(httpRequest, "userServiceCookieName"))
                .thenReturn(new Cookie("userServiceCookieName", "jwt"));

        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> controller.deleteUser(httpRequest, 1));
    }


    @Test
    void testGetUserFromJwt() {
        User mockUser = User.builder().gapUserId(1).build();
        when(roleService.isSuperAdmin(any(HttpServletRequest.class)))
                .thenReturn(true);
        when(customJwtService.getUserFromJwt(any(HttpServletRequest.class)))
                .thenReturn(Optional.of(mockUser));
        final ResponseEntity<UserAndRelationsDto> methodResponse = controller.getUserFromJwt(mock(HttpServletRequest.class));

        assertThat(methodResponse.getBody()).isEqualTo(new UserAndRelationsDto(mockUser));
    }

    @Test
    void testGetUserFromJwtThrowsErrorWhenNotSuperAdmin() {
        Mockito.doThrow(ForbiddenException.class).when(roleService).isSuperAdmin(any(HttpServletRequest.class));
        assertThrows(ForbiddenException.class, () -> controller.getUserFromJwt(mock(HttpServletRequest.class)));
    }

    @Test
    void testGetUserFromJwtThrowsInvalidRequestWhenUserIsEmpty() {
        when(roleService.isSuperAdmin(any(HttpServletRequest.class)))
                .thenReturn(true);
        when(customJwtService.getUserFromJwt(any(HttpServletRequest.class)))
                .thenReturn(Optional.empty());
        assertThrows(InvalidRequestException.class, () -> controller.getUserFromJwt(mock(HttpServletRequest.class)));
    }

    @Test
    void updateDepartmentCantBeCalledOnApplicantAndFindUser() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        ChangeDepartmentDto changeDepartmentDto = new ChangeDepartmentDto(1);
        User mockUser = User.builder().sub("1").gapUserId(1)
                .roles(List.of(Role.builder()
                                .name(RoleEnum.FIND)
                                .description("desc").build(),
                        Role.builder()
                                .name(RoleEnum.APPLICANT)
                                .description("desc").build()))
                .emailAddress("test@test.com").build();
        when(oneLoginUserService.getUserById(1)).thenReturn(mockUser);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(oneLoginUserService.isUserApplicantAndFindOnly(mockUser)).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> controller.updateDepartment(httpRequest, 1, changeDepartmentDto));

    }

    @Test
    void testGetUserEmailsBySub() {
        User mockUser = User.builder().sub("1").gapUserId(1)
                .emailAddress("test1@test.com").build();
        User mockUser2 = User.builder().sub("2").gapUserId(2)
                .emailAddress("test2@test.com").build();

        List<String> userEmails = List.of(mockUser.getEmailAddress(), mockUser2.getEmailAddress());

        List<UserEmailDto> userEmailDtos = List.of(
                new UserEmailDto(mockUser.getEmailAddress().getBytes(), mockUser.getSub()),
                new UserEmailDto(mockUser2.getEmailAddress().getBytes(), mockUser2.getSub())
        );

        when(oneLoginUserService.getUserEmailsBySubs(userEmails)).thenReturn(userEmailDtos);
        final ResponseEntity<List<UserEmailDto>> methodResponse = controller.getUserEmailsBySubs(userEmails);

        assertThat(methodResponse.getBody()).isEqualTo(
                List.of(new UserEmailDto(mockUser.getEmailAddress().getBytes(), mockUser.getSub()),
                        new UserEmailDto(mockUser2.getEmailAddress().getBytes(), mockUser2.getSub()))
        );
    }

    @Test
    void testUserEmailsFromSubs() {
        User mockUser = User.builder().sub("1").gapUserId(1)
                .emailAddress("test1@test.com").build();
        User mockUser2 = User.builder().sub("2").gapUserId(2)
                .emailAddress("test2@test.com").build();

        List<String> userEmails = List.of(mockUser.getEmailAddress(), mockUser2.getEmailAddress());
        UserSubsRequestDto userSubsRequestDto = new UserSubsRequestDto(userEmails);

        List<UserEmailDto> userEmailDtos = List.of(
                new UserEmailDto(mockUser.getEmailAddress().getBytes(), mockUser.getSub()),
                new UserEmailDto(mockUser2.getEmailAddress().getBytes(), mockUser2.getSub())
        );

        when(oneLoginUserService.getUserEmailsBySubs(userEmails)).thenReturn(userEmailDtos);
        final ResponseEntity<List<UserEmailDto>> methodResponse = controller.getUserEmailsFromSubs(userSubsRequestDto);

        assertThat(methodResponse.getBody()).isEqualTo(
                List.of(new UserEmailDto(mockUser.getEmailAddress().getBytes(), mockUser.getSub()),
                        new UserEmailDto(mockUser2.getEmailAddress().getBytes(), mockUser2.getSub()))
        );
    }

    @Test
    void testGetUserByEmailWhenARoleIsSpecified() {
        String testEmail = "test@test.com";
        Role roles = Role.builder().id(1).name(RoleEnum.ADMIN).build();
        User mockUser = User.builder().sub("1").gapUserId(1).roles(List.of(roles))
                .emailAddress(testEmail).build();
        UserDto mockUserDto = new UserDto(mockUser);

        when(oneLoginUserService.getUserByEmailAndRole(testEmail, "ADMIN")).thenReturn(mockUser);
        final ResponseEntity<UserDto> methodResponse = controller.getUserByEmail(testEmail, Optional.of("ADMIN"));

        assertThat(methodResponse.getBody()).isEqualTo(mockUserDto);
    }

    @Test
    void testGetUserByEmailWhenRoleIsAbsent() {
        String testEmail = "test@test.com";
        User mockUser = User.builder().sub("1").gapUserId(1)
                .emailAddress(testEmail).build();

        UserDto mockUserDto = new UserDto(mockUser);

        when(oneLoginUserService.getUserByEmail(testEmail)).thenReturn(mockUser);
        final ResponseEntity<UserDto> methodResponse = controller.getUserByEmail(testEmail, Optional.empty());

        assertThat(methodResponse.getBody()).isEqualTo(mockUserDto);
    }

    @Test
    void testGetEmailFromSub() {
        String userSub = "1";
        String userEmail = "test@test.com";
        User mockUser = User.builder().sub(userSub).gapUserId(1)
                .emailAddress(userEmail).build();
        when(oneLoginUserService.getUserBySub(userSub)).thenReturn(mockUser);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        final ResponseEntity<String> methodResponse = controller.getEmailFromSub(mockRequest, userSub);

        assertThat(methodResponse.getBody()).isEqualTo(userEmail);
        verify(oneLoginUserService, times(1)).getUserBySub(userSub);
    }

    @Test
    void testGetEmailFromSub_UserNotFound() {
        String userSub = "2";
        when(oneLoginUserService.getUserBySub(userSub)).thenThrow(UserNotFoundException.class);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        assertThrows(UserNotFoundException.class, () -> {
            controller.getEmailFromSub(mockRequest, userSub);
        });
        verify(oneLoginUserService, times(1)).getUserBySub(userSub);
    }
}