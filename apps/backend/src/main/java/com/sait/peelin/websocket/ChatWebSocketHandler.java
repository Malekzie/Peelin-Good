package com.sait.peelin.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sait.peelin.dto.v1.ChatMessageDto;
import com.sait.peelin.model.User;
import com.sait.peelin.service.ChatService;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String THREAD_ID_ATTR = "threadId";
    private static final String USER_ATTR = "user";
    private static final String THREAD_ID_PARAM = "threadId";

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    private final Map<Integer, Set<WebSocketSession>> sessionsByThread = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer threadId = extractThreadId(session.getUri());
        if (threadId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing threadId"));
            return;
        }

        Object userAttr = session.getAttributes().get(USER_ATTR);
        if (!(userAttr instanceof User user)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        try {
            chatService.assertUserCanAccessThread(new ChatService.ThreadRef(threadId), user);
        } catch (ResponseStatusException ex) {
            session.close(new CloseStatus(resolveCloseCode(ex.getStatusCode()), "Forbidden"));
            return;
        }

        session.getAttributes().put(THREAD_ID_ATTR, threadId);
        sessionsByThread
                .computeIfAbsent(threadId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);

        ObjectNode connected = objectMapper.createObjectNode();
        connected.put("type", "connected");
        connected.put("threadId", threadId);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connected)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Object userAttr = session.getAttributes().get(USER_ATTR);
        Object threadAttr = session.getAttributes().get(THREAD_ID_ATTR);

        if (!(userAttr instanceof User user) || !(threadAttr instanceof Integer threadId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid session"));
            return;
        }

        JsonNode payload = objectMapper.readTree(message.getPayload());
        String type = payload.hasNonNull("type") ? payload.get("type").asText() : "message";

        switch (type) {
            case "message" -> handleCreateMessage(session, threadId, user, payload);
            case "read" -> handleRead(threadId, user);
            default -> session.close(CloseStatus.BAD_DATA.withReason("Unsupported message type"));
        }
    }

    private void handleCreateMessage(
            WebSocketSession session,
            Integer threadId,
            User user,
            JsonNode payload
    ) throws Exception {
        String text = payload.hasNonNull("text") ? payload.get("text").asText() : null;

        ChatMessageDto created;
        try {
            created = chatService.postMessage(
                    new ChatService.ThreadRef(threadId),
                    new ChatService.MessageDraft(text),
                    user
            );
        } catch (ResponseStatusException ex) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("type", "error");
            error.put("status", ex.getStatusCode().value());
            error.put("message", ex.getReason() != null ? ex.getReason() : "Request failed");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            return;
        }

        ObjectNode outbound = objectMapper.createObjectNode();
        outbound.put("type", "message");
        outbound.set("message", objectMapper.valueToTree(created));

        broadcastToThread(threadId, outbound);
    }

    private void handleRead(Integer threadId, User user) throws Exception {
        boolean changed = chatService.markRead(new ChatService.ThreadRef(threadId), user);
        if (!changed) {
            return;
        }

        List<ChatMessageDto> messages = chatService.messages(new ChatService.ThreadRef(threadId), user);

        ObjectNode outbound = objectMapper.createObjectNode();
        outbound.put("type", "read");
        outbound.put("threadId", threadId);
        outbound.put("readerUserId", user.getUserId().toString());
        outbound.set("messages", objectMapper.valueToTree(messages));

        broadcastToThread(threadId, outbound);
    }

    private void broadcastToThread(Integer threadId, ObjectNode payload) throws Exception {
        String outbound = objectMapper.writeValueAsString(payload);

        for (WebSocketSession threadSession : sessionsByThread.getOrDefault(threadId, Set.of())) {
            if (threadSession.isOpen()) {
                threadSession.sendMessage(new TextMessage(outbound));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object threadAttr = session.getAttributes().get(THREAD_ID_ATTR);
        if (threadAttr instanceof Integer threadId) {
            Set<WebSocketSession> sessions = sessionsByThread.get(threadId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByThread.remove(threadId);
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Integer extractThreadId(URI uri) {
        String query = extractQuery(uri);
        if (query == null) {
            return null;
        }

        for (String part : query.split("&")) {
            Integer threadId = parseThreadIdParam(part);
            if (threadId != null) {
                return threadId;
            }
        }

        return null;
    }

    private String extractQuery(URI uri) {
        if (uri == null) {
            return null;
        }

        String query = uri.getQuery();
        return isBlank(query) ? null : query;
    }

    private Integer parseThreadIdParam(String part) {
        String[] kv = part.split("=", 2);
        if (!isThreadIdParam(kv)) {
            return null;
        }

        try {
            return Integer.valueOf(kv[1]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isThreadIdParam(String[] kv) {
        return kv.length == 2 && THREAD_ID_PARAM.equals(kv[0]);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int resolveCloseCode(HttpStatusCode status) {
        int value = status.value();
        return switch (value) {
            case 401 -> 4401;
            case 403 -> 4403;
            case 404 -> 4404;
            case 409 -> 4409;
            default -> 1011;
        };
    }
}