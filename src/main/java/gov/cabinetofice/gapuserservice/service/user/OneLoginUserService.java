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

        if (!hasEmail && !hasDepartment && !hasRole)
            return userRepository.findByOrderByEmail(pageable);

        if (hasEmail && !hasDepartment && !hasRole)
            return userRepository.findAllUsersByFuzzySearchOnEmailAddress(emailAddress, pageable);

        if (!hasEmail && !hasDepartment)
            return userRepository.findUsersByRoles(roleIds, pageable);

        if (!hasEmail && !hasRole)
            return userRepository.findUsersByDepartment(departmentIds, pageable);

        if (!hasEmail)
            return userRepository.findUsersByDepartmentAndRoles(roleIds, departmentIds, pageable);

        if (!hasDepartment)
            return userRepository.findUsersByRolesAndFuzzySearchOnEmailAddress(roleIds, emailAddress, pageable);

        if (!hasRole)
            return userRepository.findUsersByDepartmentAndFuzzySearchOnEmailAddress(departmentIds, emailAddress, pageable);

        return userRepository.findUsersByDepartmentAndRolesAndFuzzySearchOnEmailAddress(roleIds, departmentIds, emailAddress, pageable);
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
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.removeAllRoles();

        if (newRoles == null || newRoles.isEmpty()) {
            userRepository.save(user);
            return user;
        }

        for (Integer roleId : newRoles) {
            Role role = roleRepository.findById(roleId).orElseThrow();
            user.addRole(role);
        }

        addRoleIfNotPresent(user, RoleEnum.FIND);
        addRoleIfNotPresent(user, RoleEnum.APPLICANT);
        userRepository.save(user);

        return user;
    }

    private void addRoleIfNotPresent(User user, RoleEnum roleName) {
        if (user.getRoles().stream().noneMatch(role -> role.getName().equals(roleName))) {
            Role role = roleRepository.findByName(roleName).orElseThrow(() -> new RoleNotFoundException(
                    "Update Roles failed: ".concat(roleName.name()).concat(" role not found")));
            user.addRole(role);
        }
    }

    public User deleteUser(Integer id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        userRepository.delete(user);
        return user;
    }
}