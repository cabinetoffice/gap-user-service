package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.service.OneLoginService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginControllerV2Test {

    @InjectMocks
    private LoginControllerV2 loginController;

    @Mock
    private OneLoginService oneLoginService;


}
