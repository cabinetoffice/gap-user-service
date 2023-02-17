package gov.cabinetofice.gapuserservice.service;

public interface ThirdPartyJwtService {
    boolean isTokenValid(final String thirdPartyJwt);
}
