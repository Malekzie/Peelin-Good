package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ProductDto;
import com.sait.peelin.dto.v1.ProductUpsertRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Product;
import com.sait.peelin.model.ProductTag;
import com.sait.peelin.model.ProductTagId;
import com.sait.peelin.model.Tag;
import com.sait.peelin.repository.ProductRepository;
import com.sait.peelin.repository.ProductTagRepository;
import com.sait.peelin.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;
    private final ProfilePhotoStorageService profilePhotoStorageService;

    @Cacheable(value = "products", key = "'all:' + #search + ':' + #tagId")
    public List<ProductDto> list(String search, Integer tagId) {
        List<Product> products;
        if (tagId != null) {
            products = productRepository.findByTagId(tagId);
        } else if (StringUtils.hasText(search)) {
            products = productRepository.findByProductNameContainingIgnoreCase(search.trim());
        } else {
            products = productRepository.findAll();
        }

        if (products.isEmpty()) {
            return List.of();
        }

        java.util.Map<Integer, List<Integer>> tagsByProduct = productTagRepository
                .findByProduct_IdIn(products.stream().map(com.sait.peelin.model.Product::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        pt -> pt.getProduct().getId(),
                        java.util.stream.Collectors.mapping(pt -> pt.getTag().getId(), java.util.stream.Collectors.toList())
                ));

        return products.stream()
                .map(p -> CatalogMapper.product(p, tagsByProduct.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Cacheable(value = "products", key = "#id")
    public ProductDto get(Integer id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return CatalogMapper.product(p, productTagRepository);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto create(ProductUpsertRequest req) {
        Product p = new Product();
        apply(req, p);
        p = productRepository.save(p);
        syncTags(p.getId(), req.getTagIds());
        productRepository.flush();
        return CatalogMapper.product(productRepository.findById(p.getId()).orElseThrow(), productTagRepository);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto update(Integer id, ProductUpsertRequest req) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        apply(req, p);
        productRepository.save(p);
        productTagRepository.deleteByProduct_Id(id);
        syncTags(id, req.getTagIds());
        return CatalogMapper.product(productRepository.findById(id).orElseThrow(), productTagRepository);
    }

    /**
     * Uploads an image for a product to the {@code bakery/} folder in object storage,
     * then persists the resulting public URL on the product record.
     *
     * @param id    product ID
     * @param image image file (JPG or PNG, max 5 MB)
     * @return updated {@link ProductDto}
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto uploadImage(Integer id, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        String ct = image.getContentType() != null ? image.getContentType().toLowerCase() : "";
        if (!ct.equals("image/jpeg") && !ct.equals("image/jpg") && !ct.equals("image/png")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG and PNG images are allowed");
        }
        if (image.getSize() > 5L * 1024L * 1024L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image exceeds 5 MB limit");
        }

        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String url = profilePhotoStorageService.uploadProductImage(image, p.getProductImageUrl());
        p.setProductImageUrl(url);
        return CatalogMapper.product(productRepository.save(p), productTagRepository);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found");
        }
        productTagRepository.deleteByProduct_Id(id);
        productRepository.deleteById(id);
    }

    private void apply(ProductUpsertRequest req, Product p) {
        p.setProductName(req.getName().trim());
        p.setProductDescription(req.getDescription());
        p.setProductBasePrice(req.getBasePrice());
        p.setProductImageUrl(req.getImageUrl());
    }

    private void syncTags(Integer productId, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        Product product = productRepository.getReferenceById(productId);
        for (Integer tid : tagIds) {
            Tag tag = tagRepository.findById(tid).orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tid));
            ProductTag pt = new ProductTag();
            ProductTagId id = new ProductTagId();
            id.setProductId(productId);
            id.setTagId(tag.getId());
            pt.setId(id);
            pt.setProduct(product);
            pt.setTag(tag);
            productTagRepository.save(pt);
        }
    }
}
