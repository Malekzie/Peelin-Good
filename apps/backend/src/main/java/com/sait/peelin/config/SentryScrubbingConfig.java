package com.sait.peelin.config;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Defense-in-depth for Sentry events: even with send-default-pii=false, custom code may still
 * attach request data / user email. Strip the highest-risk fields before transmission.
 */
@Configuration
public class SentryScrubbingConfig {

    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i)^(authorization|cookie|set-cookie|x-api-key|x-auth-token|token|access_token|refresh_token|password|secret|stripe-signature)$");

    private static final Pattern SENSITIVE_QUERY_KEY = Pattern.compile(
            "(?i)(^|&)(token|access_token|refresh_token|code|state|id_token|password)=[^&]*");

    @Bean
    public EventProcessor sentryScrubbingProcessor() {
        return new EventProcessor() {
            @Override
            public SentryEvent process(SentryEvent event, Hint hint) {
                Request request = event.getRequest();
                if (request != null) {
                    Map<String, String> headers = request.getHeaders();
                    if (headers != null) {
                        headers.replaceAll((k, v) -> SENSITIVE_KEY.matcher(k).matches() ? "[redacted]" : v);
                    }
                    request.setCookies(null);
                    String qs = request.getQueryString();
                    if (qs != null && !qs.isEmpty()) {
                        request.setQueryString(SENSITIVE_QUERY_KEY.matcher(qs).replaceAll("$1$2=[redacted]"));
                    }
                    String url = request.getUrl();
                    if (url != null) {
                        request.setUrl(SENSITIVE_QUERY_KEY.matcher(url).replaceAll("$1$2=[redacted]"));
                    }
                }

                User user = event.getUser();
                if (user != null) {
                    user.setIpAddress(null);
                    user.setEmail(null);
                }

                // Scrub breadcrumb data by best effort (Sentry SDK already drops most).
                if (event.getBreadcrumbs() != null) {
                    event.getBreadcrumbs().forEach(b -> {
                        Map<String, Object> data = b.getData();
                        if (data != null) {
                            data.replaceAll((k, v) ->
                                    SENSITIVE_KEY.matcher(k.toLowerCase(Locale.ROOT)).matches() ? "[redacted]" : v);
                        }
                    });
                }
                return event;
            }
        };
    }
}
