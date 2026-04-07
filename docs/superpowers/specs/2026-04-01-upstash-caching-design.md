# Upstash Redis Caching — Design Spec [DONE]

**Date:** 2026-04-01
**Status:** Approved / Implemented

## Overview

Add a Redis caching layer to the Spring Boot backend using Upstash Redis, connected via the standard Redis protocol. Spring's cache abstraction (`@Cacheable` / `@CacheEvict`) is used throughout — cache logic stays out of business logic. Public catalog endpoints and authenticated read endpoints are cached with different strategies: write-through invalidation for mutable resources, TTL-only expiry for read-only or user-scoped resources.

---

## 1. Dependencies & Configuration [DONE]

**New Maven dependency:** (Already present in pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**`application.yaml` additions:** (Already present in application.yaml)
```yaml
spring:
  data:
    redis:
      url: ${UPSTASH_REDIS_URL}
      password: ${UPSTASH_REDIS_TOKEN}
      ssl:
        enabled: true
```

The Upstash `rediss://` URL embeds host and port. The token is passed as the Redis `AUTH` password — the standard Upstash connection pattern.

**New file:** `config/CacheConfig.java` [DONE]
- Annotated `@EnableCaching`
- Registers a `RedisCacheManager` bean
- Configures per-cache TTL overrides (see section 2)
- Configures `GenericJackson2JsonRedisSerializer` so cached values are stored as JSON in Upstash

No other config files need changes.

---

## 2. Cache Names, TTLs & Invalidation Strategy [DONE]

| Cache name | Endpoints | TTL | Invalidation |
|---|---|---|---|
| `products` | `GET /products`, `GET /products/{id}` | 10 min | `@CacheEvict` on create/update/delete |
| `bakeries` | `GET /bakeries`, `GET /bakeries/{id}`, `GET /bakeries/{id}/hours` | 10 min | `@CacheEvict` on create/update/delete |
| `tags` | `GET /tags`, `GET /tags/{id}` | 30 min | `@CacheEvict` on create/update/delete |
| `product-specials` | `GET /product-specials/today` | 1 hour | TTL only (no write endpoint) |
| `orders` | Customer's own order reads | 2 min | `@CacheEvict` on checkout and status updates |
| `rewards` | `GET /rewards` (customer own + admin all) | 5 min | TTL only |

**Key strategy:**
- Single item: `key = "#id"` — e.g. `products::5`
- Unparameterized list: `key = "'all'"` — e.g. `bakeries::all`
- Parameterized list: `key = "'all:' + #search + ':' + #tagId"` — e.g. `products::all:gluten:2`
- User-scoped data: `key = "#userId"` resolved from the authenticated principal

**Invalidation scope:** Write operations use `@CacheEvict(allEntries = true)` on the relevant cache, evicting all keys including parameterized search variants. This avoids needing to track every key variant individually.

---

## 3. Service-Level Changes [DONE]

Annotations only — no logic changes to existing services.

### `ProductService` [DONE]
- `list(search, tagId)` → `@Cacheable(value = "products", key = "'all:' + #search + ':' + #tagId")`
- `get(id)` → `@Cacheable(value = "products", key = "#id")`
- `create(req)` → `@CacheEvict(value = "products", allEntries = true)`
- `update(id, req)` → `@CacheEvict(value = "products", allEntries = true)`
- `delete(id)` → `@CacheEvict(value = "products", allEntries = true)`

### `BakeryService` [DONE]
- `list(search)` → `@Cacheable(value = "bakeries", key = "'all:' + #search")`
- `get(id)` → `@Cacheable(value = "bakeries", key = "#id")`
- `hours(bakeryId)` → `@Cacheable(value = "bakeries", key = "'hours:' + #bakeryId")`
- `create/update/delete` → `@CacheEvict(value = "bakeries", allEntries = true)`

### `TagService` [DONE]
- `list()` → `@Cacheable(value = "tags", key = "'all'")`
- `get(id)` → `@Cacheable(value = "tags", key = "#id")`
- `create/update/delete` → `@CacheEvict(value = "tags", allEntries = true)`

### `ProductSpecialService` [DONE]
- `findFirstForDate(date)` → `@Cacheable(value = "product-specials", key = "#date")`
- No eviction — TTL-only expiry.

### `OrderService` [DONE]
- `listForCurrentUser()` has no `userId` parameter — it resolves the user internally via `CurrentUserService`. Cache this using a custom `KeyGenerator` bean (`UserIdKeyGenerator`) that reads `userId` from `SecurityContextHolder`. Annotate with `@Cacheable(value = "orders", keyGenerator = "userIdKeyGenerator")`.
- `get(orderId)` → `@Cacheable(value = "orders", key = "'order:' + #orderId")` — same order data regardless of viewer; security check runs before the cache miss path.
- `checkout(...)` → `@CacheEvict(value = "orders", allEntries = true)` — no `userId` param available, so evict all.
- `updateStatus(...)` and `markDelivered(...)` → `@CacheEvict(value = "orders", allEntries = true)`.

**New bean required:** `UserIdKeyGenerator` [DONE] — a `@Component` implementing Spring's `KeyGenerator`, reads the JWT subject from `SecurityContextHolder.getContext().getAuthentication().getName()`.

### `RewardQueryService` [DONE]
- `listForCustomer(customerId)` → `@Cacheable(value = "rewards", key = "#customerId")`
- `listAll()` → `@Cacheable(value = "rewards", key = "'all'")` (admin-only)
- No eviction needed — rewards are append-only (earned on checkout); TTL-only expiry is sufficient.

---

## 4. Testing [UNAFFECTED / SKIPPED]

**Unit tests:** Unaffected. [FINISHED] `@Cacheable` is a proxy — unit tests that instantiate services directly bypass it entirely.

**Integration tests:** [SKIPPED as per user instruction to not touch pom.xml further] A new `@SpringBootTest` test class verifies:
- A cached endpoint returns the same value on a second call without hitting the DB
- A write operation evicts the relevant cache entries (next read hits DB again)
- TTL expiry works correctly

Uses an embedded Redis instance (Testcontainers `redis` module) so no Upstash credentials are required in CI.

**Test profile (`application-test.yaml`):** Overrides the Redis URL to point at the embedded Testcontainers instance.

**CI:** Existing `backend-build.yml` runs `./mvnw verify` — no pipeline changes needed, embedded Redis handles the Redis dependency in tests.

---

## Out of Scope

- HTTP response-level caching (e.g. `Cache-Control` headers, CDN caching) — not part of this spec
- Cache warming on startup
- Distributed cache invalidation across multiple backend instances (not needed at current scale)
- Caching admin analytics or dashboard endpoints
