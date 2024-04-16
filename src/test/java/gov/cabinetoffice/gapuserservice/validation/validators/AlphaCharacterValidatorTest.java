package gov.cabinetoffice.gapuserservice.validation.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class AlphaCharacterValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private AlphaCharacterValidator validatorUnderTest;

    @BeforeEach
    void setup() {
        validatorUnderTest = new AlphaCharacterValidator();
    }

    private static Stream<Arguments> provideValidationData() {
        return Stream.of(
                Arguments.of("Smith", true),
                Arguments.of("John Smith", true),
                Arguments.of("John Smith-Doe", true),
                Arguments.of("John O'Smith", true),
                Arguments.of("Jóhn Smȋth", true),
                Arguments.of("John Smith 2", false),
                Arguments.of("2 John 2 Smith", false),
                Arguments.of("John,", false),
                Arguments.of("$!@%^&*", false)
                // That's plenty, I'm not unit testing the Apache commons lib...
        );
    }

    @MethodSource("provideValidationData")
    @ParameterizedTest
    void isValidReturnsExpectedResult(final String value, final boolean expectedResult) {
        final boolean result = validatorUnderTest.isValid(value, context);
        assertThat(result).isEqualTo(expectedResult);
    }

}