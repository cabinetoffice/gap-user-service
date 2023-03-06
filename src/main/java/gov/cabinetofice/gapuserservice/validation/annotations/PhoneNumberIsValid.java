package gov.cabinetofice.gapuserservice.validation.annotations;

import gov.cabinetofice.gapuserservice.validation.validators.PhoneNumberValidator;
import jakarta.validation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumberIsValid {

    String message() default "Please use a valid UK mobile number";

    String field();

    Class<?>[] groups() default {};
    Class<?>[] payload() default {};
}
