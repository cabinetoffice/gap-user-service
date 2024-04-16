package gov.cabinetoffice.gapuserservice.validation.validators;

import gov.cabinetoffice.gapuserservice.validation.annotations.FieldNotNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NullFieldValidator implements ConstraintValidator<FieldNotNull, Object> {
    @Override
    public boolean isValid(Object fieldValue, ConstraintValidatorContext constraintValidatorContext) {
        return fieldValue != null;
    }
}
