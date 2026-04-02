package com.sait.peelin.service;

import com.sait.peelin.dto.v1.BakeryDto;
import com.sait.peelin.dto.v1.BakeryHourDto;
import com.sait.peelin.dto.v1.BakeryUpsertRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Address;
import com.sait.peelin.model.Bakery;
import com.sait.peelin.model.BakeryStatus;
import com.sait.peelin.repository.AddressRepository;
import com.sait.peelin.repository.BakeryHourRepository;
import com.sait.peelin.repository.BakeryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BakeryService {

    private final BakeryRepository bakeryRepository;
    private final BakeryHourRepository bakeryHourRepository;
    private final AddressRepository addressRepository;

    /**
     * Must run in a transaction while mapping: {@link Bakery#getAddress()} is lazy-loaded.
     * Without this, the list/get endpoints fail with {@code LazyInitializationException} after {@code findAll()}.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "bakeries", key = "'all:' + #search")
    public List<BakeryDto> list(String search) {
        List<Bakery> list = StringUtils.hasText(search)
                ? bakeryRepository.findByBakeryNameContainingIgnoreCase(search.trim())
                : bakeryRepository.findAll();
        return list.stream().map(CatalogMapper::bakery).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "bakeries", key = "#id")
    public BakeryDto get(Integer id) {
        Bakery b = bakeryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bakery not found"));
        return CatalogMapper.bakery(b);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "bakeries", key = "'hours:' + #bakeryId")
    public List<BakeryHourDto> hours(Integer bakeryId) {
        ensureBakery(bakeryId);
        return bakeryHourRepository.findByBakery_IdOrderByDayOfWeekAsc(bakeryId).stream()
                .map(CatalogMapper::hour)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "bakeries", allEntries = true)
    public BakeryDto create(BakeryUpsertRequest req) {
        Address addr = new Address();
        CatalogMapper.copyAddress(req.getAddress(), addr);
        addr = addressRepository.save(addr);

        Bakery b = new Bakery();
        b.setAddress(addr);
        b.setBakeryName(req.getName().trim());
        b.setBakeryPhone(req.getPhone());
        b.setBakeryEmail(req.getEmail());
        b.setStatus(req.getStatus() != null ? req.getStatus() : BakeryStatus.open);
        b.setLatitude(req.getLatitude());
        b.setLongitude(req.getLongitude());
        applyBakeryImageUrl(b, req.getBakeryImageUrl());
        return CatalogMapper.bakery(bakeryRepository.save(b));
    }

    @Transactional
    @CacheEvict(value = "bakeries", allEntries = true)
    public BakeryDto update(Integer id, BakeryUpsertRequest req) {
        Bakery b = bakeryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bakery not found"));
        CatalogMapper.copyAddress(req.getAddress(), b.getAddress());
        addressRepository.save(b.getAddress());
        b.setBakeryName(req.getName().trim());
        b.setBakeryPhone(req.getPhone());
        b.setBakeryEmail(req.getEmail());
        if (req.getStatus() != null) {
            b.setStatus(req.getStatus());
        }
        b.setLatitude(req.getLatitude());
        b.setLongitude(req.getLongitude());
        applyBakeryImageUrl(b, req.getBakeryImageUrl());
        return CatalogMapper.bakery(bakeryRepository.save(b));
    }

    private void applyBakeryImageUrl(Bakery b, String url) {
        if (url == null) {
            return;
        }
        String t = url.trim();
        b.setBakeryImageUrl(t.isEmpty() ? null : t);
    }

    @Transactional
    @CacheEvict(value = "bakeries", allEntries = true)
    public void delete(Integer id) {
        if (!bakeryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bakery not found");
        }
        bakeryRepository.deleteById(id);
    }

    private void ensureBakery(Integer id) {
        if (!bakeryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bakery not found");
        }
    }
}
