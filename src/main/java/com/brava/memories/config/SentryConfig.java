package com.brava.memories.config;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/** Error tracking. Entirely inert unless SENTRY_DSN is set — Sentry.captureException calls
 *  elsewhere in the app are always safe to make; they silently no-op if this never initializes. */
@Component
public class SentryConfig {
    private final AppProperties props;
    public SentryConfig(AppProperties props) { this.props = props; }

    @PostConstruct
    void init() {
        String dsn = props.sentry().dsn();
        if (dsn == null || dsn.isBlank()) return;
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnvironment(props.sentry().environment());
            options.setTracesSampleRate(0.1);
        });
    }
}
