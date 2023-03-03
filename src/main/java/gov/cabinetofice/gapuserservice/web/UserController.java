package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.CreateUserDto;
import gov.cabinetofice.gapuserservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

@RequiredArgsConstructor
@Controller
public class UserController {

    private final UserService userService;

    private static final String REGISTRATION_SUCCESS_VIEW = "something";
    private static final String REGISTRATION_PAGE_VIEW = "register-user";

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid @RequestBody CreateUserDto user, BindingResult result) {

        if (result.hasErrors()) {
            return new ModelAndView(REGISTRATION_PAGE_VIEW);
        }

        userService.createNewUser(user);
        return new ModelAndView(REGISTRATION_SUCCESS_VIEW);
    }
}
