package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.util.RestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpadminServiceTest {

    @InjectMocks
    private SpadminService spadminService;

    private static MockedStatic<RestUtils> mockedStatic;
    private Map<String, String> testKeyPair;
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;


    @Mock
    RoleMapper roleMapper;

    @Test
    public void testUpdateRolesForUser() {
        String userSub = "user123";
        Collection<List<String>> newRoles = new ArrayList<>();
        newRoles.add(List.of("2", "1"));

        List<Role> currentUserRoles = spy(List.of(Role.builder().name(RoleEnum.FIND).id(1).build()));
        User user = spy(User.builder().id(1).sub(userSub).roles(currentUserRoles).build());
        Role role1 = Role.builder().id(1).name(RoleEnum.FIND).description("a desc").build();
        Role role2 = Role.builder().id(2).name(RoleEnum.APPLICANT).description("a desc 2").build();

        doReturn(user).when(user).removeAllRoles();
        doNothing().when(user).addRole(role2);
        doNothing().when(user).addRole(role1);

        when(userRepository.findBySub(userSub)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1)).thenReturn(Optional.of(role1));
        when(roleRepository.findById(2)).thenReturn(Optional.of(role2));

        User updatedUser = spadminService.updateRolesForUser(userSub, newRoles);

        Mockito.verify(userRepository, times(1)).findBySub(userSub);
        Mockito.verify(roleRepository, times(2)).findById(anyInt());
        Mockito.verify(userRepository, times(1)).save(user);

        assertThat(user).isEqualTo(updatedUser);
    }


    @Test
    void getAllRoles() {
        Role role1 = Role.builder().id(1).name(RoleEnum.FIND).description("a desc").build();
        Role role2 = Role.builder().id(2).name(RoleEnum.APPLICANT).description("a desc2").build();
        List<Role> mockRoles = List.of(role1, role2);

        RoleDto roleDto1 =  RoleDto.builder().id("1").name("FIND").description("a desc").build();
        RoleDto roleDto2 =  RoleDto.builder().id("2").name("APPLICANT").description("a desc2").build();
        List<RoleDto> expectedRoleDtos = List.of(roleDto1, roleDto2);


        when(roleRepository.findAll()).thenReturn(mockRoles);
        when(roleMapper.roleToRoleDto(role1)).thenReturn(roleDto1);
        when(roleMapper.roleToRoleDto(role2)).thenReturn(roleDto2);

        List<RoleDto> actualRoleDtos = spadminService.getAllRoles();

        assertThat(expectedRoleDtos).isEqualTo(actualRoleDtos);;
    }

}
