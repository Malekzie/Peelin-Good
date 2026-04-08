# Sentry Integration — Desktop

**Platform:** Desktop (Workshop5, JavaFX) — possibly also Spring Boot backend  
**Status:** Planning / Optional

---

## What it does

Integrate Sentry for error tracking so unhandled exceptions in the desktop app (and optionally the backend) are automatically reported to a Sentry project dashboard.

---

## Scope

- Sentry SDK in Workshop5 (JavaFX)
- Capture unhandled exceptions automatically
- Tag events with user ID and role from `UserSession`
- Optional: backend (`spring-boot-starter-actuator` + Sentry Spring Boot SDK)

---

## Desktop changes (Workshop5)

**Dependency (`pom.xml`):**
```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry</artifactId>
    <version><!-- latest --></version>
</dependency>
```

**Initialisation (in `MainApplication.start()`):**
```java
Sentry.init(options -> {
    options.setDsn(System.getenv("SENTRY_DSN"));
    options.setEnvironment("production"); // or "dev"
});
```

**User context (after login in `LoginController`):**
```java
Sentry.configureScope(scope -> {
    scope.setUser(new User(session.getUserId(), session.getUsername(), null, null));
    scope.setTag("role", session.getRole());
});
```

**Unhandled exceptions:** JavaFX `Thread.setDefaultUncaughtExceptionHandler` → `Sentry.captureException()`

---

## Backend changes (Workshop7, optional)

```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
</dependency>
```

Add `sentry.dsn=${SENTRY_DSN}` to `application.yaml`. Spring Boot auto-configuration handles the rest.

---

## Open questions

- Is Sentry being self-hosted or using sentry.io (free tier)?
- Is backend Sentry in scope for the demo, or desktop only?
- Should this be gated on a `SENTRY_DSN` env var so it's skipped locally when not set?
