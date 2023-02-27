package gov.cabinetofice.gapuserservice.service.jwt.impl;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.exceptions.JwkNotValidTokenException;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Calendar;

@RequiredArgsConstructor
@Service
@Slf4j
public class ColaJwtServiceImpl implements JwtService {
    private final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties;
    private final JwkProvider jwkProvider;
    private final Mac sha256HMac;

    @Override
    public boolean isTokenValid(final String colaJwt) {
        final String trimmedToken = URLDecoder.decode(colaJwt, StandardCharsets.UTF_8).substring(2);

        if (!isValidColaSignature(trimmedToken)) {
            log.error("COLAs JWT signature is invalid");
            return false;
        }

        final DecodedJWT decodedJWT = decodeJwt(trimmedToken);
        if (!isValidJwtSignature(decodedJWT)) {
            log.error("JWTs signature is invalid");
            return false;
        }

        if (isTokenExpired(decodedJWT)) {
            return false;
        }

        boolean isExpectedIssuer = decodedJWT.getIssuer().equals(thirdPartyAuthProviderProperties.getDomain());
        boolean isExpectedAud = decodedJWT.getAudience().get(0).equals(thirdPartyAuthProviderProperties.getAppClientId());
        if (!isExpectedAud || !isExpectedIssuer) {
            throw new JwkNotValidTokenException("Third party token is not valid");
        }

        return true;
    }

    public DecodedJWT decodeJwt(final String colaJwt) {
        // Strips away the 4th part of COLAs JWT: COLAs signature
        final String jwt = colaJwt.substring(0, colaJwt.lastIndexOf('.'));
        return JWT.decode(jwt);
    }

    private boolean isTokenExpired(DecodedJWT jwt) {
        return jwt.getExpiresAt().before(Calendar.getInstance().getTime());
    }

    private boolean isValidJwtSignature(final DecodedJWT decodedJWT) {
        try {
            final Jwk jwk = jwkProvider.get(decodedJWT.getKeyId());
            final Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            algorithm.verify(decodedJWT);
        } catch (JwkException | SignatureVerificationException e) {
            log.error("An error occurred while verifying JWT signature", e);
            return false;
        }
        return true;
    }

    private boolean isValidColaSignature(final String jwt) {
        final String jwtWithoutSignature = jwt.substring(0, jwt.lastIndexOf('.'));
        final String signature = jwt.substring(jwt.lastIndexOf('.') + 1);
        final String hash = getBase64Sha256Hmac(jwtWithoutSignature).replace("=", "");
        return MessageDigest.isEqual(signature.getBytes(), hash.getBytes());
    }

    private String getBase64Sha256Hmac(final String value) {
        final byte[] hashBytes = sha256HMac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}
