package gov.cabinetofice.gapuserservice.dto;

public record MigrateFindResponseDto(Integer id, String hashedEmailAddress, String encryptedEmailAddress,
                                     String sub, String createdAt, String updatedAt, String emailAddress,
                                     boolean isNewUser) {

}