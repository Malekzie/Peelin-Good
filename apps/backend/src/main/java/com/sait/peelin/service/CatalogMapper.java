package com.sait.peelin.service;

import com.sait.peelin.dto.v1.*;
import com.sait.peelin.model.*;
import com.sait.peelin.repository.ProductTagRepository;

import java.util.List;
import java.util.stream.Collectors;

public final class CatalogMapper {

    private CatalogMapper() {}

    public static AddressDto address(Address a) {
        if (a == null) return null;
        return new AddressDto(
                a.getId(),
                a.getAddressLine1(),
                a.getAddressLine2(),
                a.getAddressCity(),
                a.getAddressProvince(),
                a.getAddressPostalCode()
        );
    }

    public static void copyAddress(AddressUpsertRequest r, Address a) {
        a.setAddressLine1(r.getLine1());
        a.setAddressLine2(r.getLine2());
        a.setAddressCity(r.getCity());
        a.setAddressProvince(r.getProvince());
        a.setAddressPostalCode(r.getPostalCode());
    }

    public static TagDto tag(Tag t) {
        return new TagDto(t.getId(), t.getTagName());
    }

    public static ProductDto product(Product p, ProductTagRepository productTagRepository) {
        List<Integer> tagIds = productTagRepository.findByProduct_Id(p.getId()).stream()
                .map(pt -> pt.getTag().getId())
                .collect(Collectors.toList());
        return product(p, tagIds);
    }

    public static ProductDto product(Product p, List<Integer> tagIds) {
        return new ProductDto(
                p.getId(),
                p.getProductName(),
                p.getProductDescription(),
                p.getProductBasePrice(),
                p.getProductImageUrl(),
                tagIds
        );
    }

    public static BakeryDto bakery(Bakery b) {
        return new BakeryDto(
                b.getId(),
                b.getBakeryName(),
                b.getBakeryPhone(),
                b.getBakeryEmail(),
                b.getStatus(),
                b.getLatitude(),
                b.getLongitude(),
                b.getBakeryImageUrl(),
                address(b.getAddress())
        );
    }

    public static BakeryHourDto hour(BakeryHour h) {
        return new BakeryHourDto(
                h.getId(),
                h.getDayOfWeek(),
                h.getOpenTime(),
                h.getCloseTime(),
                Boolean.TRUE.equals(h.getIsClosed())
        );
    }

    public static BatchDto batch(Batch b) {
        return new BatchDto(
                b.getId(),
                b.getBakery().getId(),
                b.getProduct().getId(),
                b.getBatchProductionDate(),
                b.getBatchExpiryDate(),
                b.getBatchQuantityProduced()
        );
    }
}
