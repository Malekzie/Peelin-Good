package com.sait.peelin.service;

import com.sait.peelin.model.User;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Login accepts username or email; principal may be either (case-insensitive).
        User user = userRepository
                .findByUsernameIgnoreCaseOrUserEmailIgnoreCase(username.trim(), username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return userDetailsFor(user);
    }

    /** Builds {@link UserDetails} from a persisted {@link User} (JWT filter / tests). */
    public UserDetails userDetailsFor(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getUserPasswordHash(),
                Boolean.TRUE.equals(user.getActive()),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name().toUpperCase()))
        );
    }

}
