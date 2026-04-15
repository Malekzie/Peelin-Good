package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ProductSpecialDto;
import com.sait.peelin.dto.v1.ProductSpecialTodayDto;
import com.sait.peelin.dto.v1.ProductSpecialUpsertRequest;
import com.sait.peelin.model.Product;
import com.sait.peelin.model.ProductSpecial;
import com.sait.peelin.repository.ProductRepository;
import com.sait.peelin.repository.ProductSpecialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSpecialService {

    private final ProductSpecialRepository productSpecialRepository;
    private final ProductRepository productRepository;

    // ── Queries ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = "product-specials", key = "#date")
    public ProductSpecialTodayDto findFirstForDate(LocalDate date) {
        return productSpecialRepository.findFirstByFeaturedOnOrderByProductSpecialIdAsc(date)
                .map(ps -> new ProductSpecialTodayDto(ps.getProductId(), ps.getDiscountPercent()))
                .orElse(new ProductSpecialTodayDto(null, null));
    }

    @Transactional(readOnly = true)
    public List<ProductSpecialDto> findAllSpecials() {
        return productSpecialRepository.findAll().stream()
                .map(s -> toDto(s, requireProduct(s.getProductId())))
                .toList();
    }

    // ── Mutations ────────────────────────────────────────────────

    /**
     * Creates a new product special entry and evicts the date-keyed cache so
     * the next call to {@link #findFirstForDate} reflects the new row.
     *
     * <p>Enforces two business rules before persisting:
     * <ul>
     *   <li>Only one special may be featured per calendar day.</li>
     *   <li>The discount may not exceed 50 %.</li>
     * </ul>
     */
    @Transactional
    @CacheEvict(value = "product-specials", allEntries = true)
    public ProductSpecialDto create(ProductSpecialUpsertRequest req) {
        requireProduct(req.getProductId());
        requireUniqueFeaturedOnForCreate(req.getFeaturedOn());

        ProductSpecial ps = new ProductSpecial();
        ps.setProductId(req.getProductId());
        ps.setFeaturedOn(req.getFeaturedOn());
        ps.setDiscountPercent(req.getDiscountPercent());

        ProductSpecial saved = productSpecialRepository.save(ps);
        return toDto(saved, requireProduct(saved.getProductId()));
    }

    /**
     * Replaces all mutable fields on an existing product special and evicts the cache.
     *
     * <p>Enforces the same business rules as {@link #create}: one special per day (excluding the
     * row being updated so a date-only update does not conflict with itself) and a 50 % discount
     * ceiling.
     */
    @Transactional
    @CacheEvict(value = "product-specials", allEntries = true)
    public ProductSpecialDto update(Integer id, ProductSpecialUpsertRequest req) {
        ProductSpecial ps = productSpecialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product special not found: " + id));

        requireProduct(req.getProductId());
        requireUniqueFeaturedOnForUpdate(req.getFeaturedOn(), id);

        ps.setProductId(req.getProductId());
        ps.setFeaturedOn(req.getFeaturedOn());
        ps.setDiscountPercent(req.getDiscountPercent());

        ProductSpecial saved = productSpecialRepository.save(ps);
        return toDto(saved, requireProduct(saved.getProductId()));
    }

    /**
     * Deletes a product special by ID and evicts the cache.
     */
    @Transactional
    @CacheEvict(value = "product-specials", allEntries = true)
    public void delete(Integer id) {
        if (!productSpecialRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product special not found: " + id);
        }
        productSpecialRepository.deleteById(id);
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * Throws {@code 409 Conflict} when another special is already scheduled for {@code date}.
     * Used by the create path where no existing row should be excluded from the check.
     */
    private void requireUniqueFeaturedOnForCreate(LocalDate date) {
        if (productSpecialRepository.existsByFeaturedOn(date)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A product special is already scheduled for " + date
                            + ". Only one special is allowed per day.");
        }
    }

    /**
     * Throws {@code 409 Conflict} when a <em>different</em> special is already scheduled for
     * {@code date}.  The {@code currentId} of the row being updated is excluded so that saving
     * the same featured-on date does not trigger a self-conflict.
     */
    private void requireUniqueFeaturedOnForUpdate(LocalDate date, Integer currentId) {
        if (productSpecialRepository.existsByFeaturedOnAndProductSpecialIdNot(date, currentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A product special is already scheduled for " + date
                            + ". Only one special is allowed per day.");
        }
    }

    private Product requireProduct(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productId));
    }

    private static ProductSpecialDto toDto(ProductSpecial s, Product p) {
        return new ProductSpecialDto(
                s.getProductSpecialId(),
                s.getFeaturedOn(),
                s.getDiscountPercent(),
                p.getId(),
                p.getProductName(),
                p.getProductDescription(),
                p.getProductBasePrice(),
                p.getProductImageUrl()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product-specials", key = "'list:' + #date", unless = "#result.isEmpty()")
    public List<ProductSpecialTodayDto> findForDate(LocalDate date) {
        return productSpecialRepository.findByFeaturedOnOrderByProductSpecialIdAsc(date)
                .stream()
                .map(ps -> new ProductSpecialTodayDto(ps.getProductId(), ps.getDiscountPercent()))
                .toList();
    }
}
