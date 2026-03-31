package com.sait.peelin.service;

import com.sait.peelin.dto.v1.AddressCreateRequest;
import com.sait.peelin.dto.v1.AddressSummaryDto;
import com.sait.peelin.model.Address;
import com.sait.peelin.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressAdminService {

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<AddressSummaryDto> listAll() {
        return addressRepository.findAll().stream()
                .map(a -> new AddressSummaryDto(
                        a.getId(),
                        a.getAddressLine1(),
                        a.getAddressLine2(),
                        a.getAddressCity(),
                        a.getAddressProvince(),
                        a.getAddressPostalCode()
                ))
                .toList();
    }

    @Transactional
    public Integer create(AddressCreateRequest req) {
        Address a = new Address();
        a.setAddressLine1(req.line1());
        a.setAddressLine2(req.line2());
        a.setAddressCity(req.city());
        a.setAddressProvince(req.province());
        a.setAddressPostalCode(req.postalCode());
        return addressRepository.save(a).getId();
    }
}
