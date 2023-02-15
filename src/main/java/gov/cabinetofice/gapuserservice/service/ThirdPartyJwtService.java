package gov.cabinetofice.gapuserservice.service;

public interface ThirdPartyJwtService {
    boolean verifyToken(final String thirdPartyJwt);
}
