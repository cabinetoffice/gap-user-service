package gov.cabinetoffice.gapuserservice.web;

import gov.cabinetoffice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetoffice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetoffice.gapuserservice.dto.CreateUserDto;
import gov.cabinetoffice.gapuserservice.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

@RequiredArgsConstructor
@Controller
@RequestMapping("/register")
public class RegistrationController {

    private final UserService userService;
    private final ThirdPartyAuthProviderProperties authProviderProperties;
    private final FindAGrantConfigProperties findProperties;

    public static final String REGISTRATION_SUCCESS_VIEW = "registration-success";
    public static final String REGISTRATION_PAGE_VIEW = "register-user";

    @GetMapping
    public ModelAndView showRegistrationPage(final @ModelAttribute("user") CreateUserDto user) {
        return new ModelAndView(REGISTRATION_PAGE_VIEW).addObject("homePageUrl", findProperties.getUrl() + "/apply/applicant");
    }

    @GetMapping("/success")
    public ModelAndView showSuccessPage() {
        return new ModelAndView(REGISTRATION_SUCCESS_VIEW)
                .addObject("loginUrl", authProviderProperties.getLoginUrl());
    }

    @PostMapping
    public ModelAndView showRegistrationPage(final @Valid @ModelAttribute("user") CreateUserDto user, final BindingResult result) {

        if (!StringUtils.isEmptyOrWhitespace(user.getEmail()) && userService.doesUserExist(user.getEmail())) {
            final FieldError duplicateEmailError = new FieldError("user",
                    "email",
                    user.getEmail(),
                    true,
                    null,
                    null,
                    "A user with this email already exists");
            result.addError(duplicateEmailError);
        }

        // TODO implement validation groups so that error messages display in the correct order on the front end.
        if (result.hasErrors()) {
            return new ModelAndView(REGISTRATION_PAGE_VIEW)
                    .addObject("homePageUrl", findProperties.getUrl());
        }

        userService.createNewUser(user);

        return new ModelAndView("redirect:/register/success");
    }
}
