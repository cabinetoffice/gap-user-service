package gov.cabinetoffice.gapuserservice.validation.validators;

import gov.cabinetoffice.gapuserservice.validation.annotations.ContainsOnlyAlphaChars;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class AlphaCharacterValidator implements ConstraintValidator<ContainsOnlyAlphaChars, String> {
    @Override
    public boolean isValid(String fieldValue, ConstraintValidatorContext constraintValidatorContext) {
        boolean validWithDashes = false;
        boolean validWithApostrophe = false;

        if (StringUtils.contains(fieldValue, "-")) {
            final String valueWithoutDashes = StringUtils.remove(fieldValue, "-");
            validWithDashes = StringUtils.isAlphaSpace(valueWithoutDashes);
        }

        if (StringUtils.contains(fieldValue, "'")) {
            final String valueWithoutApostrophe = StringUtils.remove(fieldValue, "'");
            validWithApostrophe = StringUtils.isAlphaSpace(valueWithoutApostrophe);
        }

        return validWithDashes || validWithApostrophe || StringUtils.isAlphaSpace(fieldValue);
    }
}
