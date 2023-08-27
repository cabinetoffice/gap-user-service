

package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
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
import gov.cabinetofice.gapuserservice.service.JwtBlacklistService;
import gov.cabinetofice.gapuserservice.util.UserQueryCondition;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Service
public class OneLoginUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final JwtBlacklistService jwtBlacklistService;
    private final ApplicationConfigProperties configProperties;
    private final ThirdPartyAuthProviderProperties authenticationProvider;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    public Page<User> getPaginatedUsers(Pageable pageable, UserQueryDto userQueryDto) {
        final Map<UserQueryCondition, BiFunction<UserQueryDto, Pageable, Page<User>>> conditionMap = createUserQueryConditionMap();
        final UserQueryCondition condition = userQueryDto.getCondition();
        final BiFunction<UserQueryDto, Pageable, Page<User>> action = conditionMap.get(condition);
        return action.apply(userQueryDto, pageable);
    }

    private Map<UserQueryCondition, BiFunction<UserQueryDto, Pageable, Page<User>>> createUserQueryConditionMap() {
        return Map.of(
            new UserQueryCondition(false, false, false), (dto, pageable) -> userRepository.findByOrderByEmail(pageable),
            new UserQueryCondition(true,  false, false), (dto, pageable) -> userRepository.findUsersByDepartment(dto.departmentIds(), pageable),
            new UserQueryCondition(false, true,  false), (dto, pageable) -> userRepository.findUsersByRoles(dto.roleIds(), pageable),
            new UserQueryCondition(false, false, true),  (dto, pageable) -> userRepository.findUsersByFuzzyEmail(dto.email(), pageable),
            new UserQueryCondition(true,  true,  false), (dto, pageable) -> userRepository.findUsersByDepartmentAndRoles(dto.roleIds(), dto.departmentIds(), pageable),
            new UserQueryCondition(true,  false, true),  (dto, pageable) -> userRepository.findUsersByDepartmentAndFuzzyEmail(dto.departmentIds(), dto.email(), pageable),
            new UserQueryCondition(false, true,  true),  (dto, pageable) -> userRepository.findUsersByRolesAndFuzzyEmail(dto.roleIds(), dto.email(), pageable),
            new UserQueryCondition(true,  true,  true),  (dto, pageable) -> userRepository.findUsersByDepartmentAndRolesAndFuzzyEmail(dto.roleIds(), dto.departmentIds(), dto.email(), pageable)
        );
    }

    public User getUserById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("user with id: " + id + "not found"));
    }
    public User getUserBySub(String sub) {
        return userRepository.findBySub(sub)
                .orElseThrow(() -> new UserNotFoundException("user with sub: " + sub + "not found"));
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

    public void invalidateUserJwt(final Cookie customJWTCookie, final HttpServletResponse response) {
        if (customJWTCookie.getValue() != null) {
            jwtBlacklistService.addJwtToBlacklist(customJWTCookie.getValue());
        }
        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(userServiceCookieName, null),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );
        userTokenCookie.setMaxAge(0);
        response.addCookie(userTokenCookie);

        final String authenticationCookieDomain = Objects.equals(this.configProperties.getProfile(), "LOCAL") ? "localhost" : "cabinetoffice.gov.uk";

        final Cookie thirdPartyAuthToken = WebUtil.buildCookie(
                new Cookie(authenticationProvider.getTokenCookie(), null),
                Boolean.TRUE,
                Boolean.TRUE,
                authenticationCookieDomain
        );
        thirdPartyAuthToken.setMaxAge(0);
        response.addCookie(thirdPartyAuthToken);
    }
}