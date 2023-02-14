package gov.cabinetofice.gapuserservice.service;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.exceptions.JwkNotValidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;

@RequiredArgsConstructor
@Service
public class ThirdPartyJwtService {
    private final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties;

    public DecodedJWT decodedJwt(String normalisedJWT) {
        return JWT.decode(normalisedJWT);
    }

    public boolean verifyToken(DecodedJWT jwt) throws JwkException {
        if (isTokenExpired(jwt)) {
            return false;
        }

        boolean isExpectedIssuer = jwt.getIssuer().equals(thirdPartyAuthProviderProperties.getDomain());
        boolean isExpectedAud = jwt.getAudience().get(0).equals(thirdPartyAuthProviderProperties.getAppClientId());
        if (!isExpectedAud || !isExpectedIssuer) {
            throw new JwkNotValidTokenException("Third party token is not valid");
        }
        JwkProvider provider = new UrlJwkProvider(thirdPartyAuthProviderProperties.getDomain());
        Jwk jwk = provider.get(jwt.getKeyId());
        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
        algorithm.verify(jwt);

        return true;
    }
    private boolean isTokenExpired(DecodedJWT jwt) {
        return jwt.getExpiresAt().before(Calendar.getInstance().getTime());
    }
}
