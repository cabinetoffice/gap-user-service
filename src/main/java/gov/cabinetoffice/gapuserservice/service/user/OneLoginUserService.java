package gov.cabinetoffice.gapuserservice.service.user;

import gov.cabinetoffice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetoffice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetoffice.gapuserservice.dto.*;
import gov.cabinetoffice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetoffice.gapuserservice.enums.MigrationStatus;
import gov.cabinetoffice.gapuserservice.exceptions.*;
import gov.cabinetoffice.gapuserservice.mappers.RoleMapper;
import gov.cabinetoffice.gapuserservice.model.Department;
import gov.cabinetoffice.gapuserservice.model.Role;
import gov.cabinetoffice.gapuserservice.model.RoleEnum;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.repository.DepartmentRepository;
import gov.cabinetoffice.gapuserservice.repository.RoleRepository;
import gov.cabinetoffice.gapuserservice.repository.UserRepository;
import gov.cabinetoffice.gapuserservice.service.JwtBlacklistService;
import gov.cabinetoffice.gapuserservice.service.encryption.AwsEncryptionServiceImpl;
import gov.cabinetoffice.gapuserservice.util.UserQueryCondition;
import gov.cabinetoffice.gapuserservice.util.WebUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    private static final String NOT_FOUND = "not found";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_HEADER_PREFIX = "Bearer ";

    private final AwsEncryptionServiceImpl awsEncryptionService;

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
                new UserQueryCondition(true, false, false), (dto, pageable) -> userRepository.findUsersByDepartment(dto.departmentIds(), pageable),
                new UserQueryCondition(false, true, false), (dto, pageable) -> userRepository.findUsersByRoles(dto.roleIds(), pageable),
                new UserQueryCondition(false, false, true), (dto, pageable) -> userRepository.findUsersByFuzzyEmail(dto.email(), pageable),
                new UserQueryCondition(true, true, false), (dto, pageable) -> userRepository.findUsersByDepartmentAndRoles(dto.roleIds(), dto.departmentIds(), pageable),
                new UserQueryCondition(true, false, true), (dto, pageable) -> userRepository.findUsersByDepartmentAndFuzzyEmail(dto.departmentIds(), dto.email(), pageable),
                new UserQueryCondition(false, true, true), (dto, pageable) -> userRepository.findUsersByRolesAndFuzzyEmail(dto.roleIds(), dto.email(), pageable),
                new UserQueryCondition(true, true, true), (dto, pageable) -> userRepository.findUsersByDepartmentAndRolesAndFuzzyEmail(dto.roleIds(), dto.departmentIds(), dto.email(), pageable)
        );
    }

    public User getUserById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("user with id: " + id + NOT_FOUND));
    }

    public User getUserBySub(String sub) {
        return userRepository.findBySub(sub)
                .orElseThrow(() -> new UserNotFoundException("user with sub: " + sub + NOT_FOUND));
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
                        .orElseThrow(() -> new UserNotFoundException("user with sub: " + userSub + NOT_FOUND));

            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID: " + userSub);
                throw new UserNotFoundException("Invalid UUID: " + userSub);
            }
        }
        return user.get();
    }

    public User updateDepartment(Integer id, Integer departmentId, String jwt) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

        Optional<Department> optionalDepartment = departmentRepository.findById(departmentId);
        if (optionalDepartment.isEmpty()) {
            throw new DepartmentNotFoundException("Department not found");
        }

        webClientBuilder.build()
                .patch()
                .uri(adminBackend + "/users/funding-organisation")
                .header(AUTHORIZATION_HEADER_NAME, BEARER_HEADER_PREFIX + jwt)
                .bodyValue(UpdateFundingOrgDto.builder()
                        .email(optionalUser.get().getEmailAddress())
                        .sub(optionalUser.get().getSub())
                        .departmentName(optionalDepartment.get().getName())
                        .build())
                .retrieve()
                .onStatus(httpStatus -> httpStatus.equals(HttpStatus.NOT_FOUND), clientResponse -> {
                    log.error("User with sub {} does not exist as an admin in the Apply database",
                            optionalUser.get().getSub());
                    return Mono.empty();
                })
                .bodyToMono(Void.class)
                .block();

        User user = optionalUser.get();
        Department department = optionalDepartment.get();

        user.setDepartment(department);
        return userRepository.save(user);
    }

    public User updateRoles(Integer id, UpdateUserRolesRequestDto updateUserRolesRequestDto, String jwt) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        handleAdminRoleChange(user, updateUserRolesRequestDto, jwt);
        handleTechSupportRoleChange(user, updateUserRolesRequestDto, jwt);

        user.removeAllRoles();

        if (updateUserRolesRequestDto.newUserRoles().isEmpty()) {
            userRepository.save(user);
            return user;
        }

        for (Integer roleId : updateUserRolesRequestDto.newUserRoles()) {
            Role role = roleRepository.findById(roleId).orElseThrow();
            user.addRole(role);
        }

        addRoleIfNotPresent(user, RoleEnum.FIND);
        addRoleIfNotPresent(user, RoleEnum.APPLICANT);
        deleteDepartmentIfPresentAndUserIsOnlyApplicantOrFind(user);
        userRepository.save(user);

        return user;
    }

    private String getSub(User user) {
        return Optional.ofNullable(user.getSub())
                .map(Object::toString)
                .or(() -> Optional.ofNullable(user.getColaSub()).map(UUID::toString))
                .orElseThrow(() -> new UserNotFoundException("Both the user's sub and colaSub are null"));
    }

    private void handleAdminRoleChange(User user, UpdateUserRolesRequestDto updateUserRolesRequestDto, String jwt) {
        if (!updateUserRolesRequestDto.newUserRoles().contains(RoleEnum.ADMIN.getRoleId()) && user.isAdmin()) {
            removeAdminReferenceApply(getSub(user), jwt);
        }
    }

    private void handleTechSupportRoleChange(User user, UpdateUserRolesRequestDto updateUserRolesRequestDto, String jwt) {
        if (updateUserRolesRequestDto.newUserRoles().contains(RoleEnum.TECHNICAL_SUPPORT.getRoleId())
                && !user.isTechnicalSupport()) {

            Integer departmentId = updateUserRolesRequestDto.departmentId() == null
                    ? user.getDepartment().getId() : updateUserRolesRequestDto.departmentId();

            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new DepartmentNotFoundException
                            ("Department not found with id: " + updateUserRolesRequestDto.departmentId()));
            addTechSupportUserToApply(user, department.getName(), jwt);
        } else {
            if (user.isTechnicalSupport() && !updateUserRolesRequestDto.newUserRoles()
                    .contains(RoleEnum.TECHNICAL_SUPPORT.getRoleId())) {
                deleteTechSupportUserFromApply(getSub(user), jwt);
            }
        }
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
        final User user = userRepository.findById(id).orElseThrow(() ->
                new UserNotFoundException("user with id: " + id + NOT_FOUND));
        deleteUserFromFind(jwt, user);
        deleteUserFromApply(jwt, user);
        userRepository.deleteById(id);
    }

    public void addTechSupportUserToApply(User user, String departmentName, String jwt) {
        webClientBuilder.build()
                .post()
                .uri(adminBackend.concat("/users/tech-support-user"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(CreateTechSupportUserDto.builder()
                        .userSub(getSub(user)).departmentName(departmentName).build()))
                .header(AUTHORIZATION_HEADER_NAME, BEARER_HEADER_PREFIX + jwt)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void removeAdminReferenceApply(String sub, String jwt) {
        webClientBuilder.build()
                .delete()
                .uri(adminBackend.concat("/users/admin-user/".concat(sub)))
                .header(AUTHORIZATION_HEADER_NAME, BEARER_HEADER_PREFIX + jwt)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.equals(HttpStatus.OK), clientResponse -> {
                    log.error("Unable to delete admin user with sub {}, HTTP status code {}", sub, clientResponse.statusCode());
                    return Mono.empty();
                })
                .bodyToMono(Void.class)
                .block();
    }

    public void deleteTechSupportUserFromApply(String sub, String jwt) {
        webClientBuilder.build()
                .delete()
                .uri(adminBackend.concat("/users/tech-support-user/".concat(sub)))
                .header(AUTHORIZATION_HEADER_NAME, BEARER_HEADER_PREFIX + jwt)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.equals(HttpStatus.OK), clientResponse -> {
                    log.error("Unable to delete tech support user with sub {}, HTTP status code {}", sub, clientResponse.statusCode());
                    return Mono.empty();
                })
                .bodyToMono(Void.class)
                .block();
    }

    private void deleteUserFromApply(String jwt, User user) {
        String sub = getSub(user);
        String query = (user.hasSub() ? "?oneLoginSub=" : "?colaSub=") + sub;
        String uri = adminBackend + "/users/delete" + query;

        webClientBuilder.build()
                .delete()
                .uri(uri)
                .header(AUTHORIZATION_HEADER_NAME, BEARER_HEADER_PREFIX + jwt)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.equals(HttpStatus.NOT_FOUND), clientResponse -> {
                    log.error("User with sub {} does not exist the Apply database", sub);
                    return Mono.empty();
                })
                .bodyToMono(Void.class)
                .block();
    }

    private void deleteUserFromFind(String jwt, User user) {
        String query = !StringUtils.isEmpty(user.getSub())
                ? "?sub=".concat(user.getSub()) : "?email=".concat(user.getEmailAddress());

        webClientBuilder.build()
                .delete()
                .uri(findFrontend.concat("/api/user/delete").concat(query))
                .header(AUTHORIZATION_HEADER_NAME, BEARER_HEADER_PREFIX + jwt)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.equals(HttpStatus.NOT_FOUND), clientResponse -> {
                    log.error("User with ID".concat(user.getGapUserId().toString())
                            .concat("cannot be found in the Find database"));
                    return Mono.empty();
                })
                .bodyToMono(Void.class)
                .block();
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
        boolean userHasBeenUnblocked = payloadRoles.equals("[]") && !formattedUserRoles.isEmpty();

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
        byte[] encryptedEmail = awsEncryptionService.encryptField(user.getEmailAddress());
        try {
            final MigrateFindUserDto requestBody = new MigrateFindUserDto(encryptedEmail, user.getSub());
            MigrateFindResponseDto response = webClientBuilder.build()
                    .patch()
                    .uri(findFrontend + "/api/user/migrate")
                    .cookie("user-service-token", jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(MigrateFindResponseDto.class)
                    .block();
            if (Objects.requireNonNull(response).isNewUser()) {
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
                    .header(AUTHORIZATION_HEADER_NAME, BEARER_HEADER_PREFIX + jwt)
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

    public List<UserEmailDto> getUserEmailsBySubs(List<String> subs) {
        List<User> users = userRepository.findBySubIn(subs);
        return users.stream().map(user -> UserEmailDto.builder()
                .emailAddress(awsEncryptionService.encryptField(user.getEmailAddress()))
                .sub(user.getSub())
                .build())
                .toList();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailAddress(email).orElseThrow(() -> new UserNotFoundException("user with email: " + email + NOT_FOUND));
    }

    public User getUserByEmailAndRole(String email, String roleName) {
        final RoleEnum roleEnum = RoleEnum.valueOf(roleName);
        final Role role = roleRepository.findByName(roleEnum).orElseThrow(() -> new RoleNotFoundException("Could not find user: '" + roleEnum + "' role not found"));
        return userRepository.findByEmailAddressAndRole(email, role.getId()).orElseThrow(() -> new UserNotFoundException("user with email: " + email + " and role: " + roleName + " " + NOT_FOUND));
    }
}
