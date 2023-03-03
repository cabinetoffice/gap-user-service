package gov.cabinetofice.gapuserservice.validation;

import gov.cabinetofice.gapuserservice.dto.CreateUserDto;
import gov.cabinetofice.gapuserservice.validation.annotations.EmailAddressesMatch;
import gov.cabinetofice.gapuserservice.validation.validators.EmailAddressMatchMatchValidator;
import jakarta.validation.ConstraintValidatorContext;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.hibernate.validator.internal.util.annotation.AnnotationDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Stream;


@ExtendWith(MockitoExtension.class)
class EmailAddressMatchValidatorTest {
    private EmailAddressMatchMatchValidator emailAddressesMatchValidator;

    private ConstraintValidatorContext validatorContext;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setup () {
        nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        validatorContext = mock(ConstraintValidatorContext.class);

        emailAddressesMatchValidator = new EmailAddressMatchMatchValidator();
    }

    private static Stream<Arguments> provideRegistrationData() {
        return Stream.of(

                // validation only kicks in if both values are not blank
                Arguments.of(
                        CreateUserDto.builder()
                        .email("")
                        .emailConfirmed("")
                        .build()
                ),
                Arguments.of(
                        CreateUserDto.builder()
                                .email("email@test.com")
                                .emailConfirmed("")
                                .build()
                ),
                Arguments.of(
                        CreateUserDto.builder()
                                .email("")
                                .emailConfirmed("email@test.com")
                                .build()
                ),

                // email addresses match
                Arguments.of(
                        CreateUserDto.builder()
                                .email("email@test.com")
                                .emailConfirmed("email@test.com")
                                .build()
                )
        );
    }

    @MethodSource("provideRegistrationData")
    @ParameterizedTest
    void isValid_ReturnsTrueWhenExpected(CreateUserDto registrationData) {
        final String emailField = "email";
        final String emailMatchField = "emailConfirmed";
        final Map<String, Object> map = Map.of("field",emailField, "fieldMatch", emailMatchField);
        final EmailAddressesMatch fieldsMatch = new AnnotationDescriptor.Builder<>(EmailAddressesMatch.class, map).build().getAnnotation();

        emailAddressesMatchValidator.initialize(fieldsMatch);
        boolean methodResponse = emailAddressesMatchValidator.isValid(registrationData, validatorContext);

        assertThat(methodResponse).isTrue();
    }

    @Test
    void isValid_ReturnsFalseWhenExpected() {
        final String message = "fields do not match";
        final String emailField = "email";
        final String emailMatchField = "emailConfirmed";
        final Map<String, Object> map = Map.of("field",emailField, "fieldMatch", emailMatchField, "message", message);
        final EmailAddressesMatch fieldsMatch = new AnnotationDescriptor.Builder<>(EmailAddressesMatch.class, map).build().getAnnotation();

        final CreateUserDto registrationData = CreateUserDto.builder()
                .email("an-email@test.com")
                .emailConfirmed("another-email@test.com")
                .build();

        when(validatorContext.buildConstraintViolationWithTemplate(Mockito.anyString()))
                .thenReturn(builder);
        when(validatorContext.getDefaultConstraintMessageTemplate())
                .thenReturn(message);
        when(builder.addPropertyNode(Mockito.anyString()))
                .thenReturn(nodeBuilder);

        emailAddressesMatchValidator.initialize(fieldsMatch);
        boolean methodResponse = emailAddressesMatchValidator.isValid(registrationData, validatorContext);

        verify(validatorContext, atLeastOnce()).buildConstraintViolationWithTemplate(message);
        verify(builder, atLeastOnce()).addPropertyNode(emailField);
        verify(builder, atLeastOnce()).addPropertyNode(emailMatchField);

        assertThat(methodResponse).isFalse();
    }
}
