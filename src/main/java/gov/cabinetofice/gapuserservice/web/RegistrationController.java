package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.CreateUserDto;
import gov.cabinetofice.gapuserservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ModelAndView showRegistrationPage() {
        return new ModelAndView(REGISTRATION_PAGE_VIEW);
    }

    @PostMapping
    public ModelAndView showRegistrationPage(@Valid CreateUserDto user, BindingResult result) {

        if (result.hasErrors()) {
            return new ModelAndView(REGISTRATION_PAGE_VIEW);
        }

        userService.createNewUser(user);
        return new ModelAndView(REGISTRATION_SUCCESS_VIEW);
    }
}
