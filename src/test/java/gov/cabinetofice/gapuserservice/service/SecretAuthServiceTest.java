package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SecretAuthServiceTest {
    @InjectMocks
    private SecretAuthService secretAuthService;

    @Test
    void shouldNotThrowUnauthorisedExceptionWhenSecretIsValid() {

        ReflectionTestUtils.setField(secretAuthService, "lambdaSecret", "validSecret");
        assertDoesNotThrow(() -> secretAuthService.authenticateSecret("validSecret"));

    }

    @Test
    void shouldThrowUnauthorisedExceptionWhenSecretIsInValid() {

        ReflectionTestUtils.setField(secretAuthService, "lambdaSecret", "validSecret");
        assertThrows(UnauthorizedException.class, () ->
                secretAuthService.authenticateSecret("InvalidSecret"));

    }

}
