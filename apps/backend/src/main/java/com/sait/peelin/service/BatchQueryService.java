package com.sait.peelin.service;

import com.sait.peelin.dto.v1.BatchDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Batch;
import com.sait.peelin.repository.BatchRepository;
import com.sait.peelin.repository.BakeryRepository;
import com.sait.peelin.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchQueryService {

    private final BatchRepository batchRepository;
    private final BakeryRepository bakeryRepository;
    private final ProductRepository productRepository;

    public List<BatchDto> byBakery(Integer bakeryId, boolean activeOnly) {
        if (!bakeryRepository.existsById(bakeryId)) {
            throw new ResourceNotFoundException("Bakery not found");
        }
        List<Batch> batches = activeOnly
                ? batchRepository.findActiveByBakery(bakeryId, OffsetDateTime.now())
                : batchRepository.findByBakery_Id(bakeryId);
        return batches.stream().map(CatalogMapper::batch).toList();
    }

    public List<BatchDto> byProduct(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return batchRepository.findByProduct_Id(productId).stream().map(CatalogMapper::batch).toList();
    }
}
