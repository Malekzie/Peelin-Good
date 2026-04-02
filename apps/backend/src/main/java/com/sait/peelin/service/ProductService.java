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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;

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

    public ProductDto get(Integer id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return CatalogMapper.product(p, productTagRepository);
    }

    @Transactional
    public ProductDto create(ProductUpsertRequest req) {
        Product p = new Product();
        apply(req, p);
        p = productRepository.save(p);
        syncTags(p.getId(), req.getTagIds());
        productRepository.flush();
        return CatalogMapper.product(productRepository.findById(p.getId()).orElseThrow(), productTagRepository);
    }

    @Transactional
    public ProductDto update(Integer id, ProductUpsertRequest req) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        apply(req, p);
        productRepository.save(p);
        productTagRepository.deleteByProduct_Id(id);
        syncTags(id, req.getTagIds());
        return CatalogMapper.product(productRepository.findById(id).orElseThrow(), productTagRepository);
    }

    @Transactional
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
