package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.dto.CreateUserDto;
import gov.cabinetofice.gapuserservice.service.user.impl.ColaUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private ColaUserServiceImpl colaUserServiceImpl;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private ThirdPartyAuthProviderProperties authProviderProperties;

    @Mock
    private FindAGrantConfigProperties findAGrantProps;

    @InjectMocks
    private RegistrationController controllerUnderTest;

    @Test
    void showRegistrationPage_ShouldShowTheCorrectView() {
        final CreateUserDto user = CreateUserDto.builder().build();
        final ModelAndView methodResponse = controllerUnderTest.showRegistrationPage(user);
        assertThat(methodResponse.getViewName()).isEqualTo(RegistrationController.REGISTRATION_PAGE_VIEW);
    }

    @Test
    void registerNewUser_ShouldRedirectToRegisterView_IfUsernameAlreadyExists() {

        final CreateUserDto user = CreateUserDto.builder()
                .email("email@test.com")
                .build();

        when(colaUserServiceImpl.doesUserExist(user.getEmail()))
                .thenReturn(true);
        when(bindingResult.hasErrors())
                .thenReturn(true);

        final ModelAndView methodResponse = controllerUnderTest.showRegistrationPage(user, bindingResult);

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

        verify(colaUserServiceImpl).createNewUser(user);
        assertThat(methodResponse.getViewName()).isEqualTo("redirect:/register/success");
    }

    @Test
    void showSuccessPage_ShowsSuccessPage_WithLoginUrl() {

        final String loginUrl = "https://some-domain.com/login";
        when(authProviderProperties.getLoginUrl()).thenReturn(loginUrl);

        final ModelAndView methodResponse = controllerUnderTest.showSuccessPage();

        assertThat(methodResponse.getViewName()).isEqualTo(RegistrationController.REGISTRATION_SUCCESS_VIEW);
        assertThat(methodResponse.getModel()).containsEntry("loginUrl", loginUrl);
    }
}