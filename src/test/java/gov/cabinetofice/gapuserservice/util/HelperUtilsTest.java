package gov.cabinetofice.gapuserservice.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class HelperUtilsTest {
    @Test
    void shouldRemoveSquareBracketsAndTrimAndReturnASet() {
        List<String> input = List.of("[FIND ", "APPLY", "ADMIN] ");
        Set<String> output = Set.of("FIND", "APPLY", "ADMIN");
        Set<String> result = HelperUtils.removeSquareBracketsAndTrim(input);
        assert(result.equals(output));
    }

}
