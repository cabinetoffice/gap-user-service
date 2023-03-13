package gov.cabinetofice.gapuserservice.service.jwt.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.proc.JWTProcessorConfiguration;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;

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
    @SneakyThrows
    public boolean isTokenValid(final String customJwt) {
        final Algorithm signingKey = Algorithm.RSA256(this.rsaKey.toRSAPublicKey(), this.rsaKey.toRSAPrivateKey());

        try {
            // by default this verifies JWT was signed by same key and is not expired
            // but we can add additional checks, such as must match specific issuer etc.
            final JWTVerifier verifier = JWT.require(signingKey)
                    .withIssuer(jwtProperties.getIssuer())
                    .withAudience(jwtProperties.getAudience())
                    .build();

            verifier.verify(customJwt);
        } catch (JWTVerificationException exception){
            log.error("JWT verification failed", exception);
            return false;
        }


        return !isTokenInBlacklist(customJwt);
    }

    private boolean isTokenInBlacklist(final String customJwt) {
        return jwtBlacklistRepository.existsByJwtIs(customJwt);
    }

    @SneakyThrows
    public String generateToken() {
        // TODO look up user from database, or add additional details from decoded JWT if needed?

        final Algorithm signingKey = Algorithm.RSA256(this.rsaKey.toRSAPublicKey(), this.rsaKey.toRSAPrivateKey());

        Map<String, String> claims = new HashMap<>();
        claims.put("custom:features", "dept=Cabinet Office,user=administrator,user=ordinary_user");
        // currently this signs the JWT, it does not encrypt the whole object - not sure if we need to?
        return JWT.create()
                .withSubject("test-sub")
                .withIssuer(jwtProperties.getIssuer())
                .withAudience(jwtProperties.getAudience())
                .withExpiresAt(new Date(ZonedDateTime.now(clock).toInstant().toEpochMilli() + (jwtProperties.getExpiresAfter() * 1000 * 60)))
                .withKeyId(this.rsaKey.getKeyID())
                .withIssuedAt(new Date())
                .withJWTId("jwt-id")
                .withPayload(claims)
                .sign(signingKey);
    }

    @SneakyThrows
    public String generateToken2(Map<String, String> claims) {
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
    }

    public RSAKey getRsaKey() {
        return this.rsaKey;
    }

    public JWKSet getJWKSet() {
        return new JWKSet(this.rsaKey);
    }
}
