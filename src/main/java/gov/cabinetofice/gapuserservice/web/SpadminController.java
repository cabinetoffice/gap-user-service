package gov.cabinetofice.gapuserservice.web;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.mappers.UserMapper;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.SpadminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
@RequestMapping("/spadmin")
public class SpadminController {
    private final SpadminService spadminService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;


    @Value("${super-admin-redirect-url}")
    private static String redirectUrl;

    @PostMapping("/edit-role/{users_sub}")
    public RedirectView updateRolesForUserId(@RequestParam MultiValueMap<String, String> newRoles, @PathVariable String users_sub) {
        Collection<List<String>> newROles = newRoles.values();
        User updatedUser = spadminService.updateRolesForUser(users_sub, newROles);
        return new RedirectView(redirectUrl + "/edit-role/" + users_sub + "?success");
    }

    @GetMapping("/get-all-roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(spadminService.getAllRoles());
    }

    @GetMapping("/get-user-data/{users_sub}")
    public ResponseEntity<OneLoginUserInfoDto> getUserData(@PathVariable String users_sub) {
        User user = userRepository.findBySub(users_sub)
                .orElseThrow(()-> new RuntimeException("User not found"));

        OneLoginUserInfoDto response = OneLoginUserInfoDto.builder()
                .emailAddress(user.getEmailAddress())
                .sub(user.getSub())
                .roles(user.getRoles()
                        .stream()
                        .map(roleMapper::roleToRoleDto)
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(response);
    }
}
