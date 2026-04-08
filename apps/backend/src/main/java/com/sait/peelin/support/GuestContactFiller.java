package com.sait.peelin.support;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fills non-null {@code customer_email} / {@code customer_phone} when a guest provides only one contact method.
 * Synthetic values use a reserved domain and high numeric phone range to avoid colliding with real contacts.
 */
public final class GuestContactFiller {

    public static final String SYNTHETIC_EMAIL_SUFFIX = "@guest-checkout.invalid";

    private GuestContactFiller() {
    }

    public static String normalizeDigits(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }

    public static boolean isSyntheticGuestEmail(String email) {
        return email != null && email.toLowerCase().endsWith(SYNTHETIC_EMAIL_SUFFIX);
    }

    public static String syntheticEmailForPhoneDigits(String digits) {
        return "guest.p" + digits + SYNTHETIC_EMAIL_SUFFIX;
    }

    /**
     * 12-digit synthetic mobile-style number in a high range; unique index uses digits only.
     */
    public static String allocateSyntheticPhoneDigits() {
        long n = ThreadLocalRandom.current().nextLong(900_000_000_000L, 9_999_999_999_999L);
        return Long.toString(n);
    }
}
