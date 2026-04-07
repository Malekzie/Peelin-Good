package com.sait.peelin.validation;

import com.sait.peelin.dto.v1.GuestCustomerRequest;
import com.sait.peelin.support.GuestContactFiller;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class GuestContactValidator implements ConstraintValidator<GuestContactValid, GuestCustomerRequest> {

    private static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

    @Override
    public boolean isValid(GuestCustomerRequest req, ConstraintValidatorContext ctx) {
        if (req == null) {
            return false;
        }
        String email = req.getEmail() == null ? "" : req.getEmail().trim();
        String digits = GuestContactFiller.normalizeDigits(req.getPhone());
        boolean hasEmail = !email.isEmpty();
        boolean hasPhone = digits.length() >= 10;
        if (!hasEmail && !hasPhone) {
            return false;
        }
        if (hasEmail && !EMAIL.matcher(email).matches()) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("Invalid email").addPropertyNode("email").addConstraintViolation();
            return false;
        }
        if (hasPhone && digits.length() < 10) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("Phone must have at least 10 digits").addPropertyNode("phone")
                    .addConstraintViolation();
            return false;
        }
        if (req.getMiddleInitial() != null && !req.getMiddleInitial().isBlank()) {
            String mi = req.getMiddleInitial().trim();
            if (mi.length() > 1 || !mi.matches("[A-Za-z]")) {
                ctx.disableDefaultConstraintViolation();
                ctx.buildConstraintViolationWithTemplate("Middle initial must be a single letter")
                        .addPropertyNode("middleInitial").addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
