package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.dto.UserQueryDto;
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
import gov.cabinetofice.gapuserservice.util.UserQueryCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Service
public class OneLoginUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;


    public Page<User> getPaginatedUsers(Pageable pageable, UserQueryDto userQueryDto) {
        // Create a map with all possible conditions and their corresponding repository calls
        Map<UserQueryCondition, BiFunction<UserQueryDto, Pageable, Page<User>>> conditionMap
                = createUserQueryConditionMap();

        // Evaluate the condition based on the user query dto
        UserQueryCondition condition = getCondition(userQueryDto);

        // Pass current condition into Map to get the corresponding repository call from the BiFunction
        BiFunction<UserQueryDto, Pageable, Page<User>> action = conditionMap.get(condition);

        if (action != null) {
            // Call the repository method
            return action.apply(userQueryDto, pageable);
        }

        // Run a default query or throw an exception
        throw new RuntimeException();
    }

    private Map<UserQueryCondition, BiFunction<UserQueryDto, Pageable, Page<User>>> createUserQueryConditionMap() {
        Map<UserQueryCondition, BiFunction<UserQueryDto, Pageable, Page<User>>> conditionMap = new HashMap<>();
        conditionMap.put(new UserQueryCondition(false, false, false), (dto, pageable) -> userRepository.findByOrderByEmail(pageable));
        conditionMap.put(new UserQueryCondition(true, false, false), (dto, pageable) -> userRepository.findUsersByDepartment(dto.getDepartmentIds(), pageable));
        conditionMap.put(new UserQueryCondition(false, true, false), (dto, pageable) -> userRepository.findUsersByDepartmentAndRoles(dto.getDepartmentIds(), dto.getRoleIds(), pageable));
        conditionMap.put(new UserQueryCondition(false, false, true), (dto, pageable) -> userRepository.findAllUsersByFuzzySearchOnEmailAddress(dto.getEmail(), pageable));
        conditionMap.put(new UserQueryCondition(false, true, true), (dto, pageable) -> userRepository.findUsersByRolesAndFuzzySearchOnEmailAddress(dto.getRoleIds(), dto.getEmail(), pageable));
        conditionMap.put(new UserQueryCondition(true, true, true), (dto, pageable) -> userRepository.findUsersByDepartmentAndRolesAndFuzzySearchOnEmailAddress
                (dto.getRoleIds(), dto.getDepartmentIds(), dto.getEmail(), pageable));
        return conditionMap;
    }

    private UserQueryCondition getCondition(UserQueryDto dto) {
        boolean hasDepartment = !dto.getDepartmentIds().isEmpty();
        boolean hasRole = !dto.getRoleIds().isEmpty();
        boolean hasEmail = dto.getEmail() != null && !dto.getEmail().isBlank();
        return new UserQueryCondition(hasDepartment, hasRole, hasEmail);
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