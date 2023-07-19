package gov.cabinetofice.gapuserservice.service.jwt.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.dto.JwtPayload;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CustomJwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;
    private final JwtBlacklistRepository jwtBlacklistRepository;
    private final Clock clock;
    private final RSAKey rsaKey;
    public CustomJwtServiceImpl(JwtProperties jwtProperties, JwtBlacklistRepository jwtBlacklistRepository, Clock clock) throws JOSEException {
        this.jwtProperties = jwtProperties;
        this.jwtBlacklistRepository = jwtBlacklistRepository;
        this.clock = clock;
        // Generate 2048-bit RSA key pair in JWK format, attach some metadata
        RSAKey jwk = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
                .issueTime(new Date()) // issued-at timestamp (optional)
                .generate();

        this.rsaKey = jwk;
    }

    @Override
    public boolean isTokenValid(final String customJwt) {
        try {
            final Algorithm signingKey = Algorithm.RSA256(this.rsaKey.toRSAPublicKey(), this.rsaKey.toRSAPrivateKey());
            final JWTVerifier verifier = JWT.require(signingKey)
                    .withIssuer(jwtProperties.getIssuer())
                    .withAudience(jwtProperties.getAudience())
                    .build();

            verifier.verify(customJwt);
            return !isTokenInBlacklist(customJwt);
        } catch (JWTVerificationException exception) {
            log.error("JWT verification failed", exception);
            return false;
        } catch (JOSEException exception) {
            log.error("isTokenValid failed", exception);
            throw new RuntimeException(exception);
        }
    }

    public String generateToken(Map<String, String> claims) {
        try {
            final Algorithm signingKey = Algorithm.RSA256(this.rsaKey.toRSAPublicKey(), this.rsaKey.toRSAPrivateKey());

            return JWT.create()
                    .withPayload(claims)
                    .withIssuer(jwtProperties.getIssuer())
                    .withAudience(jwtProperties.getAudience())
                    .withExpiresAt(new Date(ZonedDateTime.now(clock).toInstant().toEpochMilli() + (jwtProperties.getExpiresAfter() * 1000 * 60)))
                    .withKeyId(this.rsaKey.getKeyID())
                    .withIssuedAt(new Date())
                    .withJWTId(UUID.randomUUID().toString())
                    .sign(signingKey);
        } catch (JOSEException exception) {
            log.error("generateToken failed", exception);
            throw new RuntimeException(exception);
        }
    }

    public JWKSet getPublicJWKSet() {
        return new JWKSet(this.rsaKey.toPublicJWK());
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
        final String roles = jsonObject.getString("roles");
        final String iss = jsonObject.getString("iss");
        final String aud = jsonObject.getString("aud");
        final int exp = jsonObject.getInt("exp");
        final int iat = jsonObject.getInt("iat");
        final String email = jsonObject.getString("email");
        return JwtPayload.builder()
                .sub(sub)
                .roles(roles)
                .iss(iss)
                .aud(aud)
                .exp(exp)
                .iat(iat)
                .email(email)
                .build();
    }
}
