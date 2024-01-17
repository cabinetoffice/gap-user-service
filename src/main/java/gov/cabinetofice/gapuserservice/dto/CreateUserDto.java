package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.validation.annotations.ContainsOnlyAlphaChars;
import gov.cabinetofice.gapuserservice.validation.annotations.EmailAddressesMatch;
import gov.cabinetofice.gapuserservice.validation.annotations.PhoneNumberIsValid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EmailAddressesMatch.List({
        @EmailAddressesMatch(
                field = "email",
                fieldMatch = "emailConfirmed",
                message = "Email addresses must match"
        ),
})
public class CreateUserDto {
    @NotBlank(message = "Enter a first name")
    @ContainsOnlyAlphaChars(message = "First name must contain only letters")
    private String firstName;

    @NotBlank(message = "Enter a last name")
    @ContainsOnlyAlphaChars(message = "Last name must contain only letters")
    private String lastName;

    @NotBlank(message = "Enter an email address")
    @Email(message = "Enter an email address in the correct format, like name@example.com")
    @Size(max = 254, message = "Email address must be 254 characters or less")
    private String email;

    @NotBlank(message = "Enter an email address")
    @Email(message = "Enter an email address in the correct format, like name@example.com")
    @Size(max = 254, message = "Email address must be 254 characters or less")
    private String emailConfirmed;

    @PhoneNumberIsValid(message = "Enter a UK telephone number, like 07123456789", field = "telephone")
    private String telephone;

    @NotBlank(message = "You must confirm that you have read and agreed to the privacy policy")
    private String privacyPolicy;
}
