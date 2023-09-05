package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.ChangeDepartmentPageDto;
import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void updateRolesForUserId() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        final ResponseEntity<String> methodResponse = controller.updateRoles(httpRequest, List.of(1,2), 1);

        assertThat(methodResponse).isEqualTo(ResponseEntity.ok("success"));
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
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(customJwtService.getUserFromJwt(httpRequest)).thenReturn(User.builder().build());
        final ResponseEntity<String> methodResponse = controller.deleteUser(httpRequest, 1);

        assertThat(methodResponse).isEqualTo(ResponseEntity.ok("success"));
    }
}