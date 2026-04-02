package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ProductDto;
import com.sait.peelin.model.Product;
import com.sait.peelin.model.ProductTag;
import com.sait.peelin.model.Tag;
import com.sait.peelin.repository.ProductRepository;
import com.sait.peelin.repository.ProductTagRepository;
import com.sait.peelin.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductTagRepository productTagRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void list_ShouldFetchTagsInBatch() {
        // Arrange
        Product p1 = new Product(); p1.setId(1); p1.setProductName("P1"); p1.setProductBasePrice(BigDecimal.TEN);
        Product p2 = new Product(); p2.setId(2); p2.setProductName("P2"); p2.setProductBasePrice(BigDecimal.TEN);
        
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        Tag t1 = new Tag(); t1.setId(10);
        Tag t2 = new Tag(); t2.setId(20);

        ProductTag pt1 = new ProductTag(); pt1.setProduct(p1); pt1.setTag(t1);
        ProductTag pt2 = new ProductTag(); pt2.setProduct(p2); pt2.setTag(t2);

        when(productTagRepository.findByProduct_IdIn(anyList())).thenReturn(List.of(pt1, pt2));

        // Act
        List<ProductDto> result = productService.list(null, null);

        // Assert
        assertEquals(2, result.size());
        verify(productTagRepository, times(1)).findByProduct_IdIn(anyList());
        verify(productTagRepository, never()).findByProduct_Id(anyInt());
        
        assertEquals(1, result.get(0).tagIds().size());
        assertEquals(10, result.get(0).tagIds().get(0));
        assertEquals(20, result.get(1).tagIds().get(0));
    }
}
