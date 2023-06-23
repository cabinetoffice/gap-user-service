package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.service.OneLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.one.login.enabled", havingValue = "true")
public class LoginControllerV2 {

    private final OneLoginService oneLoginService;

    @GetMapping("/redirect-after-login")
    public ResponseEntity<String> redirect(final @RequestParam String code) {

        String jwt = oneLoginService.createOneLoginJwt();
        String authToken = oneLoginService.getAuthToken(jwt, code);
        String userInfo = oneLoginService.getUserInfo(authToken);

        // Temporarily returning user info here for testing purposes
        return ResponseEntity.ok(userInfo);
    }

}