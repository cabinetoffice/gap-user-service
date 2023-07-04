package gov.cabinetofice.gapuserservice.service;

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

    public JSONObject getUserInfo(String accessToken) {

        try {

            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            return RestUtils.getRequestWithHeaders(oneLoginBaseUrl + "/userinfo", headers);

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
        return userRepository.existsByHashedEmail(email);
    }

    public boolean doesUserExistBySub(final String sub) {
        return userRepository.existsBySub(sub);
    }

    public List<RoleEnum> getUsersRoles(final String sub) {
        final User user = userRepository.findBySub(sub).orElseThrow(() -> new UserNotFoundException("Could not get users roles: User with sub '" + sub + "' not found"));
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    public void createUser(final String sub, final String email) {
        final User user = User.builder()
                .sub(sub)
                .hashedEmail("hash") // TODO - hash email
                .encryptedEmail("encrypt") // TODO - encrypt email
                .build();
        user.addRole(roleRepository.findByName(RoleEnum.APPLICANT)
                .orElseThrow(() -> new RoleNotFoundException("Could not create user: 'APPLICANT' role not found")));
        user.addRole(roleRepository.findByName(RoleEnum.FIND)
                .orElseThrow(() -> new RoleNotFoundException("Could not create user: 'FIND' role not found")));
        userRepository.save(user);
    }

    public void addSubToUser(final String sub, final String email) {
        final String hashedEmail = email; // TODO - hash email
        final User user = userRepository.findByHashedEmail(hashedEmail).orElseThrow(() -> new UserNotFoundException("Could not add sub to user: User with email '" + email + "' not found"));
        user.setSub(sub);
        userRepository.save(user);
    }

}
