package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.ChangeDepartmentPageDto;
import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "userServiceCookieName", "userServiceCookieName");
    }

    @Test
    void updateRolesForUserId() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(2).build()));
        final ResponseEntity<String> methodResponse = controller.updateRoles(httpRequest, List.of(1,2), 1);

        assertThat(methodResponse).isEqualTo(ResponseEntity.ok("success"));
    }

    @Test
    void testSuperAdminCannotBlockThemselves() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(1).build()));

        assertThrows(UnsupportedOperationException.class, () -> controller.updateRoles(httpRequest, List.of(), 1));
    }

    @Test
    void testSuperAdminThrowsInvalidRequestWithNoUser() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> controller.updateRoles(httpRequest, List.of(), 1));
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
        final ResponseEntity<UserDto> methodResponse = controller.getUserById(httpRequest, 1);

        assertThat(methodResponse.getBody()).isEqualTo(new UserDto(mockUser));
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
        when(httpRequest.getCookies()).thenReturn(new Cookie[] {new Cookie("userServiceCookieName", "1")});
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(2).build()));
        final ResponseEntity<String> methodResponse = controller.deleteUser(httpRequest, 1);

        assertThat(methodResponse).isEqualTo(ResponseEntity.ok("success"));
    }

    @Test
    void shouldThrowErrorWhenAdminTriesToDeleteThemselves() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getCookies()).thenReturn(new Cookie[] {new Cookie("userServiceCookieName", "1")});
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(User.builder().gapUserId(1).build()));

        assertThrows(UnsupportedOperationException.class, () -> controller.deleteUser(httpRequest, 1));
    }

    @Test
    void shouldThrowErrorWhenUserIsEmpty() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getCookies()).thenReturn(new Cookie[] {new Cookie("userServiceCookieName", "1")});
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> controller.deleteUser(httpRequest, 1));
    }


    @Test
    public void testGetUserFromJwt() {
        User mockUser = User.builder().gapUserId(1).build();
        when(roleService.isSuperAdmin(any(HttpServletRequest.class)))
                .thenReturn(true);
        when(customJwtService.getUserFromJwt(any(HttpServletRequest.class)))
                .thenReturn(Optional.of(mockUser));
        final ResponseEntity<UserDto> methodResponse = controller.getUserFromJwt(mock(HttpServletRequest.class));

        assertThat(methodResponse.getBody()).isEqualTo(new UserDto(mockUser));
    }

    @Test
    public void testGetUserFromJwtThrowsErrorWhenNotSuperAdmin() {
        Mockito.doThrow(ForbiddenException.class).when(roleService).isSuperAdmin(any(HttpServletRequest.class));
        assertThrows(ForbiddenException.class, () -> controller.getUserFromJwt(mock(HttpServletRequest.class)));
    }

    @Test
    public void testGetUserFromJwtThrowsInvalidRequestWhenUserIsEmpty()  {
        when(roleService.isSuperAdmin(any(HttpServletRequest.class)))
                .thenReturn(true);
        when(customJwtService.getUserFromJwt(any(HttpServletRequest.class)))
                .thenReturn(Optional.empty());
        assertThrows(InvalidRequestException.class, () -> controller.getUserFromJwt(mock(HttpServletRequest.class)));
    }

    @Test
    void updateDepartmentCantBeCalledOnApplicantAndFindUser() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
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

        assertThrows(InvalidRequestException.class, () -> controller.updateDepartment(httpRequest, 1, 1));

    }

}