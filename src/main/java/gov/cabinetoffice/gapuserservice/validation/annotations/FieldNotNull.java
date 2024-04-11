package gov.cabinetoffice.gapuserservice.validation.annotations;

import gov.cabinetoffice.gapuserservice.validation.validators.NullFieldValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Constraint(validatedBy = NullFieldValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldNotNull {

    String message() default "This field cannot be null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
