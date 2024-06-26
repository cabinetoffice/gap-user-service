package gov.cabinetoffice.gapuserservice.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import gov.cabinetoffice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetoffice.gapuserservice.dto.CreateUserDto;
import gov.cabinetoffice.gapuserservice.service.user.impl.ColaUserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColaUserServiceImplTest {

    private ThirdPartyAuthProviderProperties cognitoProps;
    @Mock
    private AWSCognitoIdentityProvider cognitoClient;
    private ColaUserServiceImpl serviceUnderTest;

    @Captor
    private ArgumentCaptor<AdminCreateUserRequest> adminUserRequestCaptor;

    @Captor
    private ArgumentCaptor<AdminUpdateUserAttributesRequest> emailVerificationCaptor;

    @Captor
    private ArgumentCaptor<AdminSetUserPasswordRequest> passwordResetCaptor;

    @BeforeEach
    void setup() {
        cognitoProps = ThirdPartyAuthProviderProperties.builder()
                .accessKey("an-access-key")
                .secretKey("a-secret-key")
                .userPoolId("a-user-pool-id")
                .region("eu-west-2")
                .userPassword("a-user-password")
                .userPoolId("a-user-pool-id")
                .build();

        serviceUnderTest = new ColaUserServiceImpl(cognitoProps, cognitoClient);
    }

    @Test
    void createNewUser_CreatesUserInCognito() {

        final String unformattedTelephoneNumber = "00000000000";
        final CreateUserDto applicantToCreate = CreateUserDto.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith.test@cabinetoffice.gov.uk")
                .emailConfirmed("john.smith.test@cabinetoffice.gov.uk")
                .telephone(unformattedTelephoneNumber)
                .build();

        final UserType createdUser = new UserType();
        createdUser.setUserCreateDate(new Date());
        createdUser.setUsername("john.smith.test@cabinetoffice.gov.uk");

        final AdminCreateUserResult createUserResult = new AdminCreateUserResult()
                .withUser(createdUser);

        when(cognitoClient.adminCreateUser(Mockito.any()))
                .thenReturn(createUserResult);

        serviceUnderTest.createNewUser(applicantToCreate);

        assertUserIsCreated(applicantToCreate, unformattedTelephoneNumber);
        assertEmailAddressIsVerified(createdUser);
        assertPasswordIsReset(createdUser);
    }

    private void assertPasswordIsReset(UserType createdUser) {

        verify(cognitoClient).adminSetUserPassword(passwordResetCaptor.capture());

        final AdminSetUserPasswordRequest attributes = passwordResetCaptor.getValue();
        assertThat(attributes.getUserPoolId()).isEqualTo(cognitoProps.getUserPoolId());
        assertThat(attributes.getUsername()).isEqualTo(createdUser.getUsername());
        assertThat(attributes.getPermanent()).isTrue();
        assertThat(attributes.getPassword()).isEqualTo(cognitoProps.getUserPassword());
    }

    private void assertEmailAddressIsVerified(UserType user) {

        verify(cognitoClient).adminUpdateUserAttributes(emailVerificationCaptor.capture());

        final AdminUpdateUserAttributesRequest attributes = emailVerificationCaptor.getValue();
        assertThat(attributes.getUsername()).isEqualTo(user.getUsername());
        assertThat(attributes.getUserPoolId()).isEqualTo(cognitoProps.getUserPoolId());

        assertContainsAttributeWithValue(attributes.getUserAttributes(), "email_verified", "true");
    }

    void assertUserIsCreated(CreateUserDto applicantToCreate, String unformattedTelephoneNumber) {
        // make sure we actually send the request to create a user and capture the result of it
        verify(cognitoClient).adminCreateUser(adminUserRequestCaptor.capture());

        // test those values against the data we passed in
        AdminCreateUserRequest newUserRequest = adminUserRequestCaptor.getValue();

        assertThat(newUserRequest.getUserPoolId()).isEqualTo(cognitoProps.getUserPoolId());
        assertThat(newUserRequest.getUsername()).isEqualTo(applicantToCreate.getEmail());

        // check the data contained inside the custom attributes sent to Cognito
        final List<AttributeType> attributes = newUserRequest.getUserAttributes();
        assertContainsAttributeWithValue(attributes, "custom:features", "user=ordinary_user");
        assertContainsAttributeWithValue(attributes, "custom:isAdmin", "false");
        assertContainsAttributeWithValue(attributes, "custom:phoneNumber", "+44".concat(unformattedTelephoneNumber.substring(1)));
        assertContainsAttributeWithValue(attributes, "email", applicantToCreate.getEmail());
        assertContainsAttributeWithValue(attributes, "family_name", applicantToCreate.getLastName());
        assertContainsAttributeWithValue(attributes, "given_name", applicantToCreate.getFirstName());
        assertContainsAttributeWithValue(attributes, "custom:lastLogin", "1970-01-01T00:00:00Z");
    }

    private void assertContainsAttributeWithValue(List<AttributeType> attributes, String attributeName, String value) {
        attributes.stream()
                .filter(a -> a.getName().equals(attributeName))
                .findAny()
                .ifPresentOrElse(
                        a -> assertThat(a.getValue()).isEqualTo(value),
                        () -> Assertions.fail(String.format("No attribute with name '%s' found", attributeName))
                );
    }

    @Test
    void doesUserExist_ShouldThrowException_IfNotExplicitlyCaught() {
        final String emailAddress = "email@some-domain.com";
        final AdminGetUserRequest getUserRequest = new AdminGetUserRequest()
                .withUserPoolId(cognitoProps.getUserPoolId())
                .withUsername(emailAddress);

        doThrow(RuntimeException.class).when(cognitoClient).adminGetUser(any());

        assertThrows(RuntimeException.class, () -> serviceUnderTest.doesUserExist(emailAddress));
        verify(cognitoClient).adminGetUser(getUserRequest);
    }

    @Test
    void doesUserExist_ShouldReturnFalse_ifUserNotFoundExceptionThrown() {

        final String emailAddress = "email@some-domain.com";
        final AdminGetUserRequest getUserRequest = new AdminGetUserRequest()
                .withUserPoolId(cognitoProps.getUserPoolId())
                .withUsername(emailAddress);

        doThrow(UserNotFoundException.class).when(cognitoClient).adminGetUser(any());

        final boolean methodResponse = serviceUnderTest.doesUserExist(emailAddress);

        assertThat(methodResponse).isFalse();
        verify(cognitoClient).adminGetUser(getUserRequest);
    }

    @Test
    void doesUserExist_ShouldReturnTrue_IfUserReturnedFromCognito() {

        final String emailAddress = "email@some-domain.com";
        final AdminGetUserRequest getUserRequest = new AdminGetUserRequest()
                .withUserPoolId(cognitoProps.getUserPoolId())
                .withUsername(emailAddress);

        final AdminGetUserResult cognitoUser = new AdminGetUserResult();
        when(cognitoClient.adminGetUser(getUserRequest))
                .thenReturn(cognitoUser);

        final boolean methodResponse = serviceUnderTest.doesUserExist(emailAddress);

        assertThat(methodResponse).isTrue();
        verify(cognitoClient).adminGetUser(getUserRequest);
    }
}
