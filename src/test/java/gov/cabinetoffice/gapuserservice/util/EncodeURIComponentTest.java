package gov.cabinetoffice.gapuserservice.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EncodeURIComponentTest {

    @Test
    void shouldEncodeUri() {
        String input = "/dashboard?applyMigration=SUCCEEDED&findMigration=SUCCEEDED";
        String output = EncodeURIComponent.encodeURI(input);

        Assertions.assertEquals("%2Fdashboard%3FapplyMigration%3DSUCCEEDED%26findMigration%3DSUCCEEDED", output);
    }
}
