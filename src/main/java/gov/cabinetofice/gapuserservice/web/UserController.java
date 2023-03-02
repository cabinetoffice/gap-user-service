package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.CreateUserDto;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UserController {

    private static final String REGISTRATION_SUCCESS_VIEW = "something";
    private static final String REGISTRATION_PAGE_VIEW = "something";

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid @RequestBody CreateUserDto user, BindingResult result) {

        if (result.hasErrors()) {
            return new ModelAndView(REGISTRATION_PAGE_VIEW);
        }

        //createGrantApplicantService.createNewUser(user);
        return new ModelAndView(REGISTRATION_SUCCESS_VIEW);
    }
}
