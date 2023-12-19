package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
    @InjectMocks
    private RoleService roleService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    RoleMapper roleMapper;
    @Test
    void getAllRoles() {
        Role role1 = Role.builder().id(1).name(RoleEnum.FIND).description("a desc").build();
        Role role2 = Role.builder().id(2).name(RoleEnum.APPLICANT).description("a desc2").build();
        List<Role> mockRoles = List.of(role1, role2);
        RoleDto roleDto1 =  RoleDto.builder().id(1).name("FIND").description("a desc").build();
        RoleDto roleDto2 =  RoleDto.builder().id(2).name("APPLICANT").description("a desc2").build();
        List<RoleDto> expectedRoleDtos = List.of(roleDto1, roleDto2);

        when(roleRepository.findAll()).thenReturn(mockRoles);
        when(roleMapper.roleToRoleDto(role1)).thenReturn(roleDto1);
        when(roleMapper.roleToRoleDto(role2)).thenReturn(roleDto2);

        List<RoleDto> actualRoleDtos = roleService.getAllRoles();

        assertThat(expectedRoleDtos).isEqualTo(actualRoleDtos);
    }

}
