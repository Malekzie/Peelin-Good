# WebSocket Chat — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add STOMP WebSocket infrastructure to the Spring Boot backend, extend customer-support chat with category/close support, and build the staff-to-staff direct messaging system.

**Architecture:** REST + WebSocket hybrid — REST handles all persistence, WebSocket pushes new messages, typing indicators, and read receipts. Spring's built-in simple STOMP broker over SockJS. JWT auth via `StompChannelInterceptor` on CONNECT frames.

**Tech Stack:** Spring Boot 3.5.11, `spring-boot-starter-websocket`, STOMP/SockJS, `SimpMessagingTemplate`, Nimbus JOSE JWT, Spring Security

---

## Scope

This plan covers **Workshop7 backend only** (`apps/backend/`). Frontend (SvelteKit), Android (Workshop06), and Desktop (Workshop5) require separate plans.

---

## File Structure

**New files:**

| Path | Responsibility |
|------|----------------|
| `src/main/resources/db/migration/V42__websocket_chat.sql` | Alter `chat_thread`, create `staff_conversation` + `staff_message` |
| `src/main/java/com/sait/peelin/config/StompWebSocketConfig.java` | Enable STOMP broker, register `/ws` endpoint |
| `src/main/java/com/sait/peelin/security/StompChannelInterceptor.java` | JWT auth on STOMP CONNECT frames |
| `src/main/java/com/sait/peelin/model/StaffConversation.java` | `staff_conversation` JPA entity |
| `src/main/java/com/sait/peelin/model/StaffMessage.java` | `staff_message` JPA entity |
| `src/main/java/com/sait/peelin/repository/StaffConversationRepository.java` | DB queries for staff conversations |
| `src/main/java/com/sait/peelin/repository/StaffMessageRepository.java` | DB queries for staff messages |
| `src/main/java/com/sait/peelin/dto/v1/TypingPayload.java` | WS typing indicator payload |
| `src/main/java/com/sait/peelin/dto/v1/ReadReceiptPayload.java` | WS read receipt payload |
| `src/main/java/com/sait/peelin/dto/v1/StaffConversationDto.java` | Staff conversation response |
| `src/main/java/com/sait/peelin/dto/v1/StaffMessageDto.java` | Staff message response |
| `src/main/java/com/sait/peelin/dto/v1/SendStaffMessageRequest.java` | POST body for sending DM |
| `src/main/java/com/sait/peelin/dto/v1/StartConversationRequest.java` | POST body for creating conversation |
| `src/main/java/com/sait/peelin/controller/ws/ChatWebSocketController.java` | WS typing + read relay for support chat |
| `src/main/java/com/sait/peelin/service/StaffMessageService.java` | Staff-to-staff messaging business logic |
| `src/main/java/com/sait/peelin/controller/v1/StaffMessageController.java` | REST endpoints for staff DMs |
| `src/main/java/com/sait/peelin/controller/ws/StaffMessageWebSocketController.java` | WS typing + read relay for staff DMs |
| `src/test/java/com/sait/peelin/security/StompChannelInterceptorTest.java` | Unit tests for STOMP auth |
| `src/test/java/com/sait/peelin/service/ChatServiceTest.java` | Unit tests for ChatService extensions |
| `src/test/java/com/sait/peelin/service/StaffMessageServiceTest.java` | Unit tests for StaffMessageService |
| `src/test/java/com/sait/peelin/controller/v1/ChatRestControllerTest.java` | MockMvc tests for chat REST extensions |
| `src/test/java/com/sait/peelin/controller/v1/StaffMessageControllerTest.java` | MockMvc tests for staff message REST |

**Modified files:**

| Path | Change |
|------|--------|
| `pom.xml` | Add `spring-boot-starter-websocket` |
| `src/main/java/com/sait/peelin/model/ChatThread.java` | Add `category` + `closedAt` fields |
| `src/main/java/com/sait/peelin/repository/ChatThreadRepository.java` | Add `findByStatusAndCategory...` method |
| `src/main/java/com/sait/peelin/dto/v1/ChatThreadDto.java` | Add `category` + `closedAt` to record |
| `src/main/java/com/sait/peelin/service/ChatService.java` | `createThread(category)`, `closeThread`, `openThreads(category)`, WS push in `postMessage`/`markRead` |
| `src/main/java/com/sait/peelin/controller/v1/ChatRestController.java` | Accept category on create, category filter, close endpoint |
| `src/main/java/com/sait/peelin/security/SecurityConfig.java` | Add `/ws/**` to `permitAll` |

---

## Task 1: WebSocket dependency + DB migration

**Files:**
- Modify: `apps/backend/pom.xml`
- Create: `apps/backend/src/main/resources/db/migration/V42__websocket_chat.sql`

- [ ] **Step 1: Add dependency to pom.xml**

In `pom.xml`, add before the closing `</dependencies>` tag:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

- [ ] **Step 2: Write the migration**

Create `src/main/resources/db/migration/V42__websocket_chat.sql`:

```sql
ALTER TABLE chat_thread ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'general';
ALTER TABLE chat_thread ADD COLUMN closed_at TIMESTAMPTZ;

CREATE TABLE staff_conversation (
    conversation_id  SERIAL PRIMARY KEY,
    user_a_id        UUID NOT NULL REFERENCES app_user(user_id),
    user_b_id        UUID NOT NULL REFERENCES app_user(user_id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id)
);

CREATE TABLE staff_message (
    message_id       SERIAL PRIMARY KEY,
    conversation_id  INTEGER NOT NULL REFERENCES staff_conversation(conversation_id),
    sender_id        UUID NOT NULL REFERENCES app_user(user_id),
    message_text     VARCHAR(2000),
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read          BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_staff_message_convo ON staff_message(conversation_id, sent_at);
```

- [ ] **Step 3: Verify compilation**

Run from `apps/backend/`:
```
mvnw.cmd clean compile -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```
git add apps/backend/pom.xml apps/backend/src/main/resources/db/migration/V42__websocket_chat.sql
git commit -m "feat: add websocket dependency and chat DB migration (V42)"
```

---

## Task 2: Update ChatThread entity + repository

**Files:**
- Modify: `apps/backend/src/main/java/com/sait/peelin/model/ChatThread.java`
- Modify: `apps/backend/src/main/java/com/sait/peelin/repository/ChatThreadRepository.java`

- [ ] **Step 1: Add category and closedAt fields to ChatThread**

In `ChatThread.java`, add these two fields after the `updatedAt` field (line ~35):

```java
@NotNull
@Column(name = "category", nullable = false, length = 30)
@ColumnDefault("'general'")
private String category = "general";

@Column(name = "closed_at")
private OffsetDateTime closedAt;
```

Add getters and setters at the end of the class before the closing `}`:

```java
public String getCategory() {
    return category;
}

public void setCategory(String category) {
    this.category = category;
}

public OffsetDateTime getClosedAt() {
    return closedAt;
}

public void setClosedAt(OffsetDateTime closedAt) {
    this.closedAt = closedAt;
}
```

- [ ] **Step 2: Add findByStatusAndCategory method to ChatThreadRepository**

In `ChatThreadRepository.java`, add after the `findByStatusOrderByUpdatedAtDesc` method:

```java
@EntityGraph(attributePaths = {"customerUser", "employeeUser"})
List<ChatThread> findByStatusAndCategoryOrderByUpdatedAtDesc(String status, String category);
```

- [ ] **Step 3: Verify compilation**

```
mvnw.cmd clean compile -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/model/ChatThread.java apps/backend/src/main/java/com/sait/peelin/repository/ChatThreadRepository.java
git commit -m "feat: add category and closedAt fields to ChatThread"
```

---

## Task 3: Staff entities + repositories

**Files:**
- Create: `apps/backend/src/main/java/com/sait/peelin/model/StaffConversation.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/model/StaffMessage.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/repository/StaffConversationRepository.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/repository/StaffMessageRepository.java`

- [ ] **Step 1: Create StaffConversation entity**

Create `src/main/java/com/sait/peelin/model/StaffConversation.java`:

```java
package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "staff_conversation")
public class StaffConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_a_id", referencedColumnName = "user_id", nullable = false)
    private User userA;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_b_id", referencedColumnName = "user_id", nullable = false)
    private User userB;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUserA() { return userA; }
    public void setUserA(User userA) { this.userA = userA; }
    public User getUserB() { return userB; }
    public void setUserB(User userB) { this.userB = userB; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Create StaffMessage entity**

Create `src/main/java/com/sait/peelin/model/StaffMessage.java`:

```java
package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "staff_message")
public class StaffMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private StaffConversation conversation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", referencedColumnName = "user_id", nullable = false)
    private User sender;

    @Column(name = "message_text", length = 2000)
    private String messageText;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public StaffConversation getConversation() { return conversation; }
    public void setConversation(StaffConversation conversation) { this.conversation = conversation; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
}
```

- [ ] **Step 3: Create StaffConversationRepository**

Create `src/main/java/com/sait/peelin/repository/StaffConversationRepository.java`:

```java
package com.sait.peelin.repository;

import com.sait.peelin.model.StaffConversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffConversationRepository extends JpaRepository<StaffConversation, Integer> {

    @EntityGraph(attributePaths = {"userA", "userB"})
    Optional<StaffConversation> findByUserA_UserIdAndUserB_UserId(UUID userAId, UUID userBId);

    @EntityGraph(attributePaths = {"userA", "userB"})
    List<StaffConversation> findByUserA_UserIdOrUserB_UserIdOrderByUpdatedAtDesc(
            UUID userAId, UUID userBId);
}
```

- [ ] **Step 4: Create StaffMessageRepository**

Create `src/main/java/com/sait/peelin/repository/StaffMessageRepository.java`:

```java
package com.sait.peelin.repository;

import com.sait.peelin.model.StaffMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StaffMessageRepository extends JpaRepository<StaffMessage, Integer> {

    List<StaffMessage> findByConversation_IdOrderBySentAtAsc(Integer conversationId);

    List<StaffMessage> findByConversation_IdAndIsReadFalseAndSender_UserIdNot(
            Integer conversationId, UUID senderId);
}
```

- [ ] **Step 5: Verify compilation**

```
mvnw.cmd clean compile -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/model/StaffConversation.java apps/backend/src/main/java/com/sait/peelin/model/StaffMessage.java apps/backend/src/main/java/com/sait/peelin/repository/StaffConversationRepository.java apps/backend/src/main/java/com/sait/peelin/repository/StaffMessageRepository.java
git commit -m "feat: add StaffConversation and StaffMessage entities and repositories"
```

---

## Task 4: Update and create DTOs

**Files:**
- Modify: `apps/backend/src/main/java/com/sait/peelin/dto/v1/ChatThreadDto.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/dto/v1/TypingPayload.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/dto/v1/ReadReceiptPayload.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/dto/v1/StaffConversationDto.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/dto/v1/StaffMessageDto.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/dto/v1/SendStaffMessageRequest.java`
- Create: `apps/backend/src/main/java/com/sait/peelin/dto/v1/StartConversationRequest.java`

- [ ] **Step 1: Replace ChatThreadDto to add category and closedAt**

Replace the full contents of `src/main/java/com/sait/peelin/dto/v1/ChatThreadDto.java`:

```java
package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatThreadDto(
        Integer id,
        UUID customerUserId,
        String customerDisplayName,
        String customerUsername,
        String customerEmail,
        UUID employeeUserId,
        String status,
        String category,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt
) {}
```

- [ ] **Step 2: Create TypingPayload**

Create `src/main/java/com/sait/peelin/dto/v1/TypingPayload.java`:

```java
package com.sait.peelin.dto.v1;

import java.util.UUID;

public record TypingPayload(UUID userId, boolean typing) {}
```

- [ ] **Step 3: Create ReadReceiptPayload**

Create `src/main/java/com/sait/peelin/dto/v1/ReadReceiptPayload.java`:

```java
package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadReceiptPayload(UUID userId, OffsetDateTime readAt) {}
```

- [ ] **Step 4: Create StaffConversationDto**

Create `src/main/java/com/sait/peelin/dto/v1/StaffConversationDto.java`:

```java
package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffConversationDto(
        Integer id,
        UUID otherUserId,
        String otherUsername,
        OffsetDateTime updatedAt,
        int unreadCount
) {}
```

- [ ] **Step 5: Create StaffMessageDto**

Create `src/main/java/com/sait/peelin/dto/v1/StaffMessageDto.java`:

```java
package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffMessageDto(
        Integer id,
        Integer conversationId,
        UUID senderUserId,
        String text,
        OffsetDateTime sentAt,
        boolean read
) {}
```

- [ ] **Step 6: Create SendStaffMessageRequest**

Create `src/main/java/com/sait/peelin/dto/v1/SendStaffMessageRequest.java`:

```java
package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendStaffMessageRequest(
        @NotBlank @Size(max = 2000) String text
) {}
```

- [ ] **Step 7: Create StartConversationRequest**

Create `src/main/java/com/sait/peelin/dto/v1/StartConversationRequest.java`:

```java
package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartConversationRequest(@NotNull UUID recipientId) {}
```

- [ ] **Step 8: Compile — expect failure at ChatService (will fix in Task 5)**

```
mvnw.cmd clean compile -DskipTests
```
Expected: FAIL — `ChatService.threadDto()` still calls the old 9-arg `ChatThreadDto` constructor. This is expected. We fix it in Task 5.

- [ ] **Step 9: Commit the DTO files**

```
git add apps/backend/src/main/java/com/sait/peelin/dto/
git commit -m "feat: update ChatThreadDto with category/closedAt, add staff message DTOs"
```

---

## Task 5: STOMP WebSocket config

**Files:**
- Create: `apps/backend/src/main/java/com/sait/peelin/config/StompWebSocketConfig.java`

Note: This references `StompChannelInterceptor` (created in Task 6). Both tasks must complete before compilation passes.

- [ ] **Step 1: Create StompWebSocketConfig**

Create `src/main/java/com/sait/peelin/config/StompWebSocketConfig.java`:

```java
package com.sait.peelin.config;

import com.sait.peelin.security.StompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
    }
}
```

---

## Task 6: StompChannelInterceptor + SecurityConfig

**Files:**
- Create: `apps/backend/src/main/java/com/sait/peelin/security/StompChannelInterceptor.java`
- Modify: `apps/backend/src/main/java/com/sait/peelin/security/SecurityConfig.java`
- Create: `apps/backend/src/test/java/com/sait/peelin/security/StompChannelInterceptorTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/sait/peelin/security/StompChannelInterceptorTest.java`:

```java
package com.sait.peelin.security;

import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.TokenDenylistService;
import com.sait.peelin.service.UserLookupCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompChannelInterceptorTest {

    @Mock JwtService jwtService;
    @Mock TokenDenylistService tokenDenylistService;
    @Mock UserLookupCacheService userLookupCacheService;
    @Mock MessageChannel channel;

    @InjectMocks StompChannelInterceptor interceptor;

    private User staffUser;

    @BeforeEach
    void setUp() {
        staffUser = new User();
        staffUser.setUserId(UUID.randomUUID());
        staffUser.setUsername("staffmember");
        staffUser.setUserRole(UserRole.employee);
    }

    @Test
    void connect_WithValidJwtInHeader_SetsUserPrincipal() {
        String jwt = "valid.jwt.token";
        when(tokenDenylistService.isDenied(jwt)).thenReturn(false);
        when(jwtService.extractUsername(jwt)).thenReturn("staffmember");
        when(userLookupCacheService.findActiveByLoginIdentifier("staffmember")).thenReturn(staffUser);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + jwt);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("staffmember");
    }

    @Test
    void connect_WithMissingToken_ThrowsMessageDeliveryException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void connect_WithDeniedToken_ThrowsMessageDeliveryException() {
        String jwt = "denied.token";
        when(tokenDenylistService.isDenied(jwt)).thenReturn(true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + jwt);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void nonConnectFrame_PassesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat/thread/1/typing");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
mvnw.cmd test -Dtest=StompChannelInterceptorTest -DfailIfNoTests=false
```
Expected: FAIL — `StompChannelInterceptor` does not exist.

- [ ] **Step 3: Create StompChannelInterceptor**

Create `src/main/java/com/sait/peelin/security/StompChannelInterceptor.java`:

```java
package com.sait.peelin.security;

import com.sait.peelin.model.User;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.TokenDenylistService;
import com.sait.peelin.service.UserLookupCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final UserLookupCacheService userLookupCacheService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = resolveToken(accessor);
            if (jwt == null || tokenDenylistService.isDenied(jwt)) {
                throw new MessageDeliveryException("Unauthorized");
            }
            try {
                String username = jwtService.extractUsername(jwt);
                User user = userLookupCacheService.findActiveByLoginIdentifier(username);
                if (user == null) {
                    throw new MessageDeliveryException("Unauthorized");
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getUserRole().name().toUpperCase()))
                );
                auth.setDetails(user);
                accessor.setUser(auth);
            } catch (MessageDeliveryException e) {
                throw e;
            } catch (Exception e) {
                throw new MessageDeliveryException("Unauthorized");
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        String cookieHeader = accessor.getFirstNativeHeader("cookie");
        if (cookieHeader != null) {
            for (String part : cookieHeader.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("token=")) {
                    return trimmed.substring(6);
                }
            }
        }
        return null;
    }
}
```

- [ ] **Step 4: Add /ws/** to SecurityConfig permitAll**

In `SecurityConfig.java`, inside `.authorizeHttpRequests(auth -> auth ...`, add this line directly before `.anyRequest().authenticated()`:

```java
.requestMatchers("/ws/**").permitAll()
```

- [ ] **Step 5: Run tests**

```
mvnw.cmd test -Dtest=StompChannelInterceptorTest -DfailIfNoTests=false
```
Expected: Tests: 4 passed

- [ ] **Step 6: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/config/StompWebSocketConfig.java apps/backend/src/main/java/com/sait/peelin/security/StompChannelInterceptor.java apps/backend/src/main/java/com/sait/peelin/security/SecurityConfig.java apps/backend/src/test/java/com/sait/peelin/security/StompChannelInterceptorTest.java
git commit -m "feat: add STOMP WebSocket config and JWT channel interceptor"
```

---

## Task 7: Extend ChatService

**Files:**
- Modify: `apps/backend/src/main/java/com/sait/peelin/service/ChatService.java`
- Create: `apps/backend/src/test/java/com/sait/peelin/service/ChatServiceTest.java`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/com/sait/peelin/service/ChatServiceTest.java`:

```java
package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.ChatThread;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.ChatMessageRepository;
import com.sait.peelin.repository.ChatThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatThreadRepository chatThreadRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatLookupCacheService chatLookupCacheService;
    @Mock CustomerLookupCacheService customerLookupCacheService;
    @Mock CurrentUserService currentUserService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks ChatService chatService;

    private User staffUser;
    private User customerUser;
    private ChatThread openThread;

    @BeforeEach
    void setUp() {
        staffUser = new User();
        staffUser.setUserId(UUID.randomUUID());
        staffUser.setUsername("staff1");
        staffUser.setUserRole(UserRole.employee);

        customerUser = new User();
        customerUser.setUserId(UUID.randomUUID());
        customerUser.setUsername("cust1");
        customerUser.setUserRole(UserRole.customer);

        openThread = new ChatThread();
        openThread.setId(42);
        openThread.setCustomerUser(customerUser);
        openThread.setStatus("open");
        openThread.setCategory("order_issue");
        openThread.setCreatedAt(OffsetDateTime.now());
        openThread.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void createThread_WithCategory_PersistsCategory() {
        when(currentUserService.requireUser()).thenReturn(customerUser);
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(inv -> {
            ChatThread t = inv.getArgument(0);
            t.setId(1);
            return t;
        });
        when(currentUserService.currentUserOrNull()).thenReturn(customerUser);
        when(customerLookupCacheService.findByUserId(any())).thenReturn(null);

        ChatThreadDto result = chatService.createThread("order_issue");

        assertThat(result.category()).isEqualTo("order_issue");
        verify(chatLookupCacheService).evictOpenThreadForCustomer(customerUser.getUserId());
    }

    @Test
    void closeThread_ByStaff_SetsStatusAndClosedAt() {
        when(currentUserService.requireUser()).thenReturn(staffUser);
        when(chatThreadRepository.findById(42)).thenReturn(Optional.of(openThread));
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.currentUserOrNull()).thenReturn(staffUser);
        when(customerLookupCacheService.findByUserId(any())).thenReturn(null);

        ChatThreadDto result = chatService.closeThread(42);

        assertThat(result.status()).isEqualTo("closed");
        assertThat(result.closedAt()).isNotNull();
        verify(messagingTemplate).convertAndSendToUser(
                eq(customerUser.getUserId().toString()),
                eq("/queue/chat/notifications"),
                any()
        );
    }

    @Test
    void closeThread_ByCustomer_ThrowsForbidden() {
        when(currentUserService.requireUser()).thenReturn(customerUser);

        assertThatThrownBy(() -> chatService.closeThread(42))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void closeThread_ThreadNotFound_ThrowsNotFound() {
        when(currentUserService.requireUser()).thenReturn(staffUser);
        when(chatThreadRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.closeThread(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
mvnw.cmd test -Dtest=ChatServiceTest -DfailIfNoTests=false
```
Expected: FAIL — missing `createThread(String)`, `closeThread`, `messagingTemplate` field.

- [ ] **Step 3: Update ChatService — add SimpMessagingTemplate field**

In `ChatService.java`, add this field to the class (after `currentUserService`):

```java
private final SimpMessagingTemplate messagingTemplate;
```

- [ ] **Step 4: Update ChatService — replace createThread methods**

Remove the existing `public ChatThreadDto createThread()` public method and the `private ChatThread createThread(User customer)` private method. Replace both with these three:

```java
@Transactional
@CacheEvict(value = "chat-open-threads", allEntries = true)
public ChatThreadDto createThread() {
    return createThread("general");
}

@Transactional
@CacheEvict(value = "chat-open-threads", allEntries = true)
public ChatThreadDto createThread(String category) {
    User u = currentUserService.requireUser();
    if (u.getUserRole() != UserRole.customer) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    ChatThread created = createThreadEntity(u, category);
    chatLookupCacheService.evictOpenThreadForCustomer(u.getUserId());
    return threadDto(created);
}

private ChatThread createThreadEntity(User customer, String category) {
    ChatThread t = new ChatThread();
    t.setCustomerUser(customer);
    t.setStatus("open");
    t.setCategory(category != null ? category : "general");
    t.setCreatedAt(OffsetDateTime.now());
    t.setUpdatedAt(OffsetDateTime.now());
    return chatThreadRepository.save(t);
}
```

- [ ] **Step 5: Update ChatService — fix getOrCreateOpenThreadForCustomer**

In `getOrCreateOpenThreadForCustomer()`, inside the `.orElseGet(() -> { ... })` lambda, change:

```java
ChatThread created = createThread(u);
```
to:
```java
ChatThread created = createThreadEntity(u, "general");
```

- [ ] **Step 6: Update ChatService — replace openThreads**

Remove the existing `openThreads()` method (the one with `@Cacheable`). Replace with:

```java
@Transactional(readOnly = true)
public List<ChatThreadDto> openThreads(String category) {
    User u = currentUserService.requireUser();
    if (u.getUserRole() == UserRole.admin || u.getUserRole() == UserRole.employee) {
        if (category != null && !category.isBlank()) {
            return chatThreadRepository
                    .findByStatusAndCategoryOrderByUpdatedAtDesc("open", category)
                    .stream().map(this::threadDto).toList();
        }
        return chatThreadRepository.findByStatusOrderByUpdatedAtDesc("open")
                .stream().map(this::threadDto).toList();
    }
    if (u.getUserRole() == UserRole.customer) {
        return java.util.Optional.ofNullable(
                chatLookupCacheService.findOpenThreadIdForCustomer(u.getUserId()))
                .flatMap(chatThreadRepository::findById)
                .map(this::threadDto)
                .stream().toList();
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
}

public List<ChatThreadDto> openThreads() {
    return openThreads(null);
}
```

- [ ] **Step 7: Update ChatService — add closeThread method**

Add this method after `assignEmployee`:

```java
@Transactional
@CacheEvict(value = "chat-open-threads", allEntries = true)
public ChatThreadDto closeThread(Integer threadId) {
    User staff = currentUserService.requireUser();
    if (staff.getUserRole() != UserRole.employee && staff.getUserRole() != UserRole.admin) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    ChatThread t = chatThreadRepository.findById(threadId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
    t.setStatus("closed");
    t.setClosedAt(OffsetDateTime.now());
    t.setUpdatedAt(OffsetDateTime.now());
    if (t.getCustomerUser() != null) {
        chatLookupCacheService.evictOpenThreadForCustomer(t.getCustomerUser().getUserId());
    }
    ChatThreadDto dto = threadDto(chatThreadRepository.save(t));
    messagingTemplate.convertAndSendToUser(
            t.getCustomerUser().getUserId().toString(),
            "/queue/chat/notifications",
            dto);
    return dto;
}
```

- [ ] **Step 8: Update ChatService — add WS push in postMessage**

In `postMessage()`, replace the final `return` statement:

```java
return msgDto(chatMessageRepository.save(m));
```
with:
```java
ChatMessageDto dto = msgDto(chatMessageRepository.save(m));
messagingTemplate.convertAndSend("/topic/chat/thread/" + threadId + "/messages", dto);
return dto;
```

- [ ] **Step 9: Update ChatService — add WS push in markRead**

In `markRead()`, after the for loop that saves messages, add:

```java
messagingTemplate.convertAndSend(
        "/topic/chat/thread/" + threadId + "/read",
        new ReadReceiptPayload(viewer.getUserId(), OffsetDateTime.now()));
```

- [ ] **Step 10: Update ChatService — fix threadDto constructor call**

In the `threadDto()` private method, replace `return new ChatThreadDto(...)` with:

```java
return new ChatThreadDto(
        t.getId(),
        customerUser.getUserId(),
        resolveCustomerDisplayName(customer, customerUser),
        customerUser.getUsername(),
        resolveCustomerEmail(customer, customerUser),
        t.getEmployeeUser() != null ? t.getEmployeeUser().getUserId() : null,
        t.getStatus(),
        t.getCategory(),
        t.getCreatedAt(),
        t.getUpdatedAt(),
        t.getClosedAt()
);
```

- [ ] **Step 11: Add missing imports to ChatService**

Add at the top of `ChatService.java`:

```java
import com.sait.peelin.dto.v1.ReadReceiptPayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
```

- [ ] **Step 12: Run tests**

```
mvnw.cmd test -Dtest=ChatServiceTest -DfailIfNoTests=false
```
Expected: Tests: 4 passed

- [ ] **Step 13: Run all tests**

```
mvnw.cmd test
```
Expected: BUILD SUCCESS

- [ ] **Step 14: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/service/ChatService.java apps/backend/src/test/java/com/sait/peelin/service/ChatServiceTest.java
git commit -m "feat: extend ChatService with category, closeThread, and WebSocket push"
```

---

## Task 8: Update ChatRestController

**Files:**
- Modify: `apps/backend/src/main/java/com/sait/peelin/controller/v1/ChatRestController.java`
- Create: `apps/backend/src/test/java/com/sait/peelin/controller/v1/ChatRestControllerTest.java`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/com/sait/peelin/controller/v1/ChatRestControllerTest.java`:

```java
package com.sait.peelin.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ChatRestController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = "com\\.sait\\.peelin\\.security\\..*")
)
@AutoConfigureMockMvc(addFilters = false)
class ChatRestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ChatService chatService;

    ChatThreadDto thread(String status, String category) {
        return new ChatThreadDto(1, UUID.randomUUID(), "Alice", "alice",
                "alice@test.com", null, status, category,
                OffsetDateTime.now(), OffsetDateTime.now(), null);
    }

    @Test
    void createThread_WithCategory_CallsServiceWithCategory() throws Exception {
        when(chatService.createThread("order_issue")).thenReturn(thread("open", "order_issue"));

        mockMvc.perform(post("/api/v1/chat/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"order_issue\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("order_issue"));

        verify(chatService).createThread("order_issue");
    }

    @Test
    void createThread_NoBody_CallsServiceWithGeneral() throws Exception {
        when(chatService.createThread("general")).thenReturn(thread("open", "general"));

        mockMvc.perform(post("/api/v1/chat/threads"))
                .andExpect(status().isCreated());

        verify(chatService).createThread("general");
    }

    @Test
    void openThreads_WithCategoryParam_PassesCategoryToService() throws Exception {
        when(chatService.openThreads("order_issue")).thenReturn(List.of(thread("open", "order_issue")));

        mockMvc.perform(get("/api/v1/chat/threads?category=order_issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("order_issue"));

        verify(chatService).openThreads("order_issue");
    }

    @Test
    void closeThread_Returns200WithClosedStatus() throws Exception {
        when(chatService.closeThread(5)).thenReturn(
                new ChatThreadDto(5, UUID.randomUUID(), "Bob", "bob",
                        "bob@test.com", UUID.randomUUID(), "closed", "general",
                        OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/chat/threads/5/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"));

        verify(chatService).closeThread(5);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
mvnw.cmd test -Dtest=ChatRestControllerTest -DfailIfNoTests=false
```
Expected: FAIL — missing `createThread(String)`, no `/close` endpoint.

- [ ] **Step 3: Replace ChatRestController**

Replace the full contents of `src/main/java/com/sait/peelin/controller/v1/ChatRestController.java`:

```java
package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ChatMessageDto;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.dto.v1.PostChatMessageRequest;
import com.sait.peelin.service.ChatService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Support chat between customers and staff.")
@SecurityRequirement(name = "bearer-jwt")
public class ChatRestController {

    private final ChatService chatService;

    record CreateThreadRequest(String category) {}

    @GetMapping("/threads")
    public List<ChatThreadDto> openThreads(@RequestParam(required = false) String category) {
        return chatService.openThreads(category);
    }

    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatThreadDto createThread(@RequestBody(required = false) CreateThreadRequest req) {
        String category = (req != null && req.category() != null) ? req.category() : "general";
        return chatService.createThread(category);
    }

    @GetMapping("/threads/me/open")
    public ChatThreadDto myOpenThread() {
        return chatService.getOrCreateOpenThreadForCustomer();
    }

    @GetMapping("/threads/{threadId}/messages")
    public List<ChatMessageDto> messages(@PathVariable Integer threadId) {
        return chatService.messages(threadId);
    }

    @PostMapping("/threads/{threadId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto post(@PathVariable Integer threadId,
                               @Valid @RequestBody PostChatMessageRequest req) {
        return chatService.postMessage(threadId, req);
    }

    @PostMapping("/threads/{threadId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ChatThreadDto assign(@PathVariable Integer threadId) {
        return chatService.assignEmployee(threadId);
    }

    @PostMapping("/threads/{threadId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Integer threadId) {
        chatService.markRead(threadId);
    }

    @PostMapping("/threads/{threadId}/close")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ChatThreadDto close(@PathVariable Integer threadId) {
        return chatService.closeThread(threadId);
    }
}
```

- [ ] **Step 4: Run tests**

```
mvnw.cmd test -Dtest=ChatRestControllerTest -DfailIfNoTests=false
```
Expected: Tests: 4 passed

- [ ] **Step 5: Run all tests**

```
mvnw.cmd test
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/controller/v1/ChatRestController.java apps/backend/src/test/java/com/sait/peelin/controller/v1/ChatRestControllerTest.java
git commit -m "feat: update ChatRestController with category filter and close endpoint"
```

---

## Task 9: ChatWebSocketController

**Files:**
- Create: `apps/backend/src/main/java/com/sait/peelin/controller/ws/ChatWebSocketController.java`

- [ ] **Step 1: Create the controller**

Create `src/main/java/com/sait/peelin/controller/ws/ChatWebSocketController.java`:

```java
package com.sait.peelin.controller.ws;

import com.sait.peelin.dto.v1.TypingPayload;
import com.sait.peelin.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat/thread/{threadId}/typing")
    public void typing(@DestinationVariable Integer threadId, TypingPayload payload) {
        messagingTemplate.convertAndSend(
                "/topic/chat/thread/" + threadId + "/typing", payload);
    }

    @MessageMapping("/chat/thread/{threadId}/read")
    public void markRead(@DestinationVariable Integer threadId) {
        chatService.markRead(threadId);
    }
}
```

- [ ] **Step 2: Compile to verify**

```
mvnw.cmd clean compile -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/controller/ws/ChatWebSocketController.java
git commit -m "feat: add ChatWebSocketController for typing and read relay"
```

---

## Task 10: StaffMessageService

**Files:**
- Create: `apps/backend/src/main/java/com/sait/peelin/service/StaffMessageService.java`
- Create: `apps/backend/src/test/java/com/sait/peelin/service/StaffMessageServiceTest.java`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/com/sait/peelin/service/StaffMessageServiceTest.java`:

```java
package com.sait.peelin.service;

import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.model.StaffConversation;
import com.sait.peelin.model.StaffMessage;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.StaffConversationRepository;
import com.sait.peelin.repository.StaffMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffMessageServiceTest {

    @Mock StaffConversationRepository convRepo;
    @Mock StaffMessageRepository msgRepo;
    @Mock CurrentUserService currentUserService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks StaffMessageService staffMessageService;

    private User userA;
    private User userB;
    private StaffConversation conversation;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        userA.setUsername("alice");
        userA.setUserRole(UserRole.employee);

        userB = new User();
        userB.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        userB.setUsername("bob");
        userB.setUserRole(UserRole.employee);

        conversation = new StaffConversation();
        conversation.setId(10);
        conversation.setUserA(userA);
        conversation.setUserB(userB);
        conversation.setCreatedAt(OffsetDateTime.now());
        conversation.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void getOrCreateConversation_ExistingConversation_ReturnsSame() {
        when(currentUserService.requireUser()).thenReturn(userA);
        when(convRepo.findByUserA_UserIdAndUserB_UserId(userA.getUserId(), userB.getUserId()))
                .thenReturn(Optional.of(conversation));
        when(msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(10, userA.getUserId()))
                .thenReturn(Collections.emptyList());

        StaffConversationDto result = staffMessageService.getOrCreateConversation(userB.getUserId());

        assertThat(result.id()).isEqualTo(10);
        verify(convRepo, never()).save(any());
    }

    @Test
    void getOrCreateConversation_NewConversation_SavesThenReFetches() {
        when(currentUserService.requireUser()).thenReturn(userA);
        when(convRepo.findByUserA_UserIdAndUserB_UserId(userA.getUserId(), userB.getUserId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(conversation));  // second call after save
        when(msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(10, userA.getUserId()))
                .thenReturn(Collections.emptyList());

        StaffConversationDto result = staffMessageService.getOrCreateConversation(userB.getUserId());

        assertThat(result.id()).isEqualTo(10);
        assertThat(result.otherUsername()).isEqualTo("bob");
        verify(convRepo).save(any(StaffConversation.class));
    }

    @Test
    void sendMessage_PersistsAndPushesViaWs() {
        StaffMessage saved = new StaffMessage();
        saved.setId(99);
        saved.setConversation(conversation);
        saved.setSender(userA);
        saved.setMessageText("hello");
        saved.setSentAt(OffsetDateTime.now());
        saved.setIsRead(false);

        when(currentUserService.requireUser()).thenReturn(userA);
        when(convRepo.findById(10)).thenReturn(Optional.of(conversation));
        when(msgRepo.save(any(StaffMessage.class))).thenReturn(saved);

        StaffMessageDto result = staffMessageService.sendMessage(10, "hello");

        assertThat(result.text()).isEqualTo("hello");
        verify(messagingTemplate).convertAndSend(
                eq("/topic/messages/conversation/10/messages"),
                any(StaffMessageDto.class));
    }

    @Test
    void sendMessage_NotParticipant_ThrowsForbidden() {
        User outsider = new User();
        outsider.setUserId(UUID.randomUUID());
        outsider.setUserRole(UserRole.employee);

        when(currentUserService.requireUser()).thenReturn(outsider);
        when(convRepo.findById(10)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> staffMessageService.sendMessage(10, "hi"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void conversations_ReturnsList() {
        when(currentUserService.requireUser()).thenReturn(userA);
        when(convRepo.findByUserA_UserIdOrUserB_UserIdOrderByUpdatedAtDesc(
                userA.getUserId(), userA.getUserId()))
                .thenReturn(List.of(conversation));
        when(msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(10, userA.getUserId()))
                .thenReturn(Collections.emptyList());

        List<StaffConversationDto> result = staffMessageService.conversations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
mvnw.cmd test -Dtest=StaffMessageServiceTest -DfailIfNoTests=false
```
Expected: FAIL — `StaffMessageService` does not exist.

- [ ] **Step 3: Create StaffMessageService**

Create `src/main/java/com/sait/peelin/service/StaffMessageService.java`:

```java
package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ReadReceiptPayload;
import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.StaffConversation;
import com.sait.peelin.model.StaffMessage;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.StaffConversationRepository;
import com.sait.peelin.repository.StaffMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffMessageService {

    private final StaffConversationRepository convRepo;
    private final StaffMessageRepository msgRepo;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public StaffConversationDto getOrCreateConversation(UUID recipientId) {
        User me = currentUserService.requireUser();
        requireStaff(me);

        UUID a = me.getUserId().compareTo(recipientId) < 0 ? me.getUserId() : recipientId;
        UUID b = me.getUserId().compareTo(recipientId) < 0 ? recipientId : me.getUserId();

        convRepo.findByUserA_UserIdAndUserB_UserId(a, b).ifPresentOrElse(
                c -> {},
                () -> {
                    User ua = new User(); ua.setUserId(a);
                    User ub = new User(); ub.setUserId(b);
                    StaffConversation c = new StaffConversation();
                    c.setUserA(ua);
                    c.setUserB(ub);
                    c.setCreatedAt(OffsetDateTime.now());
                    c.setUpdatedAt(OffsetDateTime.now());
                    convRepo.save(c);
                }
        );

        StaffConversation conv = convRepo.findByUserA_UserIdAndUserB_UserId(a, b)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return conversationDto(conv, me.getUserId());
    }

    @Transactional(readOnly = true)
    public List<StaffConversationDto> conversations() {
        User me = currentUserService.requireUser();
        requireStaff(me);
        return convRepo.findByUserA_UserIdOrUserB_UserIdOrderByUpdatedAtDesc(
                me.getUserId(), me.getUserId())
                .stream().map(c -> conversationDto(c, me.getUserId())).toList();
    }

    @Transactional(readOnly = true)
    public List<StaffMessageDto> messages(Integer conversationId) {
        User me = currentUserService.requireUser();
        requireStaff(me);
        StaffConversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, me);
        return msgRepo.findByConversation_IdOrderBySentAtAsc(conversationId)
                .stream().map(this::msgDto).toList();
    }

    @Transactional
    public StaffMessageDto sendMessage(Integer conversationId, String text) {
        User me = currentUserService.requireUser();
        requireStaff(me);
        StaffConversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, me);

        StaffMessage m = new StaffMessage();
        m.setConversation(conv);
        m.setSender(me);
        m.setMessageText(text);
        m.setSentAt(OffsetDateTime.now());
        m.setIsRead(false);

        conv.setUpdatedAt(OffsetDateTime.now());
        convRepo.save(conv);

        StaffMessageDto dto = msgDto(msgRepo.save(m));
        messagingTemplate.convertAndSend(
                "/topic/messages/conversation/" + conversationId + "/messages", dto);
        return dto;
    }

    @Transactional
    public void markRead(Integer conversationId) {
        User me = currentUserService.requireUser();
        requireStaff(me);
        StaffConversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, me);

        List<StaffMessage> unread = msgRepo
                .findByConversation_IdAndIsReadFalseAndSender_UserIdNot(
                        conversationId, me.getUserId());
        for (StaffMessage msg : unread) {
            msg.setIsRead(true);
            msgRepo.save(msg);
        }
        messagingTemplate.convertAndSend(
                "/topic/messages/conversation/" + conversationId + "/read",
                new ReadReceiptPayload(me.getUserId(), OffsetDateTime.now()));
    }

    private StaffConversationDto conversationDto(StaffConversation conv, UUID myUserId) {
        User other = conv.getUserA().getUserId().equals(myUserId)
                ? conv.getUserB() : conv.getUserA();
        int unread = msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(
                conv.getId(), myUserId).size();
        return new StaffConversationDto(
                conv.getId(), other.getUserId(), other.getUsername(),
                conv.getUpdatedAt(), unread);
    }

    private StaffMessageDto msgDto(StaffMessage m) {
        return new StaffMessageDto(
                m.getId(), m.getConversation().getId(),
                m.getSender().getUserId(), m.getMessageText(),
                m.getSentAt(), m.getIsRead());
    }

    private void requireStaff(User u) {
        if (u.getUserRole() != UserRole.employee && u.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireParticipant(StaffConversation conv, User u) {
        boolean participant = conv.getUserA().getUserId().equals(u.getUserId())
                || conv.getUserB().getUserId().equals(u.getUserId());
        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
```

- [ ] **Step 4: Run tests**

```
mvnw.cmd test -Dtest=StaffMessageServiceTest -DfailIfNoTests=false
```
Expected: Tests: 5 passed

- [ ] **Step 5: Run all tests**

```
mvnw.cmd test
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/service/StaffMessageService.java apps/backend/src/test/java/com/sait/peelin/service/StaffMessageServiceTest.java
git commit -m "feat: add StaffMessageService for staff-to-staff direct messaging"
```

---

## Task 11: StaffMessageController

**Files:**
- Create: `apps/backend/src/main/java/com/sait/peelin/controller/v1/StaffMessageController.java`
- Create: `apps/backend/src/test/java/com/sait/peelin/controller/v1/StaffMessageControllerTest.java`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/com/sait/peelin/controller/v1/StaffMessageControllerTest.java`:

```java
package com.sait.peelin.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.service.StaffMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = StaffMessageController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = "com\\.sait\\.peelin\\.security\\..*")
)
@AutoConfigureMockMvc(addFilters = false)
class StaffMessageControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean StaffMessageService staffMessageService;

    UUID otherUser = UUID.randomUUID();

    StaffConversationDto sampleConvo() {
        return new StaffConversationDto(1, otherUser, "bob", OffsetDateTime.now(), 0);
    }

    StaffMessageDto sampleMsg() {
        return new StaffMessageDto(1, 1, UUID.randomUUID(), "hello", OffsetDateTime.now(), false);
    }

    @Test
    void listConversations_Returns200() throws Exception {
        when(staffMessageService.conversations()).thenReturn(List.of(sampleConvo()));

        mockMvc.perform(get("/api/v1/messages/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void startConversation_Returns201() throws Exception {
        when(staffMessageService.getOrCreateConversation(otherUser)).thenReturn(sampleConvo());

        mockMvc.perform(post("/api/v1/messages/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientId\":\"" + otherUser + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otherUsername").value("bob"));
    }

    @Test
    void getMessages_Returns200() throws Exception {
        when(staffMessageService.messages(1)).thenReturn(List.of(sampleMsg()));

        mockMvc.perform(get("/api/v1/messages/conversations/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("hello"));
    }

    @Test
    void sendMessage_Returns201() throws Exception {
        when(staffMessageService.sendMessage(eq(1), eq("hello"))).thenReturn(sampleMsg());

        mockMvc.perform(post("/api/v1/messages/conversations/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("hello"));
    }

    @Test
    void markRead_Returns204() throws Exception {
        doNothing().when(staffMessageService).markRead(1);

        mockMvc.perform(post("/api/v1/messages/conversations/1/read"))
                .andExpect(status().isNoContent());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
mvnw.cmd test -Dtest=StaffMessageControllerTest -DfailIfNoTests=false
```
Expected: FAIL — `StaffMessageController` does not exist.

- [ ] **Step 3: Create StaffMessageController**

Create `src/main/java/com/sait/peelin/controller/v1/StaffMessageController.java`:

```java
package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.SendStaffMessageRequest;
import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.dto.v1.StartConversationRequest;
import com.sait.peelin.service.StaffMessageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
@Tag(name = "Staff Messages", description = "Staff-to-staff direct messaging.")
@SecurityRequirement(name = "bearer-jwt")
public class StaffMessageController {

    private final StaffMessageService staffMessageService;

    @GetMapping("/conversations")
    public List<StaffConversationDto> conversations() {
        return staffMessageService.conversations();
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffConversationDto startConversation(@Valid @RequestBody StartConversationRequest req) {
        return staffMessageService.getOrCreateConversation(req.recipientId());
    }

    @GetMapping("/conversations/{convoId}/messages")
    public List<StaffMessageDto> messages(@PathVariable Integer convoId) {
        return staffMessageService.messages(convoId);
    }

    @PostMapping("/conversations/{convoId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffMessageDto send(@PathVariable Integer convoId,
                                @Valid @RequestBody SendStaffMessageRequest req) {
        return staffMessageService.sendMessage(convoId, req.text());
    }

    @PostMapping("/conversations/{convoId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Integer convoId) {
        staffMessageService.markRead(convoId);
    }
}
```

- [ ] **Step 4: Run tests**

```
mvnw.cmd test -Dtest=StaffMessageControllerTest -DfailIfNoTests=false
```
Expected: Tests: 5 passed

- [ ] **Step 5: Run all tests**

```
mvnw.cmd test
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/controller/v1/StaffMessageController.java apps/backend/src/test/java/com/sait/peelin/controller/v1/StaffMessageControllerTest.java
git commit -m "feat: add StaffMessageController REST endpoints for staff DMs"
```

---

## Task 12: StaffMessageWebSocketController

**Files:**
- Create: `apps/backend/src/main/java/com/sait/peelin/controller/ws/StaffMessageWebSocketController.java`

- [ ] **Step 1: Create the controller**

Create `src/main/java/com/sait/peelin/controller/ws/StaffMessageWebSocketController.java`:

```java
package com.sait.peelin.controller.ws;

import com.sait.peelin.dto.v1.TypingPayload;
import com.sait.peelin.service.StaffMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StaffMessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final StaffMessageService staffMessageService;

    @MessageMapping("/messages/conversation/{convoId}/typing")
    public void typing(@DestinationVariable Integer convoId, TypingPayload payload) {
        messagingTemplate.convertAndSend(
                "/topic/messages/conversation/" + convoId + "/typing", payload);
    }

    @MessageMapping("/messages/conversation/{convoId}/read")
    public void markRead(@DestinationVariable Integer convoId) {
        staffMessageService.markRead(convoId);
    }
}
```

- [ ] **Step 2: Run all tests**

```
mvnw.cmd test
```
Expected: BUILD SUCCESS — all tests pass

- [ ] **Step 3: Commit**

```
git add apps/backend/src/main/java/com/sait/peelin/controller/ws/StaffMessageWebSocketController.java
git commit -m "feat: add StaffMessageWebSocketController for typing and read relay"
```

---

## Final verification

```
mvnw.cmd verify
```
Expected: BUILD SUCCESS

Start the server and spot-check:
1. `GET /actuator/health` → `{"status":"UP"}`
2. `GET /swagger-ui/index.html` → new endpoints visible: `/api/v1/chat/threads/{threadId}/close` and `/api/v1/messages/conversations/**`
3. No startup errors in console — Flyway migration V42 runs cleanly against a dev PostgreSQL instance

---

## What's next

- **Frontend plan** — `ws.ts` service, customer chat widget, staff chat inbox, staff DM views (Workshop7 SvelteKit)
- **Android plan** — `ChatWebSocketClient` replaces polling (Workshop06)
- **Desktop plan** — `MessageApi` REST rewrite + polling (Workshop5)
