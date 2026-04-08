package com.sait.peelin.support;

import org.springframework.util.StringUtils;

/**
 * Normalizes North American phone numbers to a single stored display form: {@code (###) ###-####}.
 * Values that are not exactly 10 digits (after optional leading country code {@code 1}), such as
 * synthetic guest placeholders, are stored as digits only.
 */
public final class PhoneNumberFormatter {

    private PhoneNumberFormatter() {
    }

    /**
     * @return formatted NANP string, digits-only for non-standard lengths, or empty if {@code raw} is blank
     */
    public static String formatStoredPhone(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String trimmed = raw.trim();
        String digits = GuestContactFiller.normalizeDigits(trimmed);
        if (digits.isEmpty()) {
            return trimmed;
        }
        String ten = toTenDigitNanp(digits);
        if (ten != null) {
            return "(" + ten.substring(0, 3) + ") " + ten.substring(3, 6) + "-" + ten.substring(6);
        }
        return digits;
    }

    /**
     * @return formatted phone or {@code null} when {@code raw} is null/blank
     */
    public static String formatStoredPhoneOrNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String formatted = formatStoredPhone(raw);
        return formatted.isEmpty() ? null : formatted;
    }

    private static String toTenDigitNanp(String digits) {
        if (digits.length() == 10) {
            return digits;
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            return digits.substring(1);
        }
        return null;
    }
}
