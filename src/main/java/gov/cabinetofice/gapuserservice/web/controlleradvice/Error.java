package gov.cabinetofice.gapuserservice.web.controlleradvice;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Error {
    private String fieldName;
    private String errorMessage;
}
