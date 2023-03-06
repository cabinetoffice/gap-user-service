package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.CreateUserDto;
import gov.cabinetofice.gapuserservice.service.user.UserService;
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

@RequiredArgsConstructor
@Controller
@RequestMapping("/register")
public class RegistrationController {

    private final UserService userService;

    public static final String REGISTRATION_SUCCESS_VIEW = "something";
    public static final String REGISTRATION_PAGE_VIEW = "register-user";

    @GetMapping
    public ModelAndView showRegistrationPage(final @ModelAttribute("user") CreateUserDto user) {
        return new ModelAndView(REGISTRATION_PAGE_VIEW);
    }

    @PostMapping
    public ModelAndView showRegistrationPage(final @Valid @ModelAttribute("user") CreateUserDto user, final BindingResult result) {

        if(userService.doesUserExist(user)) {
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
            return new ModelAndView(REGISTRATION_PAGE_VIEW);
        }

        userService.createNewUser(user);
        return new ModelAndView(REGISTRATION_SUCCESS_VIEW);
    }
}
