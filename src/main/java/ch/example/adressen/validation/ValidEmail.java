package ch.example.adressen.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A well-formed email address with a domain ending ({@code foo@bar.ch}, not {@code foo@bar}). Used for
 * the {@code Email} fields of the model; checked by Apache Commons Validator.
 */
@Documented
@Constraint(validatedBy = ValidEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    String message() default "must be a well-formed email address";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
