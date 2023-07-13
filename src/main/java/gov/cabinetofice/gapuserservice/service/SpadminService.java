package gov.cabinetofice.gapuserservice.service;

//import gov.cabinetofice.gapuserservice.entities.UserRoles;

import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpadminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public User updateRolesForUser(String userSub, Object roles) {
        User user = userRepository.findBySub(userSub).orElseThrow(()-> new RuntimeException("User not found"));
        user.removeAllRoles();
            for (String id : (ArrayList<String>) roles) {
                Role fullRole = roleRepository.findById(Integer.valueOf(id)).orElseThrow();
                user.addRole(fullRole);
            }
        userRepository.save(user);
        return user;
    }

    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::roleToRoleDto)
                .collect(Collectors.toList());
    }
}
