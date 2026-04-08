package com.sait.peelin.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneNumberFormatterTest {

    @Test
    void formatsTenDigitsAndDigitsInput() {
        assertEquals("(403) 555-1212", PhoneNumberFormatter.formatStoredPhone("4035551212"));
        assertEquals("(403) 555-1212", PhoneNumberFormatter.formatStoredPhone("(403) 555-1212"));
    }

    @Test
    void stripsLeadingOneCountryCode() {
        assertEquals("(403) 555-1212", PhoneNumberFormatter.formatStoredPhone("14035551212"));
        assertEquals("(403) 555-1212", PhoneNumberFormatter.formatStoredPhone("+1 (403) 555-1212"));
    }

    @Test
    void leavesSyntheticLongDigitStringsAsDigits() {
        String synthetic = "9561234567890";
        assertEquals(synthetic, PhoneNumberFormatter.formatStoredPhone(synthetic));
    }

    @Test
    void formatStoredPhoneOrNull_blankToNull() {
        assertNull(PhoneNumberFormatter.formatStoredPhoneOrNull(null));
        assertNull(PhoneNumberFormatter.formatStoredPhoneOrNull("   "));
    }

    @Test
    void formatStoredPhoneOrNull_formatsValue() {
        assertEquals("(555) 123-4567", PhoneNumberFormatter.formatStoredPhoneOrNull("5551234567"));
    }
}
