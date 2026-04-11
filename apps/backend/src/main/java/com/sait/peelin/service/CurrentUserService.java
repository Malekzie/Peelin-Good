package com.sait.peelin.service;

import com.sait.peelin.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserLookupCacheService userLookupCacheService;

    public User requireUser() {
        User user = currentUserOrNull();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }

    public User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        Object details = auth.getDetails();
        if (details instanceof User detailUser) {
            return Boolean.TRUE.equals(detailUser.getActive()) ? detailUser : null;
        }

        String name = auth.getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        User resolved = userLookupCacheService.findActiveByLoginIdentifier(trimmed);
        if (resolved != null && auth instanceof AbstractAuthenticationToken authToken) {
            // Keep this user snapshot on auth details to avoid duplicate lookups in the same request.
            authToken.setDetails(resolved);
        }
        return resolved;
    }
}
