package com.sait.peelin.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fail-fast startup validator: if any required environment variable is missing or blank,
 * print a loud banner enumerating every offender and exit. This exists because silent
 * fallback to hard-coded dev defaults (e.g. `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD=Password1`)
 * has produced real incidents in the past.
 *
 * Registered in {@link com.sait.peelin.Application#main} so it fires on
 * {@code ApplicationEnvironmentPreparedEvent} — earlier than any @Value resolution.
 * Only enforced in the {@code prod} profile; dev/test see Spring's native placeholder errors.
 */
public class EnvValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    /**
     * Required only when {@code SPRING_PROFILES_ACTIVE} contains "prod". Dev/test resolve
     * placeholders differently (test yaml hard-codes app.jwt.secret), so Spring's own placeholder
     * failure already surfaces these loudly in those profiles. Each entry below documents why
     * the app is non-functional without it.
     */
    private static final Map<String, String> PROD_REQUIRED = new LinkedHashMap<>();
    static {
        PROD_REQUIRED.put("JWT_SECRET",                  "HS256/HS512 signing key for auth tokens. Must be at least 32 bytes of high-entropy secret.");
        PROD_REQUIRED.put("SPRING_DATASOURCE_URL",       "JDBC URL for managed Postgres.");
        PROD_REQUIRED.put("SPRING_DATASOURCE_USERNAME",  "Postgres role.");
        PROD_REQUIRED.put("SPRING_DATASOURCE_PASSWORD",  "Postgres password. Never commit to git.");
        PROD_REQUIRED.put("APP_CORS_ORIGINS",            "Comma-separated allowlist of browser origins; blank allows nothing.");
        PROD_REQUIRED.put("FRONTEND_URL",                "Public URL of the SvelteKit site; used in password-reset emails and OAuth redirects.");
        PROD_REQUIRED.put("STRIPE_SECRET_KEY",           "Stripe server key (sk_live_...). Required to create PaymentIntents.");
        PROD_REQUIRED.put("STRIPE_WEBHOOK_SECRET",       "whsec_... for signature verification. Without it, webhook endpoint accepts forged requests.");
        PROD_REQUIRED.put("STRIPE_PUBLISHABLE_KEY",      "pk_live_... sent to the browser to initialise Stripe.js.");
        PROD_REQUIRED.put("MAIL_USERNAME",               "SMTP auth; password-reset and welcome emails silently no-op without it.");
        PROD_REQUIRED.put("MAIL_PASSWORD",               "SMTP auth.");
        PROD_REQUIRED.put("VALKEY_HOST",                 "Redis/Valkey host; app boots without cache but performance is degraded.");
        PROD_REQUIRED.put("VALKEY_PASSWORD",             "Redis/Valkey auth.");
        PROD_REQUIRED.put("GOOGLE_CLIENT_ID",            "OAuth2 Google provider.");
        PROD_REQUIRED.put("GOOGLE_CLIENT_SECRET",        "OAuth2 Google provider.");
        PROD_REQUIRED.put("MICROSOFT_CLIENT_ID",         "OAuth2 Microsoft provider.");
        PROD_REQUIRED.put("MICROSOFT_CLIENT_SECRET",     "OAuth2 Microsoft provider.");
        PROD_REQUIRED.put("DO_SPACES_ENDPOINT",          "Object storage endpoint for customer uploads.");
        PROD_REQUIRED.put("DO_SPACES_BUCKET",            "Object storage bucket.");
        PROD_REQUIRED.put("DO_SPACES_KEY",               "Object storage access key.");
        PROD_REQUIRED.put("DO_SPACES_SECRET",            "Object storage secret key.");
        PROD_REQUIRED.put("DO_SPACES_BASE_URL",          "Public CDN base URL used when rendering image URLs to clients.");
        PROD_REQUIRED.put("SENTRY_DSN",                  "Error-reporting DSN; observability gap if unset.");
        PROD_REQUIRED.put("OPENROUTER_API_KEY",          "Spring AI provider key; recommendations and moderation silently degrade without it.");
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        Set<String> active = Set.of(env.getActiveProfiles());
        boolean isProd = active.contains("prod");

        // Only enforce in prod. Dev/test rely on Spring's native placeholder errors, which are
        // adequate when the developer is sitting at the terminal seeing the stacktrace.
        if (!isProd) return;

        Map<String, String> required = new LinkedHashMap<>(PROD_REQUIRED);

        List<String[]> missing = new ArrayList<>();
        for (Map.Entry<String, String> e : required.entrySet()) {
            String value = env.getProperty(e.getKey());
            if (value == null || value.isBlank()) {
                missing.add(new String[]{e.getKey(), e.getValue()});
            }
        }

        if (missing.isEmpty()) return;

        String banner = buildBanner(missing, active);
        // Use stderr directly; the logger may not be wired yet at this stage.
        System.err.println(banner);
        System.err.println("Exiting with status 1 — refusing to boot with missing required env vars in profile=prod.");
        System.exit(1);
    }

    private static String buildBanner(List<String[]> missing, Set<String> activeProfiles) {
        int width = 100;
        String bar = "!".repeat(width);
        StringBuilder sb = new StringBuilder("\n\n").append(bar).append("\n");
        sb.append(center("MISSING REQUIRED ENVIRONMENT VARIABLES", width)).append("\n");
        sb.append(center("profile=" + (activeProfiles.isEmpty() ? "(none)" : String.join(",", activeProfiles)), width)).append("\n");
        sb.append(bar).append("\n\n");
        for (String[] m : missing) {
            sb.append("  * ").append(m[0]).append("\n");
            wrap(m[1], width - 8).forEach(line -> sb.append("        ").append(line).append("\n"));
            sb.append("\n");
        }
        sb.append(bar).append("\n");
        sb.append("  Fix: set the variables above in your environment, .env file, or deployment\n");
        sb.append("       platform (DigitalOcean App Platform -> Settings -> App-Level Environment).\n");
        sb.append(bar).append("\n");
        return sb.toString();
    }

    private static String center(String s, int width) {
        int pad = Math.max(0, (width - s.length()) / 2);
        return " ".repeat(pad) + s;
    }

    private static List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        String[] words = s.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if (line.length() + w.length() + 1 > width && line.length() > 0) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(w);
        }
        if (line.length() > 0) out.add(line.toString());
        return out.isEmpty() ? Arrays.asList("") : out;
    }
}
