package gov.cabinetofice.gapuserservice.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.exceptions.*;
import gov.cabinetofice.gapuserservice.model.Nonce;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.NonceRepository;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetofice.gapuserservice.util.LoggingUtils;
import gov.cabinetofice.gapuserservice.util.RestUtils;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import static net.logstash.logback.argument.StructuredArguments.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.view.RedirectView;


import java.io.IOException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

import static gov.cabinetofice.gapuserservice.util.HelperUtils.generateSecureRandomString;

@RequiredArgsConstructor
@Service
@Log4j2
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

    @Value("${admin-backend}")
    private String adminBackend;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${onelogin.logout-url}")
    public String oneLoginLogoutEndpoint;

    @Value("${onelogin.post-logout-redirect-uri}")
    public String postLogoutRedirectUri;


    private static final String SCOPE = "openid email";
    private static final String VTR = "[\"Cl.Cm\"]";
    private static final String UI = "en";
    private static final String GRANT_TYPE = "authorization_code";

    private final ApplicationConfigProperties configProperties;
    private final NonceRepository nonceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WebClient.Builder webClientBuilder;
    private final JwtBlacklistService jwtBlacklistService;
    private final CustomJwtServiceImpl customJwtService;
    private final OneLoginUserService oneLoginUserService;
    private final LoggingUtils loggingUtils;

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
                .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                .build();
        final List<RoleEnum> newUserRoles = getNewUserRoles();
        for (RoleEnum roleEnum : newUserRoles) {
            final Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new RoleNotFoundException("Could not create user: '" + roleEnum + "' role not found"));
            user.addRole(role);
        }
        return userRepository.save(user);
    }

    public String generateNonce() {
        return Objects.equals(this.configProperties.getProfile(), "LOCAL") ? "aEwkamaos5C" : generateSecureRandomString(64);
    }

    public String generateState() {
        return generateSecureRandomString(64);
    }

    public String buildEncodedStateJson(final String redirectUrl, final String state) {
        JSONObject stateJsonObject = new JSONObject();
        stateJsonObject.put("state", state);
        stateJsonObject.put("redirectUrl", redirectUrl);
        final String stateJsonString = stateJsonObject.toString();
        return Base64.getEncoder().encodeToString(stateJsonString.getBytes());
    }

    public StateCookieDto decodeStateCookie(final String stateCookie) {
        final byte[] decodedStateBytes = Base64.getDecoder().decode(stateCookie);
        final String decodedStateString = new String(decodedStateBytes);
        final ObjectMapper mapper = new ObjectMapper();
        StateCookieDto stateCookieDto = new StateCookieDto();
        try {
            stateCookieDto = mapper.readValue(decodedStateString, StateCookieDto.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return stateCookieDto;
    }

    public String generateAndStoreState(final HttpServletResponse response, final String redirectUrl) {
        final String state = this.generateState();
        final String encodedStateJsonString = this.buildEncodedStateJson(redirectUrl, state);
        String STATE_COOKIE = "state";
        final Cookie stateCookie = WebUtil.buildSecureCookie(STATE_COOKIE, encodedStateJsonString, 3600);
        response.addCookie(stateCookie);
        return encodedStateJsonString;
    }

    public String generateAndStoreNonce() {
        final String nonce = this.generateNonce();
        final Nonce nonceModel = Nonce.builder().nonceString(nonce).build();
        this.nonceRepository.save(nonceModel);
        return nonce;
    }

    public Nonce readAndDeleteNonce(final String nonce) {
        final Optional<Nonce> nonceModel = this.nonceRepository.findFirstByNonceStringOrderByNonceStringAsc(nonce);
        nonceModel.ifPresent(this.nonceRepository::delete);
        return nonceModel.orElse(new Nonce());
    }

    public Boolean isNonceExpired(Nonce nonce) {
        final Date nonceCreatedAt = nonce.getCreatedAt();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MINUTE, -10);
        final Date expiryTime = c.getTime();
        return nonceCreatedAt == null || nonceCreatedAt.before(expiryTime) || nonceCreatedAt.after(new Date());
    }

    public void setUsersLoginJourneyState(final User user, final LoginJourneyState newState) {
        user.setLoginJourneyState(newState);
        userRepository.save(user);
    }

    public String getOneLoginAuthorizeUrl(final String state, final String nonce) {
        return oneLoginBaseUrl +
                        "/authorize?response_type=code" +
                        "&scope=" + SCOPE +
                        "&client_id=" + clientId +
                        "&state=" + state +
                        "&redirect_uri=" + serviceRedirectUrl +
                        "&nonce=" + nonce +
                        "&vtr=" + VTR +
                        "&ui_locales=" + UI;
    }

    public Optional<User> getUserFromSub(final String sub) {
        return userRepository.findBySub(sub);
    }

    public Optional<User> getUserFromSubOrEmail(final String sub, final String email) {
        final Optional<User> userOptional = userRepository.findBySub(sub);
        if (userOptional.isPresent()) return userOptional;
        return userRepository.findByEmailAddress(email);
    }

    public Map<String, String> generateCustomJwtClaims(final OneLoginUserInfoDto userInfo, final String idToken) {
        final User user = getUserFromSub(userInfo.getSub())
                .orElseThrow(() -> new UserNotFoundException("User not found when generating custom jwt claims"));
        final Map<String, String> claims = new HashMap<>();
        claims.put("idToken", idToken);
        claims.put("email", userInfo.getEmailAddress());
        claims.put("sub", userInfo.getSub());
        claims.put("roles", user.getRoles().stream().map(Role::getName).toList().toString());
        if (user.hasDepartment()) claims.put("department", user.getDepartment().getName());
        return claims;
    }

    public JSONObject getOneLoginUserTokenResponse(final String code) {
        final String oneLoginJwt = createOneLoginJwt();
        return getTokenResponse(oneLoginJwt, code);
    }

    public OneLoginUserInfoDto getOneLoginUserInfoDto(final String accessToken) {
        return getUserInfo(accessToken);
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
            log.info(
                    loggingUtils.getLogMessage("one login userInfo response: ", 1),
                    entries(userInfo.toMap())
            );
            return OneLoginUserInfoDto.builder()
                    .emailAddress(userInfo.getString("email"))
                    .sub(userInfo.getString("sub"))
                    .build();
        } catch (IOException e) {
            throw new AuthenticationException("unable to retrieve user info");
        }
    }

    public JSONObject getTokenResponse(final String jwt, final String code) {
        final String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + sanitizeCode(code) +
                "&redirect_uri=" + serviceRedirectUrl +
                "&client_assertion_type=" + clientAssertionType +
                "&client_assertion=" + jwt;

        try {
            return RestUtils.postRequestWithBody(oneLoginBaseUrl + "/token", requestBody,
                    "application/x-www-form-urlencoded");
        } catch (IOException e) {
            throw new InvalidRequestException("invalid request");
        }
    }

    public void migrateUser(final User user, final String jwt) {
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
    }

    public RedirectView logoutUser(final Cookie customJWTCookie, final HttpServletResponse response) {
        final DecodedJWT decodedJwt = customJwtService.decodedJwt(customJWTCookie.getValue());
        final JwtPayload payload = customJwtService.decodeTheTokenPayloadInAReadableFormat(decodedJwt);
        oneLoginUserService.invalidateUserJwt(customJWTCookie, response);
        return new RedirectView(oneLoginLogoutEndpoint + "?id_token_hint=" + sanitizeCode(payload.getIdToken()) +
                "&post_logout_redirect_uri=" + postLogoutRedirectUri);
    }


    public void validateIdToken(IdTokenDto decodedIdToken) {
        long currentEpochSeconds = Instant.now().getEpochSecond();

        if (!decodedIdToken.getIss().equals(oneLoginBaseUrl.concat("/"))) {
            String message = "invalid iss property in One Login ID token";
            log.error(
                    loggingUtils.getLogMessage(message + ": ", 3),
                    keyValue("issFromToken", decodedIdToken.getIss()),
                    keyValue("expectedIss", oneLoginBaseUrl.concat("/")),
                    keyValue("idToken", decodedIdToken)
            );
            throw new UnauthorizedException(message);
        }
        if (!decodedIdToken.getAud().equals(clientId)) {
            String message = "invalid iss property in One Login ID token";
            log.error(
                    loggingUtils.getLogMessage(message + ": ", 3),
                    keyValue("issFromToken", decodedIdToken.getIss()),
                    keyValue("expectedIss", oneLoginBaseUrl.concat("/")),
                    keyValue("idToken", decodedIdToken)
            );
            throw new UnauthorizedException(message);
        }
        if (currentEpochSeconds > decodedIdToken.getExp())  {
            String message = "One Login ID token expired";
            log.error(
                    loggingUtils.getLogMessage(message +  ": ", 3),
                    keyValue("currentTime", currentEpochSeconds),
                    keyValue("tokenExpiry", decodedIdToken.getExp()),
                    keyValue("idToken", decodedIdToken)
            );
            throw new UnauthorizedException(message);
        }
        if (currentEpochSeconds < decodedIdToken.getIat()) {
            String message = "One Login ID token issue date in future";
            log.error(
                    loggingUtils.getLogMessage(message + ": ", 3),
                    keyValue("currentTime", currentEpochSeconds),
                    keyValue("tokenIat", decodedIdToken.getIat()),
                    keyValue("idToken", decodedIdToken)
            );
            throw new UnauthorizedException(message);
        }
    }

    public void validateUserSub(String idTokenSub, String userInfoSub) {
        if (!idTokenSub.equals(userInfoSub)) {
            String message = "Sub in One Login ID token does not match sub from /userinfo";
            log.error(
                    loggingUtils.getLogMessage(message + ": ", 3),
                    keyValue("subFromIDToken", idTokenSub),
                    keyValue("subFromUserInfo", userInfoSub)
            );
            throw new UnauthorizedException(message);

        }
    }

    public void validateAuthTokenSignatureAndAlgorithm(String authToken) {
        try {
            SignedJWT signedAuthToken = SignedJWT.parse(authToken);
            JWSAlgorithm jwtAlgorithm = JWSAlgorithm.parse(signedAuthToken.getHeader().getAlgorithm().getName());
            String keyId = signedAuthToken.getHeader().getKeyID();

            JWKSet jwkSet = JWKSet.load(new URL(oneLoginBaseUrl.concat("/.well-known/jwks.json")));
            Optional<JWK> optionalMatchingJwk = Optional.ofNullable(jwkSet.getKeyByKeyId(keyId));
            JWK matchingJwk = optionalMatchingJwk.orElseThrow(() -> new UnauthorizedException
                    ("Matching JWK not found for key ID: " + keyId));

            ECDSAVerifier verifier = new ECDSAVerifier((ECKey) matchingJwk);

            if (!matchingJwk.getAlgorithm().equals(jwtAlgorithm)) {
                log.error("Invalid alg property in ID token header: {}", jwtAlgorithm);
                throw new UnauthorizedException("Invalid alg property in ID token header");
            }

            if (!signedAuthToken.verify(verifier)) {
                log.error("Invalid signature in ID token: {}", signedAuthToken);
                throw new UnauthorizedException("Invalid signature in ID token");
            }

        } catch (IOException | ParseException | JOSEException e) {
            // does the error make it into the logged JSON?
            log.error("Unable to validate access token {}", authToken, e);
            throw new UnauthorizedException("Unable to validate access token");
        }
    }

    private String decodeJWT(final String token) {
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(chunks[1]));
    }

    public IdTokenDto decodeTokenId(final String idToken) {
        ObjectMapper mapper = new ObjectMapper();
        IdTokenDto decodedIdToken = new IdTokenDto();
        try {
            decodedIdToken = mapper.readValue(decodeJWT(idToken), IdTokenDto.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return decodedIdToken;
    }

    private String sanitizeCode(String code) {
        return Jsoup.clean(
                StringEscapeUtils.escapeHtml4(
                        StringEscapeUtils.escapeEcmaScript(StringUtils.replace(code, "'", "''"))
                ),
                Safelist.basic()
        );
    }
}

