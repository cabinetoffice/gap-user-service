package gov.cabinetoffice.gapuserservice.web.controlleradvice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ErrorMessage {
    private String message;

}