package gov.cabinetofice.gapuserservice.service.jwt.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomJwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;
    private final Calendar calendar;

    @Override
    public boolean isTokenValid(final String customJwt) {
        final Algorithm signingKey = Algorithm.HMAC256(jwtProperties.getSigningKey());

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

        return true;
    }

    public String generateToken() {
        // TODO look up user from database, or add additional details from decoded JWT if needed?

        final Algorithm signingKey = Algorithm.HMAC256(jwtProperties.getSigningKey());

        // currently this signs the JWT, it does not encrypt the whole object - not sure if we need to?
        return JWT.create()
                .withIssuer(jwtProperties.getIssuer())
                .withAudience(jwtProperties.getAudience())
                .withExpiresAt(new Date(calendar.getTimeInMillis() + (jwtProperties.getExpiresAfter() * 1000 * 60)))
                .sign(signingKey);
    }
}