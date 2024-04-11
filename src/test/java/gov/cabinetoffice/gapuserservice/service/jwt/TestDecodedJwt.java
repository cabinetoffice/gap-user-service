package gov.cabinetoffice.gapuserservice.service.jwt;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;
import java.util.List;
import java.util.Map;

@ToString
@Builder
@Getter
public class TestDecodedJwt implements DecodedJWT {

    private Date expiresAt;
    private String signingKey;
    private String token;
    private String header;
    private String payload;
    private String signature;
    private String algorithm;
    private String type;
    private String contentType;
    private String keyId;
    private String issuer;
    private String subject;
    private List<String> audience;
    private Date notBefore;
    private Date issuedAt;
    private String id;
    private Map<String, Claim> claims;
    private Claim claim;

    @Getter(value = AccessLevel.NONE)
    private Claim headerClaim;

    @Override
    public Claim getHeaderClaim(String s) {
        return headerClaim;
    }

    @Override
    public Claim getClaim(String s) {
        return claim;
    }
}