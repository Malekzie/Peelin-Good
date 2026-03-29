package com.sait.peelin.service;

import com.sait.peelin.dto.v1.auth.AuthResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserService {

    // TODO: look up or create User by email, assign customer role, generate JWT
    public AuthResponse processOAuth2User(String provider, OAuth2User oAuth2User) {
        throw new UnsupportedOperationException("OAuth2 login is scaffolded but not yet implemented");
    }
}
