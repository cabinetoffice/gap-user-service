package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.CreateUserDto;
import gov.cabinetofice.gapuserservice.service.UserService;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;


@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private RegistrationController controllerUnderTest;

    @Test
    void showRegistrationPage_ShouldShowTheCorrectView() {
        final CreateUserDto user = CreateUserDto.builder().build();
        final ModelAndView methodResponse = controllerUnderTest.showRegistrationPage(user);
        assertThat(methodResponse.getViewName()).isEqualTo(RegistrationController.REGISTRATION_PAGE_VIEW);
    }

    @Test
    void registerNewUser_ShouldRedirectToRegisterView_IfBindingResultErrors() {

        final CreateUserDto user = CreateUserDto.builder().build();

        when(bindingResult.hasErrors()).thenReturn(true);

        final ModelAndView methodResponse = controllerUnderTest.showRegistrationPage(user, bindingResult);

        assertThat(methodResponse.getViewName()).isEqualTo(RegistrationController.REGISTRATION_PAGE_VIEW);
    }

    @Test
    void registerNewUser_ShouldSaveNewUser_AndRedirectToSuccessPage_IfNoBindingResultErrors() {

        final CreateUserDto user = CreateUserDto.builder().build();

        when(bindingResult.hasErrors()).thenReturn(false);

        final ModelAndView methodResponse = controllerUnderTest.showRegistrationPage(user, bindingResult);

        verify(userService).createNewUser(user);
        assertThat(methodResponse.getViewName()).isEqualTo(RegistrationController.REGISTRATION_SUCCESS_VIEW);
    }
}