package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.exceptions.*;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.util.RestUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OneLoginService {

    @Value("${onelogin.client-id}")
    private String clientId;

    @Value("${onelogin.base-url}")
    private String oneLoginBaseUrl;

    @Value("${onelogin.client-assertion-type}")
    private String clientAssertionType;

    @Value("${onelogin.service-redirect-url}")
    private String serviceRedirectUrl;

    @Value("${onelogin.private-key}")
    public String privateKey;

    private static final String GRANT_TYPE = "authorization_code";

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;


    public String createOneLoginJwt() {

        PrivateKey privateKey = parsePrivateKey();

        return Jwts.builder()
                .claim("aud", oneLoginBaseUrl + "/token")
                .claim("iss", clientId)
                .claim("sub", clientId)
                .claim("exp", System.currentTimeMillis() + 300000L)
                .claim("jti", UUID.randomUUID().toString())
                .claim("iat", System.currentTimeMillis())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public OneLoginUserInfoDto getUserInfo(String accessToken) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            final JSONObject userInfo = RestUtils.getRequestWithHeaders(oneLoginBaseUrl + "/userinfo", headers);
            return OneLoginUserInfoDto.builder()
                    .email(userInfo.getString("email"))
                    .sub(userInfo.getString("sub"))
                    .build();
        } catch (IOException e) {
            throw new AuthenticationException("unable to retrieve user info");
        }
    }

    public String getAuthToken(String jwt, String code) {
        String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + code +
                "&redirect_uri=" + serviceRedirectUrl +
                "&client_assertion_type=" + clientAssertionType +
                "&client_assertion=" + jwt;

        try {

            JSONObject response = RestUtils.postRequestWithBody(oneLoginBaseUrl + "/token", requestBody,
                    "application/x-www-form-urlencoded");

            return response.getString("access_token");

        } catch (IOException e) {
            throw new InvalidRequestException("invalid request");
        } catch (JSONException e) {
            throw new AuthenticationException("unable to retrieve access_token");
        }
    }

    public PrivateKey parsePrivateKey() {
        try {
            byte [] pkcs8EncodedBytes = Base64.getDecoder().decode(privateKey);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        } catch (Exception e) {
            throw new PrivateKeyParsingException("Unable to parse private key");
        }
    }

    public boolean doesUserExistByEmail(final String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean doesUserExistBySub(final String sub) {
        return userRepository.existsBySub(sub);
    }

    public List<RoleEnum> getUsersRolesByEmail(final String email) {
        return roleRepository.findByUsers_Email(email)
                .stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    public List<RoleEnum> getUsersRolesBySub(final String sub) {
        return roleRepository.findByUsers_Sub(sub)
                .stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    public List<RoleEnum> getNewUserRoles() {
        return List.of(RoleEnum.APPLICANT, RoleEnum.FIND);
    }

    public boolean isUserAnApplicant(final List<RoleEnum> userRoles) {
        return !isUserAnAdmin(userRoles) && userRoles.stream().anyMatch((role) -> role.equals(RoleEnum.APPLICANT));
    }

    public boolean isUserAnAdmin(final List<RoleEnum> userRoles) {
        return userRoles.stream().anyMatch((role) -> role.equals(RoleEnum.ADMIN) || role.equals(RoleEnum.SUPER_ADMIN));
    }

    public boolean isUserASuperAdmin(final List<RoleEnum> userRoles) {
        return userRoles.stream().anyMatch((role) -> role.equals(RoleEnum.SUPER_ADMIN));
    }

    public List<RoleEnum> createUser(final String sub, final String email) {
        final User user = User.builder()
                .sub(sub)
                .email(email)
                .build();
        final List<RoleEnum> newUserRoles = getNewUserRoles();
        for (RoleEnum roleEnum : newUserRoles) {
            final Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RoleNotFoundException("Could not create user: '" + roleEnum + "' role not found"));
            user.addRole(role);
        }
        userRepository.save(user);
        return newUserRoles;
    }

    public void addSubToUser(final String sub, final String email) {
        final User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("Could not add sub to user: User with email '" + email + "' not found"));
        user.setSub(sub);
        userRepository.save(user);
    }
}
