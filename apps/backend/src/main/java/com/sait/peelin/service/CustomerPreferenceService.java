package com.sait.peelin.service;

import com.sait.peelin.dto.v1.CustomerPreferenceDto;
import com.sait.peelin.dto.v1.CustomerPreferenceSaveRequest;
import com.sait.peelin.model.*;
import com.sait.peelin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerPreferenceService {

    private final CustomerPreferenceRepository preferenceRepository;
    private final CustomerRepository customerRepository;
    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<CustomerPreferenceDto> getMyPreferences() {
        User u = currentUserService.requireUser();
        Customer c = customerRepository.findByUser_UserId(u.getUserId()).orElse(null);
        if (c == null) return List.of();
        return preferenceRepository.findByCustomer_Id(c.getId())
                .stream()
                .map(p -> new CustomerPreferenceDto(
                        p.getTag().getId(),
                        p.getTag().getTagName(),
                        p.getPreferenceType(),
                        p.getPreferenceStrength()
                ))
                .toList();
    }

    @Transactional
    public List<CustomerPreferenceDto> saveMyPreferences(CustomerPreferenceSaveRequest request) {
        User u = currentUserService.requireUser();
        Customer c = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer profile"));

        preferenceRepository.deleteByCustomer_Id(c.getId());

        for (CustomerPreferenceSaveRequest.PreferenceEntry entry : request.getPreferences()) {
            if (entry.getPreferenceType() == null) continue;
            Tag tag = tagRepository.findById(entry.getTagId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag not found: " + entry.getTagId()));
            CustomerPreferenceId id = new CustomerPreferenceId();
            id.setCustomerId(c.getId());
            id.setTagId(tag.getId());
            CustomerPreference pref = new CustomerPreference();
            pref.setId(id);
            pref.setCustomer(c);
            pref.setTag(tag);
            pref.setPreferenceType(entry.getPreferenceType());
            preferenceRepository.save(pref);
        }

        return getMyPreferences();
    }
}