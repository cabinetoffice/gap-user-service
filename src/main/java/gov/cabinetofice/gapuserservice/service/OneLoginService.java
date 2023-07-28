package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
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

    private static final String SCOPE = "openid email";
    private static final String VTR = "[\"Cl.Cm\"]";
    private static final String UI = "en";
    private static final String GRANT_TYPE = "authorization_code";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public PrivateKey parsePrivateKey() {
        try {
            final byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(privateKey);
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new PrivateKeyParsingException("Unable to parse private key");
        }
    }

    public List<RoleEnum> getNewUserRoles() {
        return List.of(RoleEnum.APPLICANT, RoleEnum.FIND);
    }

    public User createNewUser(final String sub, final String email) {
        final User user = User.builder()
                .sub(sub)
                .emailAddress(email)
                .acceptedPrivacyPolicy(false)
                .loginJourneyState(LoginJourneyState.CREATING_NEW_USER)
                .build();
        final List<RoleEnum> newUserRoles = getNewUserRoles();
        for (RoleEnum roleEnum : newUserRoles) {
            final Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RoleNotFoundException("Could not create user: '" + roleEnum + "' role not found"));
            user.addRole(role);
        }
        return userRepository.save(user);
    }

    public User setPrivacyPolicy(final User user) {
        user.setAcceptedPrivacyPolicy(true);
        return userRepository.save(user);
    }

    public String generateNonce() {
         return UUID.randomUUID().toString();
    }

    public String generateState() {
        return UUID.randomUUID().toString();
    }

    public void setUsersLoginJourneyState(final User user, final LoginJourneyState newState) {
        user.setLoginJourneyState(newState);
        userRepository.save(user);
    }

    public String getOneLoginAuthorizeUrl() {
        return oneLoginBaseUrl +
                        "/authorize?response_type=code" +
                        "&scope=" + SCOPE +
                        "&client_id=" + clientId +
                        "&state=" + generateState() +
                        "&redirect_uri=" + serviceRedirectUrl +
                        "&nonce=" + generateNonce() +
                        "&vtr=" + VTR +
                        "&ui_locales=" + UI;
    }

    public Optional<User> getUserFromSub(final String sub) {
        return userRepository.findBySub(sub);
    }

    public Map<String, String> generateCustomJwtClaims(final OneLoginUserInfoDto userInfo) {
        final User user = getUserFromSub(userInfo.getSub())
                .orElseThrow(() -> new UserNotFoundException("User not found when generating custom jwt claims"));
        final Map<String, String> claims = new HashMap<>();
        claims.put("email", userInfo.getEmailAddress());
        claims.put("sub", userInfo.getSub());
        claims.put("roles", user.getRoles().stream().map(Role::getName).toList().toString());
        if (user.hasDepartment()) claims.put("department", user.getDepartment().getName());
        return claims;
    }

    public OneLoginUserInfoDto getOneLoginUserInfoDto(final String code) {
        final String oneLoginJwt = createOneLoginJwt();
        final String authToken = getAuthToken(oneLoginJwt, code);
        return getUserInfo(authToken);
    }

    public User createOrGetUserFromInfo(final OneLoginUserInfoDto userInfo) {
        final Optional<User> userOptional = getUserFromSub(userInfo.getSub());
        return userOptional.orElseGet(() -> createNewUser(userInfo.getSub(), userInfo.getEmailAddress()));
    }

    public String createOneLoginJwt() {
        final PrivateKey privateKey = parsePrivateKey();

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

    public OneLoginUserInfoDto getUserInfo(final String accessToken) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            final JSONObject userInfo = RestUtils.getRequestWithHeaders(oneLoginBaseUrl + "/userinfo", headers);
            return OneLoginUserInfoDto.builder()
                    .emailAddress(userInfo.getString("email"))
                    .sub(userInfo.getString("sub"))
                    .build();
        } catch (IOException e) {
            throw new AuthenticationException("unable to retrieve user info");
        }
    }

    public String getAuthToken(final String jwt, final String code) {
        final String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + code +
                "&redirect_uri=" + serviceRedirectUrl +
                "&client_assertion_type=" + clientAssertionType +
                "&client_assertion=" + jwt;

        try {
            final JSONObject response = RestUtils.postRequestWithBody(oneLoginBaseUrl + "/token", requestBody,
                    "application/x-www-form-urlencoded");
            return response.getString("access_token");
        } catch (IOException e) {
            throw new InvalidRequestException("invalid request");
        } catch (JSONException e) {
            throw new AuthenticationException("unable to retrieve access_token");
        }
    }
}
