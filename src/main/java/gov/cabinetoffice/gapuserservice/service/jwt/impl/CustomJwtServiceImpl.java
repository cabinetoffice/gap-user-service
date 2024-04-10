package gov.cabinetoffice.gapuserservice.service.jwt.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import gov.cabinetoffice.gapuserservice.config.JwtProperties;
import gov.cabinetoffice.gapuserservice.dto.JwtHeader;
import gov.cabinetoffice.gapuserservice.dto.JwtPayload;
import gov.cabinetoffice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetoffice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetoffice.gapuserservice.model.Role;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetoffice.gapuserservice.repository.UserRepository;
import gov.cabinetoffice.gapuserservice.service.jwt.JwtService;
import gov.cabinetoffice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetoffice.gapuserservice.util.HelperUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CustomJwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;
    private final JwtBlacklistRepository jwtBlacklistRepository;

    private final UserRepository userRepository;
    private final OneLoginUserService oneLoginUserService;

    private final KmsClient kmsClient;

    private final Clock clock;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${feature.onelogin.enabled}")
    public boolean oneLoginEnabled;

    @Value("${feature.validate-user-roles-in-middleware}")
    public boolean validateUserRolesInMiddleware;

    @Value("${aws.kms.signing-key.arn}")
    public String signingKeyArn;

    @Value("${jwt.memoization-cache-expiry}")
    long memoizationCacheExpiry;
    public CustomJwtServiceImpl(OneLoginUserService oneLoginUserService,
                                JwtProperties jwtProperties, JwtBlacklistRepository jwtBlacklistRepository,
                                UserRepository userRepository, Clock clock, KmsClient kmsClient) {
        this.oneLoginUserService = oneLoginUserService;
        this.jwtProperties = jwtProperties;
        this.jwtBlacklistRepository = jwtBlacklistRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.kmsClient = kmsClient;
    }

    private final Cache<String, Boolean> memoizationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(memoizationCacheExpiry, TimeUnit.SECONDS)
            .build();

    public boolean handleTokenVerification(String token) {
        try {
            return memoizationCache.get(token, () -> verifyToken(token));
        } catch (Exception e) {
            throw new JWTVerificationException("Unable to determine token validity: ".concat(e.getMessage()));
        }
    }

    private boolean verifyToken(String customJwt) {
        String[] tokenParts = customJwt.split("\\.");

        SdkBytes signature = SdkBytes.fromByteArray(Base64.decodeBase64URLSafe(tokenParts[2]));
        SdkBytes message = SdkBytes.fromByteArray(Base64.decodeBase64URLSafe(tokenParts[1]));

        VerifyResponse verifyResponse = kmsClient.verify(VerifyRequest.builder()
                .signature(signature)
                .keyId(signingKeyArn)
                .message(message)
                .signingAlgorithm(SigningAlgorithmSpec.RSASSA_PSS_SHA_256).build());

        return verifyResponse.signatureValid();
    }
    @Override
    public boolean isTokenValid(final String customJwt) {
        try {
            boolean verifyResponse = handleTokenVerification(customJwt);

            if(Boolean.FALSE.equals(verifyResponse)) {
                throw new JWTVerificationException("Token could not be verified by KMS: ".concat(customJwt));
            }

            if (oneLoginEnabled) {
                final DecodedJWT decodedToken = decodedJwt(customJwt);
                final JwtPayload jwtPayload = decodeTheTokenPayloadInAReadableFormat(decodedToken);
                Optional<User> user = userRepository.findBySub(jwtPayload.getSub());
                if (user.isEmpty()) user = userRepository.findByEmailAddress(jwtPayload.getEmail());
                if (user.isEmpty()) return false;
                if (validateUserRolesInMiddleware) {
                    validateRolesInThePayload(jwtPayload);
                }
                if (user.get().getLoginJourneyState().equals(LoginJourneyState.PRIVACY_POLICY_PENDING)) return false;
            }

            return !isTokenInBlacklist(customJwt);
        } catch (JWTVerificationException exception) {
            log.error("JWT verification failed", exception);
            return false;
        } catch (UnauthorizedException exception) {
            log.error("JWT payload verification failed", exception);
            return false;
        }
    }

    public String generateToken(Map<String, String> claims) {
        JWTClaimsSet.Builder jwtClaimsSet = new JWTClaimsSet.Builder();
        jwtClaimsSet.jwtID(UUID.randomUUID().toString());
        for (Map.Entry<String, String> entry : claims.entrySet()) {
            jwtClaimsSet.claim(entry.getKey(), entry.getValue());
        }

        jwtClaimsSet.issueTime(new Date())
                .expirationTime(new Date(ZonedDateTime.now(clock).toInstant().toEpochMilli()
                        + (jwtProperties.getExpiresAfter() * 1000 * 60)))
                .issuer(jwtProperties.getIssuer())
                .audience(jwtProperties.getAudience());

        String payload = JSONObjectUtils.toJSONString(jwtClaimsSet.build().toJSONObject());

        SdkBytes message = SdkBytes.fromString(payload, StandardCharsets.UTF_8);

        SignRequest signRequest = SignRequest.builder().signingAlgorithm(SigningAlgorithmSpec.RSASSA_PSS_SHA_256)
                .message(message).messageType(MessageType.RAW).keyId(signingKeyArn).build();
        SignResponse signedResponse = kmsClient.sign(signRequest);

        JwtHeader jwtHeader = JwtHeader.builder()
                .alg(String.valueOf(SigningAlgorithmSpec.RSASSA_PSS_SHA_256))
                .typ("JWT")
                .kid(signingKeyArn)
                .build();

        String header = HelperUtils.asJsonString(jwtHeader);

        return String.format("%s.%s.%s",
                Base64.encodeBase64URLSafeString(header.getBytes(StandardCharsets.UTF_8)),
                Base64.encodeBase64URLSafeString(payload.getBytes(StandardCharsets.UTF_8)),
                Base64.encodeBase64URLSafeString(signedResponse.signature().asByteArray()));
    }

    private boolean isTokenInBlacklist(final String customJwt) {
        return jwtBlacklistRepository.existsByJwtIs(customJwt);
    }

    public DecodedJWT decodedJwt(String normalisedJWT) {
        return JWT.decode(normalisedJWT);
    }

    public String decodeBase64ToJson(final String base64) {
        return StringUtils.newStringUtf8(Base64.decodeBase64(base64));
    }

    public JwtPayload decodeTheTokenPayloadInAReadableFormat(DecodedJWT jwt) {
        final String payloadJson = decodeBase64ToJson(jwt.getPayload());
        final JSONObject jsonObject = new JSONObject(payloadJson);
        final String sub = jwt.getSubject();
        final String roles = jsonObject.has("roles") ? jsonObject.getString("roles") : "";
        final String iss = jsonObject.getString("iss");
        final String idToken = jsonObject.has("idToken") ? jsonObject.getString("idToken") : "";
        final String aud = jsonObject.getString("aud");
        final int exp = jsonObject.getInt("exp");
        final int iat = jsonObject.getInt("iat");
        final String email = jsonObject.getString("email");
        return JwtPayload.builder()
                .sub(sub)
                .roles(roles)
                .idToken(idToken)
                .iss(iss)
                .aud(aud)
                .exp(exp)
                .iat(iat)
                .email(email)
                .build();
    }

    public Optional<User> getUserFromJwt(final HttpServletRequest request) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null || customJWTCookie.getValue() == null) {
            throw new UnauthorizedException("No JWT token provided");
        }
        final DecodedJWT decodedJwt = decodedJwt(customJWTCookie.getValue());
        final JwtPayload payload = decodeTheTokenPayloadInAReadableFormat(decodedJwt);
        return userRepository.findBySub(payload.getSub());
    }

    public JwtPayload validateRolesInThePayload(JwtPayload payload) {
        final List<Role> userRoles = oneLoginUserService.getUserBySub(payload.getSub()).getRoles();
        final String payloadRoles = payload.getRoles();
        oneLoginUserService.validateRoles(userRoles, payloadRoles);
        return payload;
    }
}
