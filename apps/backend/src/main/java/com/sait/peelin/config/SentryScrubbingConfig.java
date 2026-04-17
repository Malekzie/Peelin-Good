package com.sait.peelin.config;

import io.sentry.Breadcrumb;
import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Defense-in-depth for Sentry events: even with send-default-pii=false, custom code may still
 * attach request data / user email. Strip the highest-risk fields before transmission.
 */
@Configuration
public class SentryScrubbingConfig {

    private static final String REDACTED = "[redacted]";

    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i)^(authorization|cookie|set-cookie|x-api-key|x-auth-token|token|access_token|refresh_token|password|secret|stripe-signature)$");

    private static final Pattern SENSITIVE_QUERY_KEY = Pattern.compile(
            "(?i)(^|&)(token|access_token|refresh_token|code|state|id_token|password)=[^&]*");

    @Bean
    public EventProcessor sentryScrubbingProcessor() {
        return new EventProcessor() {
            @Override
            public SentryEvent process(SentryEvent event, Hint hint) {
                scrubRequest(event.getRequest());
                scrubUser(event.getUser());
                scrubBreadcrumbs(event.getBreadcrumbs());
                return event;
            }
        };
    }

    private static void scrubRequest(Request request) {
        if (request == null) return;
        request.setCookies(null);
        scrubHeaders(request.getHeaders());
        request.setQueryString(redactQueryParams(request.getQueryString()));
        request.setUrl(redactQueryParams(request.getUrl()));
    }

    private static void scrubHeaders(Map<String, String> headers) {
        if (headers == null) return;
        headers.replaceAll((k, v) -> SENSITIVE_KEY.matcher(k).matches() ? REDACTED : v);
    }

    private static String redactQueryParams(String value) {
        if (value == null || value.isEmpty()) return value;
        return SENSITIVE_QUERY_KEY.matcher(value).replaceAll("$1$2=" + REDACTED);
    }

    private static void scrubUser(User user) {
        if (user == null) return;
        user.setIpAddress(null);
        user.setEmail(null);
    }

    // Sentry SDK drops most breadcrumb data already; this is belt-and-suspenders on custom fields.
    private static void scrubBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        if (breadcrumbs == null) return;
        breadcrumbs.forEach(SentryScrubbingConfig::scrubBreadcrumbData);
    }

    private static void scrubBreadcrumbData(Breadcrumb breadcrumb) {
        Map<String, Object> data = breadcrumb.getData();
        if (data == null) return;
        data.replaceAll((k, v) ->
                SENSITIVE_KEY.matcher(k.toLowerCase(Locale.ROOT)).matches() ? REDACTED : v);
    }
}
