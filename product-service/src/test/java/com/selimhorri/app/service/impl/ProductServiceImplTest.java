package com.selimhorri.app.service.impl;

import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.util.ProductUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        product = ProductUtil.getSampleProduct();
        productDto = ProductUtil.getSampleProductDto();
    }

    @Test
    void testFindById_ShouldReturnProductDto() {
        when(productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));

        ProductDto result = productService.findById(product.getProductId());

        assertNotNull(result);
        assertEquals(productDto.getProductId(), result.getProductId());
        assertEquals(productDto.getProductTitle(), result.getProductTitle());
    }

}

