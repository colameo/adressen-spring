package ch.example.adressen.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.EmailValidator;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, CharSequence> {

    // Local addresses (foo@bar, without a domain ending) are not allowed.
    private static final EmailValidator EMAIL = EmailValidator.getInstance(false);

    /** An absent value is fine (the field's nullability decides that); a present one has to be an address. */
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || EMAIL.isValid(value.toString());
    }
}
