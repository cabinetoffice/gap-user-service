package gov.cabinetofice.gapuserservice.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UserController {

    @GetMapping("/register")
    public ModelAndView registerNewUser() {
        return new ModelAndView("register-user");
    }
}