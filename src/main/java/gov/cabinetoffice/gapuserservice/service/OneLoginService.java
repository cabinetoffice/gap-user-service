package gov.cabinetoffice.gapuserservice.service;

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
import gov.cabinetoffice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetoffice.gapuserservice.dto.IdTokenDto;
import gov.cabinetoffice.gapuserservice.dto.JwtPayload;
import gov.cabinetoffice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetoffice.gapuserservice.dto.StateCookieDto;
import gov.cabinetoffice.gapuserservice.exceptions.*;
import gov.cabinetoffice.gapuserservice.model.Nonce;
import gov.cabinetoffice.gapuserservice.model.Role;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.repository.NonceRepository;
import gov.cabinetoffice.gapuserservice.service.encryption.Sha512Service;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetoffice.gapuserservice.util.LoggingUtils;
import gov.cabinetoffice.gapuserservice.util.RestUtils;
import gov.cabinetoffice.gapuserservice.util.WebUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

import static gov.cabinetoffice.gapuserservice.util.HelperUtils.generateSecureRandomString;
import static net.logstash.logback.argument.StructuredArguments.entries;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RequiredArgsConstructor
@Service
@Slf4j
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
    @Value("${onelogin.logout-url}")
    private String oneLoginLogoutEndpoint;
    @Value("${onelogin.post-logout-redirect-uri}")
    private String postLogoutRedirectUri;
    @Value("${onelogin.mfa.enabled}")
    private Boolean mfaEnabled;

    private static final String SCOPE = "openid email";
    private static final String VTR_MFA_ENABLED = "[\"Cl.Cm\"]";
    private static final String VTR_MFA_DISABLED = "[\"Cl\"]";
    private static final String UI = "en";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String ID_TOKEN = "idToken";
    private static final String STATE_COOKIE = "state";

    private final ApplicationConfigProperties configProperties;
    private final NonceRepository nonceRepository;
    private final CustomJwtServiceImpl customJwtService;
    private final OneLoginUserService oneLoginUserService;
    private final LoggingUtils loggingUtils;
    private final Sha512Service encryptionService;

    public PrivateKey parsePrivateKey() {
        try {
            final byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(privateKey);
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new PrivateKeyParsingException("Unable to parse private key");
        }
    }

    public String generateNonce() {
        return Objects.equals(this.configProperties.getProfile(), "LOCAL") ? "aEwkamaos5C" : generateSecureRandomString(64);
    }

    public String generateState() {
        return generateSecureRandomString(64);
    }

    public String buildEncodedStateJson(final String redirectUrl, final String state, final String saltId) {
        JSONObject stateJsonObject = new JSONObject();
        stateJsonObject.put("state", state);
        stateJsonObject.put("redirectUrl", redirectUrl);
        stateJsonObject.put("saltId", saltId);
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
            log.error("An JSON processing error occurred: ", e);
        }
        return stateCookieDto;
    }

    public String generateAndStoreState(final HttpServletResponse response, final String redirectUrl, final String saltId) {
        final String state = this.generateState();
        final String encodedStateJsonString = this.buildEncodedStateJson(redirectUrl, state, saltId);
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

    public String getOneLoginAuthorizeUrl(final String state, final String nonce) {
        return oneLoginBaseUrl +
                "/authorize?response_type=code" +
                "&scope=" + SCOPE +
                "&client_id=" + clientId +
                "&state=" + state +
                "&redirect_uri=" + serviceRedirectUrl +
                "&nonce=" + nonce +
                "&vtr=" + (mfaEnabled.equals(Boolean.TRUE) ? VTR_MFA_ENABLED : VTR_MFA_DISABLED) +
                "&ui_locales=" + UI;
    }

    public Map<String, String> generateCustomJwtClaims(final OneLoginUserInfoDto userInfo, final String idToken) {
        final User user = oneLoginUserService.getUserFromSub(userInfo.getSub())
                .orElseThrow(() -> new UserNotFoundException("User not found when generating custom jwt claims"));
        final Map<String, String> claims = new HashMap<>();
        claims.put(ID_TOKEN, idToken);
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

    public String createOneLoginJwt() {
        final PrivateKey jwtPrivateKey = parsePrivateKey();

        return Jwts.builder()
                .claim("aud", oneLoginBaseUrl + "/token")
                .claim("iss", clientId)
                .claim("sub", clientId)
                .claim("exp", System.currentTimeMillis() + 300000L)
                .claim("jti", UUID.randomUUID().toString())
                .claim("iat", System.currentTimeMillis())
                .signWith(jwtPrivateKey, SignatureAlgorithm.RS256)
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
                    keyValue(ID_TOKEN, decodedIdToken)
            );
            throw new UnauthorizedClientException(message);
        }
        if (!decodedIdToken.getAud().equals(clientId)) {
            String message = "invalid iss property in One Login ID token";
            log.error(
                    loggingUtils.getLogMessage(message + ": ", 3),
                    keyValue("audFromToken", decodedIdToken.getAud()),
                    keyValue("expectedAud", clientId),
                    keyValue(ID_TOKEN, decodedIdToken)
            );
            throw new UnauthorizedClientException(message);
        }
        if (currentEpochSeconds > decodedIdToken.getExp()) {
            String message = "One Login ID token expired";
            log.error(
                    loggingUtils.getLogMessage(message + ": ", 3),
                    keyValue("currentTime", currentEpochSeconds),
                    keyValue("tokenExpiry", decodedIdToken.getExp()),
                    keyValue(ID_TOKEN, decodedIdToken)
            );
            throw new UnauthorizedClientException(message);
        }
        if (currentEpochSeconds < decodedIdToken.getIat()) {
            String message = "One Login ID token issue date in future";
            log.error(
                    loggingUtils.getLogMessage(message + ": ", 3),
                    keyValue("currentTime", currentEpochSeconds),
                    keyValue("tokenIat", decodedIdToken.getIat()),
                    keyValue(ID_TOKEN, decodedIdToken)
            );
            throw new UnauthorizedClientException(message);
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
            throw new UnauthorizedClientException(message);
        }
    }

    public void validateAuthTokenSignatureAndAlgorithm(String authToken) {
        try {
            SignedJWT signedAuthToken = SignedJWT.parse(authToken);
            JWSAlgorithm jwtAlgorithm = JWSAlgorithm.parse(signedAuthToken.getHeader().getAlgorithm().getName());
            String keyId = signedAuthToken.getHeader().getKeyID();

            JWKSet jwkSet = JWKSet.load(new URL(oneLoginBaseUrl.concat("/.well-known/jwks.json")));
            Optional<JWK> optionalMatchingJwk = Optional.ofNullable(jwkSet.getKeyByKeyId(keyId));
            JWK matchingJwk = optionalMatchingJwk.orElseThrow(() -> new UnauthorizedClientException
                    ("Matching JWK not found for key ID: " + keyId));

            ECDSAVerifier verifier = new ECDSAVerifier((ECKey) matchingJwk);

            if (!matchingJwk.getAlgorithm().equals(jwtAlgorithm)) {
                log.error("Invalid alg property in ID token header: {}", jwtAlgorithm);
                throw new UnauthorizedClientException("Invalid alg property in ID token header");
            }

            if (!signedAuthToken.verify(verifier)) {
                log.error("Invalid signature in ID token: {}", signedAuthToken);
                throw new UnauthorizedClientException("Invalid signature in ID token");
            }

        } catch (IOException | ParseException | JOSEException e) {
            log.error("Unable to validate access token {}", authToken, e);
            throw new UnauthorizedClientException("Unable to validate access token");
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
            log.error("A JSON processing error occurred: ", e);
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

    public void verifyStateAndNonce(final String nonce, final StateCookieDto stateCookieDto, final String state) {
        // Validate that state returned is the same as the one stored in the cookie
        final String saltId = stateCookieDto.getSaltId();
        final String encodedStateJson = buildEncodedStateJson(
                stateCookieDto.getRedirectUrl(),
                stateCookieDto.getState(),
                saltId
        );
        final String hashedStateCookie = encryptionService.getSHA512SecurePassword(encodedStateJson, saltId);
        final boolean isStateVerified = state.equals(hashedStateCookie);
        // by only deleting the salt if the state matches we can ensure that an attacker can't arbitrarily delete salts
        // as we know they haven't changed the saltId
        if (isStateVerified) encryptionService.deleteSalt(saltId);

        // Validate that nonce is stored in the DB
        final Nonce storedNonce = readAndDeleteNonce(nonce);
        final boolean isNonceVerified = nonce.equals(storedNonce.getNonceString());

        // Validate that nonce is less than 10 mins old
        final boolean isNonceExpired = isNonceExpired(storedNonce);

        if (!isStateVerified || !isNonceVerified) {
            log.error(
                    loggingUtils.getLogMessage("/redirect-after-login encountered unauthorised user", 7),
                    keyValue("nonceVerified", isNonceVerified),
                    keyValue("stateVerified", isStateVerified),
                    keyValue("nonceFromToken", nonce),
                    keyValue("nonceFromDB", storedNonce.getNonceString()),
                    keyValue("stateFromResponse", state),
                    keyValue("hashedStateFromCookie", hashedStateCookie),
                    keyValue("stateFromCookie", encodedStateJson)
            );
            throw new UnauthorizedClientException("User authorization failed");
        } else if (isNonceExpired) {
            log.error(
                    loggingUtils.getLogMessage("/redirect-after-login encountered unauthorized user - nonce expired", 7),
                    keyValue("nonceFromToken", nonce),
                    keyValue("nonceFromDB", storedNonce.getNonceString()),
                    keyValue("nonceCreatedAt", storedNonce.getCreatedAt()),
                    keyValue("now", new Date()),
                    keyValue("stateFromResponse", state),
                    keyValue("hashedStateFromCookie", hashedStateCookie),
                    keyValue("stateFromCookie", encodedStateJson)
            );
            throw new NonceExpiredException("User authorization failed, please try again");
        }
    }
}

