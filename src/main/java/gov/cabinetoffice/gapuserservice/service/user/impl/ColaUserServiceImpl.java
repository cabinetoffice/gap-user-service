package gov.cabinetoffice.gapuserservice.service.user.impl;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import gov.cabinetoffice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetoffice.gapuserservice.dto.CreateUserDto;
import gov.cabinetoffice.gapuserservice.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ColaUserServiceImpl implements UserService {

    private final ThirdPartyAuthProviderProperties authProviderProperties;
    private final AWSCognitoIdentityProvider cognitoClient;

    @Override
    public void createNewUser(final CreateUserDto applicantInformation) {
        // create our new user
        final AdminCreateUserResult createUserResult = addNewUserToCognito(cognitoClient, applicantInformation, authProviderProperties.getUserPassword());

        // verify the email address
        verifyEmailAddress(cognitoClient, createUserResult.getUser());

        // set the account password
        setPermanentPassword(cognitoClient, createUserResult.getUser(), authProviderProperties.getUserPassword());
    }

    @Override
    public boolean doesUserExist(final String email) {
        final AdminGetUserRequest getUserRequest = new AdminGetUserRequest()
                .withUserPoolId(authProviderProperties.getUserPoolId())
                .withUsername(email);
        try {
            cognitoClient.adminGetUser(getUserRequest);
            return true;
        } catch (UserNotFoundException e) {
            log.debug("user not found", e);
            return false;
        }
    }

    private String createCognitoFormattedPhoneNumberFromString(final String unformattedNumber) {
        final String telephoneCountryCode = "+44";

        if (!unformattedNumber.contains(telephoneCountryCode)) {
            final String telephoneWithoutLeadingZero = unformattedNumber.substring(1);
            return telephoneCountryCode.concat(telephoneWithoutLeadingZero);
        }

        return unformattedNumber;
    }

    private AdminCreateUserResult addNewUserToCognito(final AWSCognitoIdentityProvider cognitoClient, final CreateUserDto applicantInformation, final String temporaryPassword) {
        final String phoneNumber = createCognitoFormattedPhoneNumberFromString(applicantInformation.getTelephone());

        final AttributeType[] attributeTypes = new AttributeType[]{
                new AttributeType().withName("custom:features").withValue("user=ordinary_user"),
                new AttributeType().withName("custom:isAdmin").withValue("false"),
                new AttributeType().withName("custom:phoneNumber").withValue(phoneNumber),
                new AttributeType().withName("email").withValue(applicantInformation.getEmail()),
                new AttributeType().withName("family_name").withValue(applicantInformation.getLastName()),
                new AttributeType().withName("given_name").withValue(applicantInformation.getFirstName()),
                new AttributeType().withName("custom:lastLogin").withValue("1970-01-01T00:00:00Z")
        };

        final AdminCreateUserRequest userRequest =
                new AdminCreateUserRequest()
                        .withUserPoolId(authProviderProperties.getUserPoolId())
                        .withUsername(applicantInformation.getEmail())
                        .withUserAttributes(attributeTypes)
                        .withTemporaryPassword(temporaryPassword)
                        .withMessageAction(MessageActionType.SUPPRESS);

        return cognitoClient.adminCreateUser(userRequest);
    }

    private void verifyEmailAddress(final AWSCognitoIdentityProvider cognitoClient, final UserType user) {
        final AttributeType userAttributeEmailVerified = new AttributeType()
                .withName("email_verified")
                .withValue("true");

        final AdminUpdateUserAttributesRequest adminUpdateUserAttributesRequest = new AdminUpdateUserAttributesRequest()
                .withUsername(user.getUsername())
                .withUserPoolId(authProviderProperties.getUserPoolId())
                .withUserAttributes(userAttributeEmailVerified);

        cognitoClient.adminUpdateUserAttributes(adminUpdateUserAttributesRequest);
    }

    private void setPermanentPassword(final AWSCognitoIdentityProvider cognitoClient, final UserType user, final String password) {
        final AdminSetUserPasswordRequest adminSetUserPasswordRequest =
                new AdminSetUserPasswordRequest()
                        .withUsername(user.getUsername())
                        .withUserPoolId(authProviderProperties.getUserPoolId())
                        .withPassword(password)
                        .withPermanent(true);

        cognitoClient.adminSetUserPassword(adminSetUserPasswordRequest);
    }
}
