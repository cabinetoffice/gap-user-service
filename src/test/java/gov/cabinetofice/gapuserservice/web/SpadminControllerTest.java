package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.SpadminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpadminControllerTest {
    @InjectMocks
    private SpadminController controller;

    @Mock
    private SpadminService spadminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @Test
    void updateRolesForUserId() {
        LinkedHashMap<String, String> roles = new LinkedHashMap<String, String>();
        roles.put("roles", "4");
        final RedirectView methodResponse = controller.updateRolesForUserId(roles, "1");

        assertThat(methodResponse.getUrl()).contains("edit-role/1?success");
    }

    @Test
    void getAllRoles_returnsArrayOfRoles() {
        final List<RoleDto> roles = List.of(RoleDto.builder().name("FIND").id("1").description("a desc").build());
        when(spadminService.getAllRoles())
                .thenReturn(roles);
        final ResponseEntity<List<RoleDto>> methodResponse = controller.getAllRoles();

        assertThat(methodResponse.getBody()).isSameAs(roles);
    }

    @Test
    void getUserData() {
        User mockUser = User.builder().sub("1").id(1)
                .roles(List.of(Role.builder()
                        .name(RoleEnum.FIND)
                        .description("desc").build()))
                .emailAddress("john").build();

        when(userRepository.findBySub("1")).thenReturn(Optional.of(mockUser));
        when(roleMapper.roleToRoleDto(Mockito.any())).thenReturn(RoleDto.builder().name("FIND").id("1").description("desc").build());
        final ResponseEntity<UserDto> methodResponse = controller.getUserData("1");

        assertThat(methodResponse.getBody()).isEqualTo(UserDto.builder().roles(List.of(RoleDto.builder().name("FIND").id("1").description("desc").build())).emailAddress("john").sub("1").build());
    }
}