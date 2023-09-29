package gov.cabinetofice.gapuserservice.service.user;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.dto.MigrateFindUserDto;
import gov.cabinetofice.gapuserservice.dto.MigrateUserDto;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.UserQueryDto;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.enums.MigrationStatus;
import gov.cabinetofice.gapuserservice.exceptions.*;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class OneLoginUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final JwtBlacklistService jwtBlacklistService;
    private final ApplicationConfigProperties configProperties;
    private final ThirdPartyAuthProviderProperties authenticationProvider;
    private final WebClient.Builder webClientBuilder;
    private final RoleMapper roleMapper;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${admin-backend}")
    private String adminBackend;

    @Value("${find-a-grant.url}")
    private String findFrontend;

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

    public Optional<User> getUserFromSub(final String sub) {
        return userRepository.findBySub(sub);
    }

    public Optional<User> getUserFromSubOrEmail(final String sub, final String email) {
        final Optional<User> userOptional = userRepository.findBySub(sub);
        if (userOptional.isPresent()) return userOptional;
        return userRepository.findByEmailAddress(email);
    }

    public List<RoleEnum> getNewUserRoles() {
        return List.of(RoleEnum.APPLICANT, RoleEnum.FIND);
    }

    public User createNewUser(final String sub, final String email) {
        final User user = User.builder()
                .sub(sub)
                .emailAddress(email)
                .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                .applyAccountMigrated(MigrationStatus.NEW_USER)
                .build();
        final List<RoleEnum> newUserRoles = getNewUserRoles();
        for (RoleEnum roleEnum : newUserRoles) {
            final Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RoleNotFoundException("Could not create user: '" + roleEnum + "' role not found"));
            user.addRole(role);
        }
        return userRepository.save(user);
    }

    public User createOrGetUserFromInfo(final OneLoginUserInfoDto userInfo) {
        final Optional<User> userOptional = getUserFromSubOrEmail(userInfo.getSub(), userInfo.getEmailAddress());
        if (userOptional.isPresent()) {
            final User user = userOptional.get();
            if (!user.hasSub()) {
                user.setSub(userInfo.getSub());
                return userRepository.save(user);
            }
            return user;
        }
        return createNewUser(userInfo.getSub(), userInfo.getEmailAddress());
    }


    public User getUserByUserSub(String userSub) {
        // Get user by One Login sub first
        Optional<User> user = userRepository.findBySub(userSub);
        if (user.isEmpty()) {
            // If user is not found by One Login sub, get user by Cola sub
            try {
                return userRepository.findByColaSub(UUID.fromString(userSub))
                        .orElseThrow(() -> new UserNotFoundException("user with sub: " + userSub + "not found"));

            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID: " + userSub);
                throw new UserNotFoundException("Invalid UUID: " + userSub);
            }
        }
        return user.get();
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
        deleteDepartmentIfPresentAndUserIsOnlyApplicantOrFind(user);
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

    private void deleteDepartmentIfPresentAndUserIsOnlyApplicantOrFind(User user) {
           if (isUserApplicantAndFindOnly(user) && doesUserHaveDepartment(user)) {
               user.setDepartment(null);
            }
    }

    public boolean isUserApplicantAndFindOnly(User user) {
        final List<Role> roles = user.getRoles();
        return !roles.isEmpty() && roles.stream().allMatch(
                role -> role.getName().equals(RoleEnum.FIND) ||
                        role.getName().equals(RoleEnum.APPLICANT));
    }

    private boolean doesUserHaveDepartment(User user) {
        return user.getDepartment() != null;
    }

    @Transactional
    public void deleteUser(Integer id, String jwt) {
        final User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("user with id: " + id + "not found"));
        webClientBuilder.build()
                .delete()
                .uri(adminBackend + "/users/delete/" + (user.hasSub() ? user.getSub() : "") + (user.hasColaSub() ? "?colaSub=" + user.getColaSub() : ""))
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        userRepository.deleteById(id);
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

    public void validateRoles(List<Role> userRoles, String payloadRoles) {
        final Set<String> formattedUserRoles = userRoles.stream()
                .map(role -> roleMapper.roleToRoleDto(role).getName())
                .collect(Collectors.toSet());
        boolean userHasBeenUnblocked = payloadRoles.equals("[]") && formattedUserRoles.size() > 0;

        if (formattedUserRoles.isEmpty()) {
            throw new UnauthorizedException("Payload is invalid - User is blocked");
        }
        if (userHasBeenUnblocked) {
            throw new UnauthorizedException("Payload is invalid - User has been unblocked");
        }
    }

    public void validateSessionsRoles(String emailAddress, String roles) {
        List<Role> userRoles = userRepository.findByEmailAddress(emailAddress).orElseThrow(() -> new InvalidRequestException("Could not get user from emailAddress")).getRoles();
        validateRoles(userRoles, roles);
    }

    public void migrateFindUser(final User user, final String jwt) {
        try {
            final MigrateFindUserDto requestBody = new MigrateFindUserDto(user.getEmailAddress(), user.getSub());
            webClientBuilder.build()
                    .patch()
                    .uri(findFrontend + "/api/user/migrate")
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            final boolean isNewUser = false; //TODO: get this from the response
            if (isNewUser) {
                log.info("Successfully created new find user: " + user.getSub());
                setUsersFindMigrationState(user, MigrationStatus.NEW_USER);
            } else {
                log.info("Successfully migrated find user: " + user.getSub());
                setUsersFindMigrationState(user, MigrationStatus.SUCCEEDED);
            }
        } catch (Exception e) {
            log.error("Failed to migrate user: " + user.getSub(), e);
            setUsersFindMigrationState(user, MigrationStatus.FAILED);
        }
    }

    public void migrateApplyUser(final User user, final String jwt) {
        try {
            final MigrateUserDto requestBody = MigrateUserDto.builder()
                    .oneLoginSub(user.getSub())
                    .colaSub(user.getColaSub())
                    .build();
            webClientBuilder.build()
                    .patch()
                    .uri(adminBackend + "/users/migrate")
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Successfully migrated apply user: " + user.getSub());
            setUsersApplyMigrationState(user, MigrationStatus.SUCCEEDED);
        } catch (Exception e) {
            log.error("Failed to migrate user: " + user.getSub(), e);
            setUsersApplyMigrationState(user, MigrationStatus.FAILED);
        }
    }

    public void setUsersApplyMigrationState(final User user, final MigrationStatus migrationStatus) {
        user.setApplyAccountMigrated(migrationStatus);
        userRepository.save(user);
    }

    public void setUsersFindMigrationState(final User user, final MigrationStatus migrationStatus) {
        user.setFindAccountMigrated(migrationStatus);
        userRepository.save(user);
    }

    public void setUsersEmail(final User user, final String newEmail) {
        user.setEmailAddress(newEmail);
        userRepository.save(user);
    }

    public void setUsersLoginJourneyState(final User user, final LoginJourneyState newState) {
        user.setLoginJourneyState(newState);
        userRepository.save(user);
    }

    public boolean hasEmailChanged(final User user, final OneLoginUserInfoDto userInfo) {
        return userInfo != null && !userInfo.getEmailAddress().equals(user.getEmailAddress());
    }
}