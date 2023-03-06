package gov.cabinetofice.gapuserservice.validation;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import gov.cabinetofice.gapuserservice.validation.validators.PhoneNumberValidator;
import jakarta.validation.ConstraintValidatorContext;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class PhoneNumberValidatorTest {
    private final PhoneNumberValidator phoneNumberValidator = new PhoneNumberValidator(PhoneNumberUtil.getInstance());
    @Mock
    ConstraintValidatorContext constraintValidatorContext;

    @Test
    void validatePhoneNumber_ReturnsExpectedResponse() {

        boolean methodResponse = phoneNumberValidator.isValid("07123456789", constraintValidatorContext);
        assertTrue(methodResponse);
    }

    @Test
    void invalidPhoneNumber_ReturnsException() {

        boolean methodResponse = phoneNumberValidator.isValid("0716789", constraintValidatorContext);
        assertThat(methodResponse).isFalse();
    }

}
