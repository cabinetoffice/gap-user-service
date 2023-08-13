package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetofice.gapuserservice.exceptions.RoleNotFoundException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.DepartmentRepository;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OneLoginUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    public Page<User> getPaginatedUsers(Pageable pageable, String emailAddress, List<Integer> departmentIds, List<Integer> roleIds) {
        final boolean hasEmail = !emailAddress.isBlank();
        final boolean hasDepartment = !departmentIds.isEmpty();
        final boolean hasRole = !roleIds.isEmpty();
        List<Integer> roleIdsNotInQuery = Collections.emptyList();
        if (hasRole) {
            roleIdsNotInQuery = roleRepository.findRoleIdsNotIn(roleIds);
        }

        if (!hasEmail && !hasDepartment && !hasRole)
            return userRepository.findByOrderByEmail(pageable);

        if (hasEmail && !hasDepartment && !hasRole)
            return userRepository.findAllUsersByFuzzySearchOnEmailAddress(emailAddress, pageable);

        if (!hasEmail && !hasDepartment)
            return userRepository.findUsersByRoles(roleIds, roleIdsNotInQuery, pageable);

        if (!hasEmail && !hasRole)
            return userRepository.findUsersByDepartment(departmentIds, pageable);

        if (!hasEmail)
            return userRepository.findUsersByDepartmentAndRoles(roleIds,roleIdsNotInQuery, departmentIds, pageable);

        if (!hasDepartment)
            return userRepository.findUsersByRolesAndFuzzySearchOnEmailAddress(roleIds, roleIdsNotInQuery,emailAddress, pageable);

        if (!hasRole)
            return userRepository.findUsersByDepartmentAndFuzzySearchOnEmailAddress(departmentIds, emailAddress, pageable);

        return userRepository.findUsersByDepartmentAndRolesAndFuzzySearchOnEmailAddress(roleIds,roleIdsNotInQuery, departmentIds, emailAddress, pageable);
    }

    public User getUserById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("user with id: " + id + "not found"));
    }

    public User updateDepartment(Integer id, Integer departmentId) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

        Optional<Department> optionalDepartment = departmentRepository.findById(departmentId);
        if (optionalDepartment.isEmpty()) {
            throw new DepartmentNotFoundException("Department not found");
        }

        User user = optionalUser.get();
        Department department = optionalDepartment.get();

        user.setDepartment(department);
        return userRepository.save(user);
    }

    public User updateRoles(Integer id, List<Integer> newRoles) {
        User user = userRepository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        user.removeAllRoles();
        for (Integer roleId : newRoles) {
            Role role = roleRepository.findById(roleId).orElseThrow();
            user.addRole(role);
        }
        if (user.getRoles().stream().noneMatch(role -> role.getName().equals(RoleEnum.APPLICANT))) {
            Role role = roleRepository.findByName(RoleEnum.APPLICANT).orElseThrow(() -> new RoleNotFoundException("Update Roles failed: Applicant role not found"));
            user.addRole(role);
        }
        if (user.getRoles().stream().noneMatch(role -> role.getName().equals(RoleEnum.FIND))) {
            Role role = roleRepository.findByName(RoleEnum.FIND).orElseThrow(() -> new RoleNotFoundException("Update Roles failed: Find role not found"));
            user.addRole(role);
        }

        userRepository.save(user);
        return user;
    }
}