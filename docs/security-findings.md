# Security Findings

Audit conducted 2026-04-02. Covers all REST controllers and their backing service layer.

---

## 1. Public endpoint leaks pending/rejected reviews

**File:** `apps/backend/src/main/java/com/sait/peelin/service/ReviewService.java`  
**Endpoint:** `GET /api/v1/products/{productId}/reviews`  
**Severity:** Medium

`ReviewService.forProduct()` returns all reviews with no status filter. Anonymous users can see reviews still in moderation (`pending`) and moderation decisions (`rejected`), including any customer comment text in those states.

**Recommended fix:**
```java
// Filter to approved only on the public endpoint
return reviewRepository.findByProduct_IdAndReviewStatus(productId, ReviewStatus.approved)
        .stream().map(this::toDto).toList();
```

---

## 2. `markRead` has no thread ownership check

**File:** `apps/backend/src/main/java/com/sait/peelin/service/ChatService.java`  
**Endpoint:** `POST /api/v1/chat/threads/{threadId}/read`  
**Severity:** Low

`markRead()` does not call `assertCanAccessThread()`. Any authenticated user who knows a thread ID can silently mark its messages as read, corrupting unread notification counts for the thread's actual participants.

All other thread-access methods (`messages`, `postMessage`) correctly call `assertCanAccessThread()` first.

**Recommended fix:**
```java
public void markRead(Integer threadId) {
    ChatThread t = chatThreadRepository.findById(threadId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
    assertCanAccessThread(t); // add this line
    ...
}
```

---

## 3. Employees can read any unassigned thread before claiming it

**File:** `apps/backend/src/main/java/com/sait/peelin/service/ChatService.java`  
**Endpoint:** `GET /api/v1/chat/threads/{threadId}/messages`  
**Severity:** Informational

`assertCanAccessThread()` allows any employee through if `t.getEmployeeUser() == null` (i.e. the thread hasn't been claimed yet). This means any employee can read a customer's chat history before formally taking ownership. Likely intentional to allow staff to triage, but worth being explicit about.

No code change recommended unless the intent is to restrict thread reading to assigned-only.

---

## What was reviewed and is fine

- **Rewards IDOR**: `RewardQueryService.listForCustomer()` verifies that a customer caller's own UUID matches the requested `customerId` before returning data. Staff bypass this check intentionally.
- **Chat thread IDOR**: `assertCanAccessThread()` correctly gates customers to their own thread and employees to threads they own or that are unassigned (see note 3 above).
- **Review submission**: `ReviewService.create()` checks `UserRole.customer` at the service layer — staff cannot submit reviews through the customer endpoint.
- **All admin/analytics endpoints**: properly gated with `@PreAuthorize` at controller level, redundant with service-layer role checks in some cases (defence in depth).
