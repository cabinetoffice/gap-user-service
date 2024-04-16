package gov.cabinetoffice.gapuserservice.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("health")
@RestController
public class HealthController {

    @GetMapping
    public ResponseEntity<String> getHealthStatus() {
        return ResponseEntity.ok("OK");
    }
}
