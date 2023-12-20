package gov.cabinetofice.gapuserservice.validation.validators;

import gov.cabinetofice.gapuserservice.validation.annotations.FieldNotNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NullFieldValidator implements ConstraintValidator<FieldNotNull, Object> {
    @Override
    public boolean isValid(Object fieldValue, ConstraintValidatorContext constraintValidatorContext) {
        return fieldValue != null;
    }
}
