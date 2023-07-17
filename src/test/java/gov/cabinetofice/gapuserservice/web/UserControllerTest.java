package gov.cabinetofice.gapuserservice.web;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @InjectMocks
    private UserController controller;
    @Mock
    private OneLoginUserService oneLoginUserService;
    @Test
    void updateRolesForUserId() {
        final ResponseEntity<String> methodResponse = controller.updateRoles(List.of(1,2), 1);

        assertThat(methodResponse).isEqualTo(ResponseEntity.ok("success"));
    }
    @Test
    void getUserData() {
        User mockUser = User.builder().sub("1").gapUserId(1)
                .roles(List.of(Role.builder()
                        .name(RoleEnum.FIND)
                        .description("desc").build()))
                .emailAddress("john").build();
        when(oneLoginUserService.getUserById(1)).thenReturn(mockUser);
        final ResponseEntity<User> methodResponse = controller.getUserById(1);

        assertThat(methodResponse.getBody()).isSameAs(mockUser);
    }
}