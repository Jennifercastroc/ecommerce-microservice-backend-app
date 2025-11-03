package com.selimhorri.app.integration;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.service.ProductService;
import com.selimhorri.app.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@Rollback
class ProductIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testMultipleProductsSaved() {
        CategoryDto category = createCategory("Bulk Category");
        ProductDto p1 = productService.save(buildProduct(category, "A", 10.0, 1));
        ProductDto p2 = productService.save(buildProduct(category, "B", 20.0, 2));

        List<ProductDto> all = productService.findAll();
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(dto -> dto.getProductId().equals(p1.getProductId())));
        assertTrue(all.stream().anyMatch(dto -> dto.getProductId().equals(p2.getProductId())));
    }

    private CategoryDto createCategory(String title) {
        Category category = categoryRepository.save(Category.builder()
                .categoryTitle(title)
                .imageUrl("http://example.com/" + UUID.randomUUID() + ".png")
                .build());

        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryTitle(category.getCategoryTitle())
                .imageUrl(category.getImageUrl())
                .build();
    }

    private ProductDto buildProduct(CategoryDto category, String title, double price, int quantity) {
        CategoryDto simpleCategory = CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryTitle(category.getCategoryTitle())
                .imageUrl(category.getImageUrl())
                .build();

        return ProductDto.builder()
                .productTitle(title)
                .imageUrl("http://example.com/" + UUID.randomUUID() + ".png")
                .sku("SKU-" + UUID.randomUUID())
                .priceUnit(price)
                .quantity(quantity)
                .categoryDto(simpleCategory)
                .build();
    }
}
