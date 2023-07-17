package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OneLoginUserServiceTest {

    @InjectMocks
    private OneLoginUserService oneLoginUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Test
    void testUpdateRolesForUser() {
        Integer userId = 1;
        List<Integer> newRoles = List.of(1, 2);
        List<Role> currentUserRoles = spy(List.of(Role.builder().name(RoleEnum.FIND).id(1).build()));
        User user = spy(User.builder().gapUserId(1).sub("sub").roles(currentUserRoles).build());
        Role role1 = Role.builder().id(1).name(RoleEnum.FIND).description("a desc").build();
        Role role2 = Role.builder().id(2).name(RoleEnum.APPLICANT).description("a desc 2").build();

        doReturn(user).when(user).removeAllRoles();
        doNothing().when(user).addRole(role2);
        doNothing().when(user).addRole(role1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1)).thenReturn(Optional.of(role1));
        when(roleRepository.findById(2)).thenReturn(Optional.of(role2));

        User updatedUser = oneLoginUserService.updateRoles(1, newRoles);

        Mockito.verify(roleRepository, times(2)).findById(anyInt());
        Mockito.verify(userRepository, times(1)).save(user);
        assertThat(user).isEqualTo(updatedUser);
    }
}
