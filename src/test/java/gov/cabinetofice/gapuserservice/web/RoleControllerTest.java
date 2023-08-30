package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {
    @InjectMocks
    private RoleController controller;
    @Mock
    private RoleService roleService;

    @Test
    void getAllRoles_returnsArrayOfRoles() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        final List<RoleDto> roles = List.of(RoleDto.builder().name("FIND").id(1).description("a desc").build());
        when(roleService.getAllRoles())
                .thenReturn(roles);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        final ResponseEntity<List<RoleDto>> methodResponse = controller.getAll(httpRequest);

        assertThat(methodResponse.getBody()).isSameAs(roles);
    }
}