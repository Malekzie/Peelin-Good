package com.sait.peelin.security;

import com.sait.peelin.model.User;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.TokenDenylistService;
import com.sait.peelin.service.UserLookupCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final UserLookupCacheService userLookupCacheService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        var existingAuth = SecurityContextHolder.getContext().getAuthentication();

        if (existingAuth != null && existingAuth.isAuthenticated() && existingAuth.getName() != null) {
            User sessionUser = userLookupCacheService.findActiveByLoginIdentifier(existingAuth.getName());

            if (sessionUser == null) {
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"reason\":\"deactivated\"}");
                return;
            }
        }

        String jwt = extractToken(request);

        if (jwt == null || tokenDenylistService.isDenied(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userLookupCacheService.findActiveByLoginIdentifier(username);
                if (user == null) {

                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"reason\":\"deactivated\"}");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name().toUpperCase()))
                );
                // Reuse the resolved user in this request so downstream current-user lookups avoid a second DB hit.
                authToken.setDetails(user);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            logger.warn("JWT validation failed: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"reason\":\"expired\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the request.
     * Checks the HttpOnly cookie first (web app), then falls back to
     * the Authorization header (mobile/desktop apps).
     */
    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
