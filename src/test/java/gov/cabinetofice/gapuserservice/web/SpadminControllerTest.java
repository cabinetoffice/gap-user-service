package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.SpadminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.view.RedirectView;

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

    @Test
    void updateRolesForUserId() {
        MultiValueMap<String, String> roles = new LinkedMultiValueMap<String, String>();
        roles.add("roles", "4");
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
        User mockUser = User.builder().sub("1").build();
        when(userRepository.findBySub("1")).thenReturn(Optional.of(mockUser));
        final ResponseEntity<OneLoginUserInfoDto> methodResponse = controller.getUserData("1");

        assertThat(methodResponse.getBody()).isEqualTo(OneLoginUserInfoDto.builder().sub("1").roles(List.of()).build());
    }
}