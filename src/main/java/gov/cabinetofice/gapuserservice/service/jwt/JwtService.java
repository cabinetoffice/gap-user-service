package gov.cabinetofice.gapuserservice.service.jwt;

public interface JwtService {
    boolean isTokenValid(final String thirdPartyJwt);
}
