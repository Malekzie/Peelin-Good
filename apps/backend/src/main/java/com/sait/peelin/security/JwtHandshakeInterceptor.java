package com.sait.peelin.security;

import com.sait.peelin.model.User;
import com.sait.peelin.repository.UserRepository;
import com.sait.peelin.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final String USER_ATTRIBUTE = "user";
    private static final String TOKEN_COOKIE_NAME = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        HttpServletRequest httpRequest = toHttpServletRequest(request);
        if (httpRequest == null) {
            return reject(response);
        }

        try {
            User user = authenticateUser(httpRequest, request.getHeaders());
            if (user == null) {
                return reject(response);
            }

            attributes.put(USER_ATTRIBUTE, user);
            return true;
        } catch (RuntimeException ex) {
            return reject(response);
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    private HttpServletRequest toHttpServletRequest(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        return servletRequest.getServletRequest();
    }

    private User authenticateUser(HttpServletRequest request, HttpHeaders headers) {
        String token = extractToken(request, headers);
        if (isBlank(token)) {
            return null;
        }

        String username = jwtService.extractUsername(token);
        if (isBlank(username)) {
            return null;
        }

        String normalizedUsername = username.trim();
        UserDetails userDetails = userDetailsService.loadUserByUsername(normalizedUsername);
        if (!jwtService.isTokenValid(token, userDetails)) {
            return null;
        }

        return userRepository
                .findByUsernameIgnoreCaseOrUserEmailIgnoreCase(normalizedUsername, normalizedUsername)
                .orElse(null);
    }

    private boolean reject(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    private String extractToken(HttpServletRequest request, HttpHeaders headers) {
        String cookieToken = extractTokenFromCookies(request);
        if (!isBlank(cookieToken)) {
            return cookieToken;
        }

        String headerToken = extractTokenFromAuthorizationHeader(headers);
        if (!isBlank(headerToken)) {
            return headerToken;
        }

        return extractTokenFromQuery(request);
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private String extractTokenFromAuthorizationHeader(HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authHeader.substring(BEARER_PREFIX.length());
    }

    private String extractTokenFromQuery(HttpServletRequest request) {
        String queryToken = request.getParameter("token");
        return isBlank(queryToken) ? null : queryToken;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}